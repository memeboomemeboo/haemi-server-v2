# Hand-off — 오픈 이슈 13건 작업 계획

> 작성일: 2026-08-25 / 작성자 세션에서 정리. 새 채팅 세션은 이 문서만 읽고 바로 이어서 작업 가능하도록 구성.
> 이 문서는 임시 작업 메모입니다. `docs/v2-*.md`(공식 스펙)와는 별개이며, 실제 근거는 항상 아래 "근거 문서" 절의 원본을 확인할 것.
>
> **2026-08-26 정정:** 이 문서의 13개 이슈 라운드는 완료됐다. RPT-ATT-004~006은 후속 PR
> [#90](https://github.com/memeboomemeboo/haemi-server-v2/pull/90)으로 `main`에 병합됐다. 새 작업은 이 문서의
> 순서표가 아니라 [v2-backlog.md](./v2-backlog.md)와 GitHub 이슈를 기준으로 시작한다.

## 0. 담당자 / 저장소

- 담당자: 황정빈 (GitHub: `hjbin-25`, email: aa01034795025@gmail.com)
- 저장소: `memeboomemeboo/haemi-server-v2` (origin), 기본 브랜치 `main`
- **이번 라운드는 13개 오픈 이슈를 전부 본인이 진행하기로 결정함.** 원래 [v2-work-assignment.md](./v2-work-assignment.md) 상 김연호 라인(`elder/attendance`, `guardian/report`)이던 #24와, 기획 결정이 필요하던 #22/#28/#29/#30도 포함해서 전부 처리.

## 1. 필수 선행 독서 (근거 문서)

작업 전 아래 문서를 반드시 참고. **이 문서들 외의 추측으로 구현 금지** (`v2-work-assignment.md` §1.1 규칙).

| 문서 | 용도 |
| --- | --- |
| [v2-work-assignment.md](./v2-work-assignment.md) | 파일 소유권(§1.3), Flyway 대역(§1.4), DoD(§1.8) |
| [v2-functional-spec.md](./v2-functional-spec.md) | 기능 요구사항 수치·문구의 유일한 출처 |
| [v2-module-architecture.md](./v2-module-architecture.md) | 패키지 위치, 의존 방향 |
| [v2-architecture.md](./v2-architecture.md) | 시간·영속성·이벤트·API 규약 |
| [v2-authorization.md](./v2-authorization.md) | 인가 규칙 (R1~R9 등) — 여기 없는 인가 판단은 구현 금지 |
| [v2-guardian-peer-access.md](./v2-guardian-peer-access.md) | 각 이슈 본문이 링크하는 근거(D-번호 결정사항, A-번호 항목) 원본 |
| [v2-open-questions-for-product.md](./v2-open-questions-for-product.md) | 미결 사항 A/B (이슈 #22, #30이 참조) |

### Flyway 버전 대역
- 담당자 대역: `V100`~`V199` (이미 머지된 마이그레이션은 수정 금지, 새 버전 추가만)

### 모든 PR 공통 DoD (v2-work-assignment.md §1.8)
- [ ] `./gradlew build` 통과
- [ ] `ApplicationModules.verify()` 통과
- [ ] ArchUnit AU-1/2/3 통과
- [ ] `elderId` 다루는 유스케이스마다 "권한 없는 보호자 → 403" 테스트 1건
- [ ] 명세 수치는 하드코딩 대신 `@ConfigurationProperties`
- [ ] 시간 의존 로직은 `HaemiClock` 주입 (`LocalDate.now()` 직접 호출 금지)
- [ ] 어르신 응답 DTO에 점수·정답률 필드 없음

## 2. 작업 순서 (13건, 의존관계 반영)

상태 표기는 각 세션이 진행하면서 `시작 전 → 진행 중 → 완료`로 직접 갱신할 것.

### 1구간 · 추억(Memory) 응답 정합성 — 프론트 블로커, 최우선

| 순서 | 이슈 | 상태 | 핵심 작업 |
| --- | --- | --- | --- |
| 1 | [#23](https://github.com/memeboomemeboo/haemi-server-v2/issues/23) 추억 응답 createdBy 누락 수정 | 완료 (PR [#38](https://github.com/memeboomemeboo/haemi-server-v2/pull/38)) | `guardian/memory/presentation/dto/MemoryDetailResponse.java`, `MemorySummaryResponse.java`에 `creatorName`/`creatorRole`/`isMine` 추가 |
| 2 | [#32](https://github.com/memeboomemeboo/haemi-server-v2/issues/32) creatorName/creatorRole null 케이스 처리 | 완료 (PR [#39](https://github.com/memeboomemeboo/haemi-server-v2/pull/39), #38 위에 스택) | null 발생 조건 문서화, fallback 값은 넣지 않기로 결정 |
| 3 | [#33](https://github.com/memeboomemeboo/haemi-server-v2/issues/33) 기타 호칭(D17) + R4 문구 정합화 | 완료 (PR [#40](https://github.com/memeboomemeboo/haemi-server-v2/pull/40), #39 위에 스택) | `creatorRole`이 "기타"일 때만 "보호자"로 치환 / `v2-authorization.md` R4 문구를 1년 컷 정책에 맞게 수정 |

### 2구간 · 인증/계정 — 보안 우선, 독립적으로 진행 가능

| 4 | [#26](https://github.com/memeboomemeboo/haemi-server-v2/issues/26) 인증코드·로그인 시도 횟수 제한 | 완료 (PR [#41](https://github.com/memeboomemeboo/haemi-server-v2/pull/41)) | 확인 5회 실패 시 잠금, 재발송 1시간 5회 제한, 로그인 5회 실패 시 15분 계정 잠금. IP 기준 제한은 인프라 부재로 이번 범위 제외 |
| 5 | [#25](https://github.com/memeboomemeboo/haemi-server-v2/issues/25) 마지막 접속 시각 필드 | 완료 (PR [#42](https://github.com/memeboomemeboo/haemi-server-v2/pull/42), #41 위에 스택) | D14: 보호자 홈의 어르신 카드에만 노출 (Q5-1 컨디션 카드 시안 기준), 리포트·어르신 본인 홈에는 미포함 |
| 6 | [#29](https://github.com/memeboomemeboo/haemi-server-v2/issues/29) 어르신 전화번호 중복 처리 정책 | 완료 (PR [#43](https://github.com/memeboomemeboo/haemi-server-v2/pull/43)) | **결정: 중복 허용, 병합 없음.** 각 가족이 독립 계정 등록 (현행 유지, 코드 변경 없음). D23으로 문서화 |

### 3구간 · 가족(Family)/역할(Role) — 내부 의존 있음, 순서 필수

| 7 | [#27](https://github.com/memeboomemeboo/haemi-server-v2/issues/27) `Family.guardianCount()` 버그 수정 | 완료 (PR [#44](https://github.com/memeboomemeboo/haemi-server-v2/pull/44)) | `memberType == GUARDIAN` 필터링으로 수정. `elderCount()`는 죽은 코드로 확인되어 삭제 |
| 8 | [#21](https://github.com/memeboomemeboo/haemi-server-v2/issues/21) 초대 코드 기반 가족 합류 | 완료 (PR [#45](https://github.com/memeboomemeboo/haemi-server-v2/pull/45), #44 위에 스택) | D4: 8자리 초대 코드 발급, `POST /families/join`으로 변경. 어르신 차단은 SecurityConfig의 역할 라우팅으로 이미 보장됨 확인 |
| 9 | [#31](https://github.com/memeboomemeboo/haemi-server-v2/issues/31) 관계 라벨 변경 API 에러 코드 통일 | 완료 (PR [#46](https://github.com/memeboomemeboo/haemi-server-v2/pull/46), #45 위에 스택) | `UpdateGuardianProfileUseCase`가 `ChangeGuardianRoleUseCase`에 위임하도록 통합, `NOT_RESOURCE_OWNER`로 일원화 |
| 10 | [#30](https://github.com/memeboomemeboo/haemi-server-v2/issues/30) GuardianRole 호칭 확장 + 코드값/표시명 분리 | 완료 (PR [#47](https://github.com/memeboomemeboo/haemi-server-v2/pull/47), #46 위에 스택) | **결정: 호칭 확장 안 함(현행 6개 유지).** 코드값/표시명 분리만 진행, Flyway 데이터 마이그레이션 포함. ⚠️ #33과 심볼 충돌 — 머지 순서 조율 필요 (PR 본문 참고) |
| 11 | [#22](https://github.com/memeboomemeboo/haemi-server-v2/issues/22) 가족 세부조회 구성원 노출 여부 | 완료 (PR [#48](https://github.com/memeboomemeboo/haemi-server-v2/pull/48), #47 위에 스택) | **결정: 이름+관계 모두 노출.** `GET /families/my` 신설, `ProfileResponse.family` 중복 제거 (breaking change) |

### 4구간 · 하루 한마디 — 다른 구간과 독립

| 12 | [#28](https://github.com/memeboomemeboo/haemi-server-v2/issues/28) 보낸 하루 한마디 이력 조회 API | 완료 (PR [#49](https://github.com/memeboomemeboo/haemi-server-v2/pull/49)) | **결정: 추가한다.** `GET .../daily-care/sent` 신설. R6: 발신자 본인 것만 반환 |

### 5구간 · 출석·리포트 — 분량 가장 큼, 신규 모듈, 마지막에 독립 집중

| 13 | [#24](https://github.com/memeboomemeboo/haemi-server-v2/issues/24) `elder/attendance` 실구현 + `guardian/report` 신규 모듈 | 완료 (PR [#50](https://github.com/memeboomemeboo/haemi-server-v2/pull/50)) | RPT-LST-001/002 + RPT-ATT-003 구현. 후속 RPT-ATT-004/005/006도 PR [#90](https://github.com/memeboomemeboo/haemi-server-v2/pull/90)으로 완료 |

## 3. #24 상세 (완료 — PR #50)

원래 김연호 라인(김연호-4, 김연호-5) 소관이던 모듈. 이번엔 본인이 진행, PR #50으로 완료.

- **elder/attendance**: `DailyParticipation`(출석의 유일한 원천) 완성. `TrainingSessionCompleted` 이벤트를 원자적으로 멱등 소비해 기록 → `AttendanceRecorded` 발행하는 리스너, `AttendanceQueryStub`를 대체하는 `AttendanceQueryImpl` 모두 완료. 스트릭은 최신 참여일을 내림차순으로 읽다가 첫 공백에서 멈추며, 자정 미완료 시 즉시 0으로 리셋된다.
  - **`elder/training` 발행처**: PR [#37](https://github.com/memeboomemeboo/haemi-server-v2/pull/37)의 `TrainingSessionService`가 10번째 문항 완료 시 `TrainingSessionCompleted`를 발행한다. 기존의 세션 없는 임시 완료 경로와 컨트롤러는 제거하며, 출석·리포트 소비자는 그대로 유지한다.
- **guardian/report**: 신규 구현 완료. `ReportParticipation` 스냅샷(원천 테이블 직접 조회 금지 원칙 준수), 3색 상태(D11/D12: 수치 미노출, 참여 게이지)를 조회 시 계산. 구현한 엔드포인트: `GET /report/elders`(RPT-LST-001), `GET /elders/{elderId}/report/summary`(RPT-LST-002), `GET /elders/{elderId}/report/attendance`(RPT-ATT-003).
  - **RPT-ATT-004~006**은 후속 PR [#90](https://github.com/memeboomemeboo/haemi-server-v2/pull/90)으로 `main`에 병합됐다. CIST 완료 시 `CognitiveTrainingCompleted`가 영역별 자동채점 집계를 별도 스냅샷으로 전달하며, 리포트는 훈련 원천 테이블을 직접 읽지 않는다.
  - **RPT-ATT-005**는 `platform/ai` 문구 생성 포트와 결정적 안전 fallback을 사용한다. 외부 모델 어댑터는 모델·자격증명·실패 정책 확정 후 추가한다.
  - RPT-LST-001/002의 "종합상태" 배지는 RPT-COG-004(→RPT-ATT-004)가 아니라 **D11 정책에 따라 RPT-ATT-003(참여 빈도) 기준으로 산출** — 인지 데이터 의존을 피할 수 있는 유일한 방법이라 이렇게 결정.
- 참고: `RPT-SUM-002`, `RPT-COG-004`는 기능명세서에 없는 ID → [부록 B](./v2-functional-spec.md#부록-b-명세-결손-목록) 참조, 각각 `RPT-LST-002`·`RPT-ATT-004`로 해석.
- 근거: [기능명세서 §4.5](./v2-functional-spec.md), [인가 R7](./v2-authorization.md)

### 후속 운영 항목

RPT-ATT-004~006은 병합 완료다. 외부 모델을 하이라이트에 연결할지와 그 자격증명·실패 정책은 기능 완료와 별개의 운영 확장이다. CIST-TRN-003/004의 큐레이션 콘텐츠 500건 원본 적재도 필요하다.

## 4. 진행 중 유의사항

- `guardian/family`, `guardian/eldermanagement`, `common/**`은 Phase 0 공동소유 파일(원래는 김연호와 합의 필요 대상)이었으나, 이번엔 본인이 전부 진행하므로 별도 합의 불필요. 다만 나중에 팀에 공유할 땐 3구간(#27, #21, #31, #30, #22) 변경 사항을 김연호에게 알릴 것.
- 각 "본인이 정책 직접 결정" 항목(#29, #30, #22, #28)은 결정 내용을 이슈 코멘트나 `v2-open-questions-for-product.md`에 기록해두면 나중에 근거 추적이 쉬움.
- 진행하다 "멈추고 사람에게 물어야 하는 상황"(v2-work-assignment.md §1.2)에 부딪히면 — 명세에 없는 수치, 인가 미확정 항목, 부록 B 결손 항목, 문서 간 불일치 — 추측하지 말고 표시해둘 것.

## 5. 새 세션에서 시작하는 법

1. [v2-backlog.md](./v2-backlog.md)에서 현재 기능 상태를 읽는다.
2. GitHub 이슈 본문은 `gh issue view <번호>`로 확인한다.
3. 제품 결정이 필요한 경우 [v2-open-questions-for-product.md](./v2-open-questions-for-product.md)를 갱신하고, 확정 전에는 구현 범위를 넓히지 않는다.
