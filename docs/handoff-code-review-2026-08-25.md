# Hand-off — PR #51 리뷰 마무리 및 전체 코드 리뷰 결과

> 작성일: 2026-08-25 / 작성 세션에서 정리. **다른 계정·새 세션이 이 문서만 읽고 바로 이어서 작업할 수 있도록** 구성했다.
> 이 문서는 작업 인수인계 메모다. `docs/v2-*.md`(공식 스펙)와는 별개이며, 기능 요구사항의 근거는 항상 스펙 원본을 확인할 것.
>
> **역사 기록 — 현재 작업 지시로 사용 금지.** 아래 PR #51·이슈 상태·코드 위치는 2026-08-25 검토 시점의 스냅샷이다.
> 예를 들어 #56은 종료됐고 S3 `StoragePort` 어댑터는 현재 `main`에 구현돼 있다. 현재 기능 상태는
> [v2-backlog.md](./v2-backlog.md), 현재 이슈 상태는 GitHub, 제품 결정은 [v2-open-questions-for-product.md](./v2-open-questions-for-product.md)를 따른다.

## 0. 저장소 / 브랜치

- 저장소: `memeboomemeboo/haemi-server-v2`, 기본 브랜치 `main`
- 작업 브랜치: `integration/2026-08-25-13-issues`
- PR: [#51 — 13개 이슈 통합 (추억·인증·가족·역할·하루한마디·출석/리포트)](https://github.com/memeboomemeboo/haemi-server-v2/pull/51) — **OPEN 유지. 머지하지 말 것** (사용자 지시)
- 최신 커밋: `e15ecef` (origin 푸시 완료)
- `./gradlew test` 전체 통과 (마지막 실행: `e15ecef` 시점)
- 워킹 트리 클린. 단 `docs/client-backend-mapping.md`가 untracked인데 **이전 세션 산출물이라 손대지 않았다.** 커밋 여부는 사용자 판단 필요.

### 최근 커밋 흐름

```
e15ecef fix: 하루 한마디 중복 전송 시 409 대신 500이 나가던 결함 (#58)
2989b3b fix: 재리뷰 미수정 2건 — 합류 유니크 위반 409 보장, 로그인 실패 죽은 코드 제거
06a4b25 feat: TrainingSessionCompleted 발행처 추가 — 출석·리포트 데이터 생성 경로 연결
fd17975 fix: 리뷰 P1 3건 + P2 1건 — 실제 메일 발송, 로그인 성공 경로 원자화, R2 제약 정리, H2 호환
```

---

## 1. 지금까지 한 일

1. PR #51(136파일, +4,346/−360)을 3회 리뷰하고 지적사항을 전부 반영해 푸시했다.
2. 레포 전체(214파일, ~7,200줄)를 읽고 **PR 범위 밖 결함 13건**을 찾았다.
3. 그중 9건을 GitHub 이슈로 등록했다(아래 §3).
4. 이슈 중 #58만 성격상 PR #51에 함께 반영했다(`e15ecef`).

---

## 2. 반드시 알고 있어야 할 코드 규약

### 2.1 유니크 위반을 409로 바꿀 때 — REQUIRES_NEW 격리 패턴 (중요)

**이 레포에서 같은 버그가 세 번 반복됐다. 새 코드에서 DB 유니크 제약을 다룬다면 반드시 이 패턴을 따를 것.**

문제: 애플리케이션 선검사(`existsBy...`)만으로는 동시 요청을 막지 못해 DB 유니크 제약이 최종 방어선이 된다. 그런데 그 위반을 **바깥 트랜잭션과 같은 커넥션에서** `catch`하면 Postgres가 트랜잭션 전체를 abort시킨다. `catch` 후 정상 흐름을 이어가거나 바깥이 커밋을 시도하면 `UnexpectedRollbackException`이 터져 **의도한 409 대신 500이 나간다.**

해결:

1. 저장 본문을 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 컴포넌트로 분리 → abort가 그 트랜잭션에만 갇힌다
2. `ConstraintViolations.isViolationOf(e, "<제약명>")`로 **어느 제약인지 판별**
3. 해당 제약이면 `DomainException`으로 빠져나가고, **아니면 그대로 재던진다**(다른 제약 위반을 엉뚱한 응답으로 감추지 않기 위해)
4. 정상 반환이 아니라 예외로 빠져나가야 한다 — abort된 트랜잭션에서 정상 반환하면 커밋 시점에 다시 터진다

적용된 클래스:

| 클래스 | 제약 |
| --- | --- |
| `guardian/family/application/FamilyInviteCodeSaver` | `uk_guardian_families_invite_code` |
| `guardian/family/application/FamilyJoinSaver` | `uk_family_member_user` |
| `guardian/dailycare/application/DailyCareSaver` | `uk_daily_care_guardian_elder_date` |

부수 규칙: 외부 상태를 바꾸는 호출(예: `mediaUploadCommand.confirmUpload`)은 **저장자 트랜잭션 안**에 둔다. 바깥에 두면 중복으로 409가 나도 MediaRef가 CONFIRMED로 남아 상태가 어긋난다.

### 2.2 카운터는 엔티티가 아니라 원자적 UPDATE로

로그인 실패 카운터, 인증번호 실패 카운터, 발송 제한 카운터는 전부 JPQL `UPDATE`로만 증가시킨다. 엔티티를 읽어 `+1`하면 동시 요청이 서로의 증가분을 덮어써 제한이 우회된다. 실패 경로에서 카운터를 올릴 때는 `REQUIRES_NEW`여야 한다(호출자가 예외를 던져 롤백하므로).

관련: `AccountRepository.incrementLoginFailure`, `EmailVerificationRepository.incrementFailCount`, `VerificationRateLimitRepository.incrementIfPresent`.

이 규약 때문에 **도메인 엔티티에는 잠금 세터가 없다.** 테스트에서 잠긴 계정을 만들려면 리플렉션으로 `lockedUntil` 필드를 직접 설정한다(`LoginUseCaseTest.lockUntil` 참고).

### 2.3 `@ConditionalOnMissingBean` 함정

이 레포는 `@Component` + `@ConditionalOnMissingBean` 조합을 5곳에서 쓴다. Spring Boot 문서상 **자동설정 클래스 밖(컴포넌트 스캔 대상)에서는 평가 순서가 보장되지 않는다.** 지금 정상 동작하는 건 우연이다. 새 코드에서 이 조합을 쓰지 말 것 — 프로파일 기반 분리를 쓴다. (#63, #56에서 정리 예정)

### 2.4 테스트 환경 주의

`application-test.yaml`은 H2 PostgreSQL 모드 + `ddl-auto: create-drop`이고 **Flyway가 비활성**이다. 즉:

- 마이그레이션 SQL(`db/migration/V1xx__*.sql`)은 테스트로 전혀 검증되지 않는다
- 네이티브 쿼리(`ON CONFLICT DO NOTHING` 등)를 쓰면 H2 호환을 따로 확인해야 한다
- 테이블 DDL이 Hibernate 생성본이라 마이그레이션의 `DEFAULT` 절이 테스트에 반영되지 않는다

### 2.5 프로덕션 설정은 안전하다

`application.yaml`에 JWT 시크릿·DB 비밀번호의 취약한 기본값이 있지만, `application-prod.yaml`이 `JWT_SECRET`, `MAIL_HOST`, `DB_PASSWORD` 등을 **환경변수 필수**로 요구한다. dev 전용이므로 이슈로 올리지 않았다.

---

## 3. 등록된 이슈 (9건, 전부 OPEN)

라벨은 레포 컨벤션(`feat`/`fix`/`chore`/`refactor`)을 따랐고, 담당자는 전부 `@me`다.

### P1 — 배포 전 반드시

| # | 제목 | 요지 |
| --- | --- | --- |
| [#56](https://github.com/memeboomemeboo/haemi-server-v2/issues/56) | fix: 인증 없이 열려 있는 /internal/storage 엔드포인트 차단 | **가장 급함.** 아래 §3.1 |
| [#57](https://github.com/memeboomemeboo/haemi-server-v2/issues/57) | feat: 액세스 토큰 재발급(refresh) 엔드포인트 추가 | 아래 §3.2 |
| [#58](https://github.com/memeboomemeboo/haemi-server-v2/issues/58) | fix: 하루 한마디 중복 전송 시 409 대신 500 | **PR #51에서 수정 완료** (`e15ecef`) |
| [#59](https://github.com/memeboomemeboo/haemi-server-v2/issues/59) | fix: 처리되지 않은 예외가 로그에 남지 않음 | 아래 §3.3 |

#### 3.1 #56 — `/internal/storage` 무인증 노출

`common/security/SecurityConfig.java:24`가 `/internal/storage/**`를 `permitAll`로 열고, `platform/media/presentation/LocalStorageController.java:16`이 그 경로를 서빙한다. 컨트롤러는 `@ConditionalOnMissingBean(name = "s3StorageAdapter")`로 보호되지만 **레포 어디에도 S3 어댑터가 없어** 조건이 항상 참이 되고 prod에서도 등록된다.

영향:
- 누구나 `PUT /internal/storage/upload?key=<임의값>`으로 파일 업로드 가능
- 누구나 `GET /internal/storage/serve?key=<키>`로 타인의 사진·음성 다운로드 가능
- 실제 저장소가 `LocalObjectStorage`의 `ConcurrentHashMap`이라 모든 미디어가 힙에 쌓이고 재시작 시 소실
- presigned URL이 `http://localhost:8080` 하드코딩이라 외부 클라이언트 업로드 불가

할 일: S3(또는 R2) `StoragePort` 어댑터를 빈 이름 `s3StorageAdapter`로 구현. 그전까지는 `LocalStorage*` 3종과 permitAll 규칙을 `@Profile("!prod")`로 묶는다.

#### 3.2 #57 — refresh 엔드포인트 없음

로그인이 access(30분)/refresh(14일) 토큰을 발급하고 `refresh_tokens`에 저장하지만 **재발급 엔드포인트가 없다.** `RefreshTokenRepository.findByToken`은 호출자 없는 죽은 코드. 결과적으로 모든 사용자가 30분마다 재로그인해야 한다.

부수 결함: refresh 토큰에는 `role` 클레임이 없어, 클라이언트가 실수로 Authorization 헤더에 넣으면 `common/security/JwtAuthenticationFilter.java:29`의 `new SimpleGrantedAuthority(null)`이 예외를 던져 **401이 아니라 500**이 나간다.

#### 3.3 #59 — 예외 로깅 누락

`common/error/GlobalExceptionHandler.java:40`의 `handleUnexpected`가 예외를 인자로 받고도 아무것도 기록하지 않는다. 프로덕션에서 500이 나면 원인 추적이 불가능하다. `RequestIdFilter`가 MDC에 `requestId`를 넣으므로 함께 로깅하면 된다.

### P2 — 기능·품질

| # | 제목 | 요지 |
| --- | --- | --- |
| [#60](https://github.com/memeboomemeboo/haemi-server-v2/issues/60) | fix: 어르신 수신함에 발신자 이름 미표시 | `GreetingQueryImpl.java:34`가 `guardianName`에 항상 `null`을 넣는다. 어르신이 누가 보냈는지 알 수 없다. `AccountQuery.findAllById`로 일괄 조회할 것 |
| [#61](https://github.com/memeboomemeboo/haemi-server-v2/issues/61) | fix: 추억 이미지 4장 제한 미구현 | `haemi.media.image.memory-max-count: 4` 설정은 있는데 읽는 코드가 없다. `RegisterMemoryUseCase`/`UpdateMemoryUseCase`가 무제한 수용 |
| [#62](https://github.com/memeboomemeboo/haemi-server-v2/issues/62) | fix: UUID PK가 `Math.random()` 사용 | `UuidGenerator.java:22`. 예측 가능하고, double→long 변환에서 하위 11비트가 항상 0이라 msb 랜덤 12비트가 실질 1비트다. `SecureRandom.nextLong()`으로 교체 |
| [#63](https://github.com/memeboomemeboo/haemi-server-v2/issues/63) | refactor: 실구현으로 대체된 Query 스텁 3종 제거 | `AttendanceQueryStub`, `GreetingQueryStub`, `MemoryQueryStub`. §2.3 참고 |

### P3 — 묶음

[#64](https://github.com/memeboomemeboo/haemi-server-v2/issues/64) `chore: 전체 코드 리뷰에서 나온 소규모 정리 5건`

1. soft delete 우회 — `DeleteMemoryUseCase`, `GetMemoryResponsesUseCase`가 `deletedAt` 필터 없는 `findById` 사용
2. `GreetingReadCommandImpl.java:19`의 `markRead`가 없는 ID/타인 항목에 무음 200 반환 (404/403이어야 함)
3. 소비자 없는 이벤트 — `GreetingSent`, `MemoryRegistered`에 리스너가 하나도 없음
4. 죽은 설정 — `haemi.security.elder-health-encryption-key`를 바인딩하는 코드 없음
5. `daysTogether` 계산이 `AttendanceQueryImpl`과 `GetElderReportSummaryUseCase`에 KST 하드코딩으로 중복

---

## 4. 다음 세션이 이어서 할 일

1. **PR #51 머지는 사용자 지시 대기.** 리뷰·수정은 끝났고 테스트도 통과했다. 임의로 머지하지 말 것.
2. **#56부터 착수 권장** — 유일한 보안 노출이고, 프로덕션 미디어 저장이 인메모리라 기능적으로도 막혀 있다.
3. #57 → #59 → P2 순.

### 미결 판단 2건 (사용자 확인 필요)

- **#58 이슈 체크리스트를 지금 체크할지.** 수정은 PR #51에 들어갔지만 머지 전이라 이슈는 열어뒀고, 체크박스도 그대로다. "지금 체크할까요?"라고 물었으나 답을 받지 못했다.
- **`docs/client-backend-mapping.md` 커밋 여부.** untracked 상태이고 이전 세션 산출물이라 이번 세션에서 손대지 않았다.

---

## 5. 작업 방식 메모

- 사용자는 **한국어로 소통한다.** 커밋 메시지·이슈 본문·PR 설명도 전부 한국어다.
- 커밋 말미에 `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`를 붙인다.
- 이슈 제목 형식: `<type>: <작업 내용>` (`feat`/`fix`/`chore`/`refactor`). `.claude/skills/issue/SKILL.md` 참고.
- PR diff 리뷰는 `/code-review <PR번호>`. 전체 코드 리뷰는 모듈 단위로
  `find <모듈> -name '*.java' | xargs cat`을 임시 파일에 덤프해 읽으면 4~5회 Read로 커버된다(전체 7,200줄).
