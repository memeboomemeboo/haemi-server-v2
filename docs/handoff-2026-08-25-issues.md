# Hand-off — haemi-server-v2 오픈 이슈 처리 (2026-08-25)

> 다른 Claude 세션(다른 계정)으로 인계하는 문서. 이 문서만 읽고 이어서 작업할 수 있게 작성.
> 개발 규칙은 `.claude/hanes/CLAUDE.md`(해미 v2 개발 하네스)를 반드시 따른다.

---

## 0. ⚠️ 가장 중요 — 반드시 해야 할 남은 일

### #65 (P1) — **#37이 머지되면 즉시 처리해야 함**

- **현재 보류 상태.** 선행 의존 #37(CIST 세션·문항·정답 도메인)이 **아직 미구현**이라 착수하지 못했다.
- **#37이 머지되는 순간 #65를 반드시 진행해야 한다.** 그전까지는 "훈련하지 않은 어르신이 출석·스트릭·리포트를 조작 가능"한 **보안/무결성 결함(P1)** 이 그대로 남아 있다.
- 관련 이슈: https://github.com/memeboomemeboo/haemi-server-v2/issues/65 (블로커 코멘트 남겨둠)

**#65가 지금 막힌 이유 (사실 확인 완료)**
- `src/main/java/com/memeboo2/haemi/elder/training/` 에는 세션 엔티티·리포지토리·문항/정답 도메인이 **없다.**
- 존재하는 건 자리표시자 enum 2개(`SessionStatus{IN_PROGRESS,COMPLETED}`, `QuestionType`)와
  `CompleteTrainingSessionUseCase`, `TrainingSessionController`뿐.
- `CompleteTrainingSessionUseCase.completeToday()`가 **세션 유무/완료 상태를 전혀 검증하지 않고** 로그인만 하면
  `TrainingSessionCompleted` 이벤트를 발행 → `elder/attendance`, `guardian/report`가 그대로 출석·리포트로 적재.

**#37 머지 후 #65 구현 지침**
1. `CompleteTrainingSessionUseCase.completeToday(elderUserId)` 첫 부분(기존 `CareAccessQuery` 검증 유지)에서
   #37이 도입한 **세션 리포지토리로 "오늘(KST) 세션이 `COMPLETED` 상태인지" 조회·검증**을 추가한다.
2. COMPLETED가 아니면 `TrainingSessionCompleted`를 **발행하지 않고** 도메인 예외로 거부한다
   (새 ErrorCode 필요 시 `TRAINING_*` 접두사, **추가만**).
3. 날짜는 `HaemiClock` 사용(`LocalDate.now()` 직접 호출 금지).
4. 테스트 2종 이상:
   - `미완료_세션이면_이벤트_미발행_및_거부`
   - `완료된_세션만_TrainingSessionCompleted_발행`
   - 인가 실패(권한 없는 접근 403) 1건.
5. 이상적으로는 이슈 본문대로 **발행 지점을 세션 완료 처리(#37의 서비스)로 옮기고** 이 유스케이스는 제거하는 방향도 검토
   (기존 주석에 그 의도가 적혀 있음). 단, 소비자(`elder/attendance`)는 그대로 둔다.

---

## 1. 현재 상태 한눈에

- 작업 브랜치: `integration/2026-08-25-13-issues` (원격 푸시됨)
- 통합 PR(단일, main으로 한 번 머지): **#78**
  https://github.com/memeboomemeboo/haemi-server-v2/pull/78
- 기준 브랜치: `origin/main` (레포에 `develop` 브랜치 없음)
- `./gradlew clean build` **통과** (컴파일·전체 테스트·ArchUnit AU-1/2/3·ModuleVerification·Flyway 체인)
- 초기에 이슈별 스택 PR(#66~#77)을 올렸으나, 사용자가 통합 브랜치 단일 머지를 택해 **전부 닫고 브랜치 삭제**. 지금 유효한 건 #78 하나.

---

## 2. 완료한 이슈 (커밋 순서 = 스택 순서)

| 커밋 | 이슈 | 내용 |
|---|---|---|
| 1 | #59 | `GlobalExceptionHandler` 미처리 예외 error 로깅, 5xx/4xx 레벨 구분, 로그 패턴에 MDC `requestId` |
| 2 | #56 | `/internal/storage` 3컴포넌트 `@Profile("!prod")` + SecurityConfig non-prod 게이팅 |
| 3 | #62 | UUID v7 랜덤 비트 `Math.random()`→`SecureRandom`, RFC 9562 검증 테스트 |
| 4 | #61 | 추억 이미지 장수 제한 — `MediaUploadCommand.memoryImageMaxCount()` 노출 후 초과 시 400 |
| 5 | #60 | 하루 한마디 발신자 이름 — `AccountQuery.findAllById` 일괄 조회(N+1 방지) |
| 6 | #63 | Query 스텁 3종(`AttendanceQueryStub`/`GreetingQueryStub`/`MemoryQueryStub`) 제거 |
| 7 | #57 | `POST /api/v1/auth/refresh` + 토큰 회전, role 없는 토큰 401(500 방지), `AUTH_REFRESH_TOKEN_INVALID` |
| 8 | #54 | `ElderResponded`에 `respondedDate` 추가, legacy는 원본 생성일 유도, `HaemiClock.toLocalDate` |
| 9 | #53 | `GreetingRead` 이벤트(최초 1회 발행) + **#64-2**(무음 실패→404/403) |
| 10 | #55 | 추억 열람 기록(B안: `POST .../{memoryId}/viewed`) + `MemoryViewed` + V121 |
| 11·12 | #52 | `ActivityType` 4종 + 종류별 플래그(V122) + `AttendanceRecorder` + 리스너 4종 + 홈 주간 배열/리포트 종류 플래그 |
| 13 | #64 | 소규모 정리 항목 1·3·4·5 (2번은 #53에 포함) |

### 결정/가정 기록
- **#55**: 이슈가 권장·기본으로 명시한 **B안(명시적 열람 엔드포인트)** 채택.
- **#52**: 선행 #53·#55를 같은 스택에서 완료했으므로 **4종 리스너 전부 연결**(원래 이슈는 TRAINING·REPLIED만 요구).
  같은 종류 여러 번 = 1회 집계(플래그), 점수/정답률 미노출(RPT-ATT-004·AU-2 준수).
- **Flyway**: V121(#55 열람 테이블), V122(#52 종류 플래그 + 기존행 TRAINING 백필). 둘 다 황정빈 대역(100~199).

---

## 3. 남은 후속 작업 (우선순위 순)

1. **#65 — #37 머지 즉시 처리 (P1).** 위 §0 지침 참조. ← 최우선.
2. **#56 prod 미디어 업로드 fail-safe.** #56 머지 후 prod엔 `StoragePort` 어댑터가 없어
   `RequestUpload/ConfirmUploadUseCase`가 주입 실패 → **prod 기동 불가**. 이는 보안상 의도된 fail-safe다.
   실배포 전 **S3(또는 R2) `StoragePort` 어댑터(빈 이름 `s3StorageAdapter`)** 를 구현해야 한다.
   presigned URL의 `http://localhost:8080` 하드코딩도 어댑터에서 실제 도메인으로 교체 필요.
3. **#60 관계 라벨(딸/아들) — 기획 확인 필요.** `GreetingQuery.ReceivedGreeting` 계약에 필드가 없어 이번 범위 제외.
   추억 화면의 `creatorRole`과 일관성 맞출지 제품 결정 후 후속 이슈로. (이슈 #60에 코멘트 남김)

---

## 4. 검증 방법

```bash
./gradlew clean build          # 전체(컴파일+테스트+ArchUnit+ModuleVerification+Flyway)
./gradlew test --tests "*ArchitectureTest*"      # AU-1/2/3
./gradlew test --tests "*ModuleVerification*"
```

### 개발 규칙 (하네스 요약 — 자세한 건 `.claude/hanes/CLAUDE.md`)
- `ElderId`/`elderUserId` 받는 유스케이스 첫 줄: `CareAccessQuery.requireGuardianOf()` 또는 `requireSelf()` 필수.
  (단, 신뢰된 이벤트 리스너가 호출하는 내부 협력자는 예외 — 예: `AttendanceRecorder.record`는 package-private으로 두어 AU-1 회피)
- 어르신 DTO에 `score/correct/rate/accuracy/rank/level` 필드명 금지(AU-2).
- `LocalDate.now()` 직접 호출 금지 → `HaemiClock` 주입(`clock.today()`/`clock.now()`/`clock.toLocalDate(instant)`).
- 새 ErrorCode: 모듈 접두사 필수(`TRAINING_*`, `MEMORY_*` …), **추가만**(수정·삭제 금지).
- Flyway: 황정빈 V100~V199 / 김연호 V200~V299. 머지된 마이그레이션 수정 금지, 새 번호 추가.
  `elder/attendance`·`guardian/report`·`elder/training` 등 CIST/RPT는 원래 **김연호 라인**(V2xx) — #65는 이 라인.

---

## 5. 참고 파일 위치

- 이벤트 계약: `src/main/java/com/memeboo2/haemi/common/event/` (`AttendanceRecorded`, `ElderResponded`, `GreetingRead`, `MemoryViewed`, `TrainingSessionCompleted`, `GreetingSent`, `MemoryRegistered`)
- 활동 종류: `common/attendance/ActivityType`, `common/attendance/DaysTogetherCalculator`
- #65 대상: `elder/training/application/CompleteTrainingSessionUseCase.java`
- #52 기록측: `elder/attendance/application/AttendanceRecorder.java` + `*Listener.java` 4종
- #52 조회측: `guardian/api/AttendanceQuery.java`(`weeklyActivities`), `guardian/home/.../GuardianHomeResponse.java`, `guardian/report/.../AttendanceDetailResponse.java`
