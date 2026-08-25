# 해미 v2 — 업무 분담 (개발자 2인 · 에이전트 실행용)

> 확정일: 2026-08-22 / 업데이트: 2026-08-24 (담당·마이그레이션 표기 정합화)
> 대상: 개발자 2인. Phase 0과 기능 라인의 실제 담당을 분리해 기록.

**이 문서는 사람과 에이전트가 함께 읽습니다.** 에이전트는 §1을 먼저 읽고, 자신에게 배정된 라인의 태스크 카드만 수행합니다.

### 담당자 요약

| 담당자 | 주 담당 | 추가 책임 |
| --- | --- | --- |
| **황정빈** | 추억·소통 라인 + **Phase 0 주도** | `auth` 공동 해결, 배포 기반, 하네스·백로그 |
| **김연호** | 훈련·분석 라인 (**CIST 6 · RPT 6**) | `platform/content`, 훈련·출석·리포트 |

> **정리 기준**: Phase 0은 황정빈이 주도했다. `auth`는 다른 작업의 병목을 해소하기 위해 두 사람이 함께 해결했다. **CIST와 RPT는 김연호 라인으로 유지**한다.

---

## 1. 에이전트 운영 규칙 ★ 작업 시작 전 필독

### 1.1 근거 문서

아래 4개 **외의 추측으로 구현하지 않습니다.**

| 문서 | 무엇의 근거인가 |
| --- | --- |
| [v2-functional-spec.md](./v2-functional-spec.md) | 기능 요구사항. 수치·문구는 여기가 유일한 출처 |
| [v2-module-architecture.md](./v2-module-architecture.md) | 패키지 위치, 의존 방향, 모듈 간 통로 |
| [v2-architecture.md](./v2-architecture.md) | 기술 결정 (시간·영속성·이벤트·API 규약) |
| [v2-authorization.md](./v2-authorization.md) | 인가 규칙. **여기 없는 인가 판단은 구현 금지** |

### 1.2 멈추고 사람에게 물어야 하는 상황

다음에 해당하면 **추측해서 진행하지 말고 작업을 중단하고 질문**합니다.

1. 명세에 수치·문구가 없는데 필요한 경우 (예: 이미지 용량 상한)
2. [v2-authorization.md §6](./v2-authorization.md)의 미확정 항목에 부딪힌 경우
3. 기능명세서 [부록 B](./v2-functional-spec.md#부록-b-명세-결손-목록)의 결손 항목을 구현해야 하는 경우
4. 상대 라인의 파일을 수정해야만 진행되는 경우
5. 문서 간 내용이 서로 어긋나는 경우

**임의로 정하고 진행한 뒤 "가정했습니다"라고 보고하는 것은 금지입니다.** 두 에이전트가 각자 다르게 가정하면 통합 시점에 드러나고, 그때는 되돌리기 비쌉니다.

### 1.3 파일 소유권 — 절대 규칙

에이전트는 **자기 라인에 배정된 경로 밖의 파일을 생성·수정·삭제하지 않습니다.**

| 경로 | 소유 |
| --- | --- |
| `platform/media/**`, `guardian/memory/**`, `elder/memory/**`, `elder/response/**`, `guardian/dailycare/**`, `elder/inbox/**`, `elder/home/**` | **황정빈 전용** |
| `platform/content/**`, `elder/training/**`, `elder/attendance/**`, `guardian/report/**` | **김연호 전용** |
| `common/**`, `guardian/family/**`, `guardian/eldermanagement/**` | **Phase 0에서 확정. 이후 변경은 양쪽 합의 필요** |
| `auth/**`, `auth/api/**` | **황정빈 주도 · 김연호 협업** (병목 해소를 위한 공동 해결). 계약 변경 시 즉시 공유 |
| `guardian/api/**` | **계약. §3 참조. 변경 시 상대에게 즉시 알림** |

`guardian/presentation/**`와 `elder/presentation/**`는 **컨트롤러 파일 단위로 소유**합니다 (§3.3 표).

### 1.4 Flyway 버전 대역

마이그레이션 충돌은 가장 흔한 병렬 작업 사고입니다.

| 개발자 | 대역 |
| --- | --- |
| Phase 0 초기 스키마 | `V100` ~ `V105` (`V1__baseline.sql`은 중복 생성 위험으로 삭제됨) |
| **황정빈** | `V100` ~ `V199` |
| **김연호** | `V200` ~ `V299` |

**대역 밖 번호를 쓰지 않습니다.** 이미 머지된 마이그레이션은 **수정하지 않고** 새 버전을 추가합니다.

### 1.5 공용 파일 변경 규칙

| 파일 | 규칙 |
| --- | --- |
| `common/error/ErrorCode` | **추가만 허용**, 기존 값 수정·삭제 금지. 접두사 필수 (`MEMORY_*`, `TRAINING_*`, `REPORT_*`, `MEDIA_*`) |
| `application.yml` | 자기 모듈 네임스페이스(`haemi.memory.*`, `haemi.training.*`) 아래에만 추가 |
| `build.gradle` | 의존성 추가 시 PR 설명에 사유 명시. 상대 리뷰 필수 |
| `HaemiApplication.java` | Phase 0 이후 변경 금지 |

### 1.6 커밋·PR

- 브랜치: 황정빈 `feat/hjb-<모듈>` / 김연호 `feat/kyh-<모듈>` — 담당자 접두사로 소유를 드러냅니다.
- **두 라인은 각각 독립 스택.** 상대 브랜치 위에 쌓지 않습니다. 한쪽이 막히면 같이 막힙니다.
- PR 하나 = 태스크 카드 하나. 카드의 완료 조건을 PR 설명에 체크리스트로 옮깁니다.
- 리뷰어는 상대 개발자 1명. 승인권은 대등합니다.

### 1.7 하네스·백로그 운영

- **하네스 담당: 황정빈.** 에이전트가 작업 시작 전에 읽는 실행 규칙·컨벤션·태스크 연결 구조를 정리하고 GitHub에 지속 반영합니다.
- 업무 분담의 실제 진행 상태는 별도 `BACKLOG.md`에 태스크 카드 단위로 관리합니다. 각 카드는 최소한 **담당자 / 상태 / 선행 조건 / 산출물 / 완료 조건 / 막힘 사유**를 가집니다.
- 각 담당자는 자기 PR을 올릴 때 해당 백로그 카드 상태도 함께 갱신합니다.
- 하네스 수정이 아키텍처·인가·계약 자체를 바꾸는 경우에는 황정빈 단독으로 확정하지 않고 김연호 리뷰를 받습니다.
- 에이전트는 하네스 또는 백로그에 없는 새 범위를 임의로 만들지 않습니다. 새 작업이 필요하면 먼저 카드로 추가합니다.

### 1.8 모든 PR의 공통 완료 조건 (DoD)

- [ ] `./gradlew build` 통과
- [ ] `ApplicationModules.verify()` 통과
- [ ] ArchUnit AU-1/2/3 통과 ([인가 문서 §5](./v2-authorization.md))
- [ ] `elderId`를 다루는 유스케이스마다 **"권한 없는 보호자 → 403" 테스트 1건**
- [ ] 명세 수치를 하드코딩하지 않고 `@ConfigurationProperties`로 노출
- [ ] 시간 의존 로직에 `HaemiClock` 주입 (`LocalDate.now()` 직접 호출 금지)
- [ ] 어르신 응답 DTO에 점수·정답률 필드 없음

---

## 2. Phase 0 — 공통 토대 + 선행 담당

공통 계약·인가·초기 스키마처럼 두 라인에 동시에 영향을 주는 토대는 함께 확정합니다. **Phase 0은 황정빈이 주도**했고, 인증은 다른 작업의 병목을 해소하기 위해 김연호가 함께 해결했습니다. 여기서 책임자를 정하는 것은 독립 설계를 허용한다는 뜻이 아니며, 공통 규칙 변경은 반드시 상대 리뷰를 거칩니다.

| # | 내용 | 방식 | 산출 |
| --- | --- | --- | --- |
| 0-0 | 에이전트 하네스 + `BACKLOG.md` 정리 | **황정빈 담당 · 김연호 리뷰** | 실행 규칙·컨벤션·태스크 카드가 연결된 하네스 / 지속 갱신 가능한 백로그 |
| 0-1 | Gradle 프로젝트, 패키지 트리, Spring Modulith, CI | **페어** | `ApplicationModules.verify()` 통과하는 빈 뼈대 |
| 0-2 | `common` 전체 | **황정빈 주도 · 김연호 리뷰** | web·error·security·persistence·**time**·event·test |
| 0-3 | `auth` 전체 | **황정빈 주도 · 김연호 협업** | account·credential·verification·session + `auth/api` |
| 0-4 | `guardian/family` + `guardian/eldermanagement` + **`access`** | ✅ 완료 | `CareAccessQuery` 구현 + ArchUnit AU-1 |
| 0-5 | 초기 스키마 마이그레이션 | **황정빈** | `V100`~`V105` 순차 적용 (`V1__baseline.sql` 미사용) |
| 0-6 | §3 계약 시그니처 합의 + 스텁 머지 | ✅ 완료 | MemoryQuery·GreetingQuery·AttendanceQuery 스텁 머지 |
| 0-7 | 배포 기반 구성·실행 검증 | **황정빈** | Docker + EC2 배포 경로 |

**0-4를 페어로 하는 이유**: `CareAccessQuery`는 이후 모든 유스케이스의 첫 줄에서 호출됩니다. 한 사람만 이해하면 나머지 절반의 코드에서 호출이 빠지고, 그게 v1이 깨진 방식입니다.

**0-2의 `common/time`과 `common/test`는 Phase 0 범위로 황정빈이 주도합니다.** CIST 시간 시나리오에서 사용하는 경우에도 공용 도구의 변경은 양쪽 합의가 필요합니다.

**0-3 `auth`는 황정빈이 주도하고 김연호가 협업했습니다.** 인증 스텁/사용자 기반이 다른 작업의 병목이 되어 공동 해결한 이력이며, 계약이나 공통 보안 규칙을 바꾸는 경우 상대 리뷰 후 머지합니다.

**0-7 배포는 황정빈이 담당합니다.** Docker·EC2 배포 기반은 구성되어 있으며, 운영 비밀값·EC2 상태·배포 성공 여부는 배포 시점에 별도로 검증합니다.

---

## 3. 계약 — Phase 0 종료 시점에 확정

갈라지기 전에 **인터페이스 시그니처만 합의하고 스텁을 머지**합니다. 구현은 각자 나중에 채웁니다.

### 3.1 황정빈이 제공 → 김연호가 소비

```java
// guardian/api/MemoryQuery.java   [소유: 황정빈]
public interface MemoryQuery {
    /** CIST-TRN-003/004 재료. 어르신에게 전달된 추억 중 이미지가 있는 것. */
    List<MemoryMaterial> materialsFor(ElderId elderId, int limit);

    record MemoryMaterial(
        MemoryId id,
        String title,
        Integer memoryYear,      // 추억 연도 (nullable)
        List<String> imageKeys,
        LocalDate registeredAt
    ) {}
}
```

**황정빈은 1단계 초반에 이 인터페이스와 빈 구현(빈 리스트 반환)을 먼저 머지합니다.** 그래야 김연호가 황정빈의 구현을 기다리지 않고 5단계를 진행할 수 있습니다. 이것이 두 라인 사이의 **유일한 진짜 의존**입니다.

### 3.2 김연호가 제공 → 황정빈이 소비

```java
// elder/attendance 조회 — HOME "오늘 출석 여부"용   [소유: 김연호]
public interface AttendanceQuery {
    boolean completedToday(ElderId elderId);
    int currentStreak(ElderId elderId);
    long daysTogether(ElderId elderId);   // 첫 등록일부터 D+
}
```

김연호는 4단계에서 이 인터페이스를 머지합니다. 황정빈은 5단계(`HomeController`)에서 소비합니다.

### 3.3 컨트롤러 소유

| 파일 | 소유 |
| --- | --- |
| `guardian/presentation/FamilyController` | Phase 0 |
| `guardian/presentation/ElderController` | Phase 0 |
| `guardian/presentation/ProfileController` | **황정빈** |
| `guardian/presentation/MemoryController` | **황정빈** |
| `guardian/presentation/DailyCareController` | **황정빈** |
| `guardian/presentation/HomeController` | **황정빈** |
| `guardian/presentation/ReportController` | **김연호** |
| `elder/presentation/HomeController` | **황정빈** |
| `elder/presentation/MemoryController` | **황정빈** |
| `elder/presentation/ResponseController` | **황정빈** |
| `elder/presentation/InboxController` | **황정빈** |
| `elder/training/presentation/TrainingSessionController` | **김연호** |

---

## 4. 황정빈 — 추억·소통 라인

**담당 기능 10개**: ALB 3 · E-ALB 2 · HOME 3 · MYP 프로필 2
**성격**: 넓고 얕음. 외부 연동(스토리지)과 화면 조합이 몰려 있음.

### 황정빈-1 · `platform/media` + `MemoryQuery` 스텁

| | |
| --- | --- |
| **목표** | presigned URL 기반 미디어 파이프라인 |
| **산출** | `platform/media/{domain,application,infrastructure}`, `guardian/api/MemoryQuery`(스텁) |
| **참조** | [v2-architecture §5](./v2-architecture.md) |
| **완료 조건** | 업로드 URL 발급 → 직업로드 → 키 확정 3단계 동작 · `MediaRef` 상태 PENDING/CONFIRMED 전이 · **`MemoryQuery` 스텁 머지 완료** |
| **멈추고 물을 것** | 이미지 용량·포맷 상한, 음성 코덱, 보관 기간 — **명세에 없음** |

> ⚠️ `MemoryQuery` 스텁을 이 단계에서 머지하지 않으면 김연호가 5단계에서 막힙니다. **최우선.**

### 황정빈-2 · `guardian/memory`

| | |
| --- | --- |
| **목표** | 추억 등록·조회 (ALB 3기능) |
| **산출** | `guardian/memory/**`, `guardian/presentation/MemoryController` |
| **참조** | 기능명세서 §2.2, [인가 R5](./v2-authorization.md) |
| **핵심 수치** | 메모 300자 · 이미지 4장 · 조회 범위 최대 1년 |
| **완료 조건** | 등록 시 `requireGuardianOf` 첫 줄 호출 · 수정·삭제는 `created_by` 검증 · 링크된 모든 보호자가 조회 가능 · 403 테스트 |
| **금지** | `elder/**` 수정 |

### 황정빈-3 · `elder/memory` + `elder/response`

| | |
| --- | --- |
| **목표** | 어르신의 추억 조회·답변 (E-ALB 2기능) |
| **산출** | `elder/memory/**`(엔티티 없음), `elder/response/**`, 컨트롤러 2개 |
| **참조** | 기능명세서 §3.3, [인가 R9](./v2-authorization.md) |
| **핵심 수치** | 감정 7종 중 **최대 2개** · 댓글 텍스트 100자 · 음성 최대 1분 |
| **완료 조건** | `elder/memory`는 `guardian/api/MemoryQuery`만 호출(엔티티·리포지토리 없음) · `requireSelf` 호출 · 응답 DTO에 **생성자 관계·이름** 포함 · `ElderResponded` 이벤트 발행 |
| **금지** | `elder/memory`에 엔티티·리포지토리 생성 (추억 소유자는 `guardian/memory`) |

### 황정빈-4 · `guardian/dailycare` + `elder/inbox`

| | |
| --- | --- |
| **목표** | 하루 한마디, 도전과제 (HOME 2기능) |
| **산출** | `guardian/dailycare/**`, `elder/inbox/**`, `DailyCareController`, `InboxController` |
| **참조** | 기능명세서 §2.1, [인가 R6](./v2-authorization.md) |
| **핵심 수치** | 음성 1분 **또는** 텍스트 100자 (1택) · `(보호자, 어르신, 날짜)`당 1회 |
| **완료 조건** | **다른 보호자에게 안 보이는 것을 테스트로 증명** (R6) · 1일 1회 제한에 `HaemiClock` 사용 · `GreetingSent` 이벤트 |
| **멈추고 물을 것** | **하루 한마디 수신 화면(읽음 처리·보관 기간)이 명세에 없음.** 이 단계 진입 전 확정 필요 |

### 황정빈-5 · `guardian/presentation` 조합 + `elder/home`

| | |
| --- | --- |
| **목표** | 홈·프로필 조합 화면 (HOME 1 + MYP 2기능) |
| **산출** | `ProfileController`, `HomeController`(guardian), `elder/home/**` |
| **참조** | 기능명세서 §2.1 어르신 정보, §2.3 프로필 |
| **의존** | **김연호의 `AttendanceQuery`** (오늘 출석·함께한 일 수) |
| **완료 조건** | 조합 계층에 엔티티·리포지토리 없음 · `accessibleElders()`가 반환한 어르신만 표시 |
| **멈추고 물을 것** | **어르신 홈 화면이 명세에 없음.** 이 단계 진입 전 확정 필요 |

---

## 5. 김연호 — 훈련·분석 라인

**담당 기능 12개**: CIST 6 · RPT 6
**성격**: 좁고 깊음. 정량 명세의 규칙이 전부 여기 있고, **시간 시나리오 테스트가 이 프로젝트에서 가장 무거운 테스트**입니다.

### 김연호-1 · `platform/content`

| | |
| --- | --- |
| **목표** | 콘텐츠 풀과 출제 이력 |
| **산출** | `platform/content/**` (헥사고날) |
| **참조** | [기능명세서 §4.4 정량](./v2-functional-spec.md#4-인지-훈련-cist-정량-명세) |
| **핵심 수치** | 풀 500개 · 쿨다운 **7일** · 재투입 **14~30일** · 세션당 신규 3~5개 · 고갈 임계 **eligible < 20개** |
| **완료 조건** | 전부 `@ConfigurationProperties` · 고갈 시 쿨다운 무시 재투입 동작 · `availability.until` 만료 자동 제외 · `platform_content_exposures(elder_id, exposed_at)` 인덱스 |
| **테스트 필수** | 고정 Clock으로 **30일 시나리오** — 7일 쿨다운이 실제로 걸리는가, 풀 고갈 시 재투입되는가 |

### 김연호-2 · `elder/training` 세션 상태 머신

| | |
| --- | --- |
| **목표** | CIST-TRN-001, 006 — 세션 진입·이어하기·완료·결과 |
| **산출** | `elder/training/domain/TrainingSession`·`TrainingQuestion`·`TrainingAnswer`, `elder/training/presentation/TrainingSessionController` |
| **참조** | 기능명세서 §3.1, §4.1, [v2-architecture §8](./v2-architecture.md) |
| **핵심 규칙** | 하루 1회 (00:00 KST 리셋) · 미완료는 이어하기 · **완료 시 잠금, 당일 재진입은 결과 조회만** |
| **완료 조건** | `UNIQUE(elder_id, session_date)` 제약 · `session_date`는 `DATE` 컬럼 (시각에서 계산 금지) · 이탈은 정상 경로로 처리 · `TrainingSessionCompleted` 이벤트 발행 |
| **테스트 필수** | 자정 경계 — 23:59 시작 세션이 00:01에 어떻게 되는가 |

### 김연호-3 · `elder/training` 문항·난이도·지연 회상

| | |
| --- | --- |
| **목표** | CIST-TRN-002~005 |
| **산출** | `domain/{question,difficulty,delayedrecall}/**` |
| **참조** | 기능명세서 §3.1, §4.2, §4.3 |
| **핵심 수치** | 구성 지남력 3·회상 3·언어 2·지연회상 2 = **10문항** · Lv.1~3 (3택/4택, ±10/±5/±3년) · 상향 **2일 연속 ≥80%** · 하향 **≤40% 즉시** · **레벨은 영역별 독립** |
| **소스 우선순위** | ① `guardian/api/MemoryQuery` → ② `platform/content` (앨범 비면 fallback) |
| **완료 조건** | 지남력은 콘텐츠 없이도 생성 가능 · 지연 회상은 앞 항목 재질문 · **오답 페널티 없음** |
| **테스트 필수** | 고정 Clock으로 레벨 상향·하향 시나리오 |
| **금지** | `guardian/memory` 내부 접근 (`guardian/api`만) |

### 김연호-4 · `elder/attendance` + `AttendanceQuery`

| | |
| --- | --- |
| **목표** | 출석·스트릭·뱃지 |
| **산출** | `elder/attendance/**`, `AttendanceQuery` 인터페이스 |
| **참조** | 기능명세서 §4.5 |
| **핵심 수치** | 스트릭 = 정수, **자정 미완료 시 리셋** · 뱃지 7·30·100일 |
| **완료 조건** | `TrainingSessionCompleted` 멱등 소비 → 일별 `DailyParticipation` 기록 → `AttendanceRecorded` 발행 · **`AttendanceQuery` 머지** (황정빈의 5단계가 대기 중) |
| **테스트 필수** | 중복 훈련 이벤트가 출석을 중복 만들지 않는가 · "7일 연속 후 하루 빠지면 리셋되는가" |

### 김연호-5 · `guardian/report`

| | |
| --- | --- |
| **목표** | RPT 6기능 |
| **산출** | `guardian/report/**` (헥사고날 out 포트), `ReportController` |
| **참조** | 기능명세서 §2.4, [인가 R7](./v2-authorization.md) |
| **핵심 수치** | 영역별 정답률 **≥70% 🟢 / 40~70% 🟡 / <40% 또는 4주 연속 하락 🟠** · 출석 주 5일↑🟢 / 3~4일🟡 / 2일↓🟠 · 표시창 최근 7일 + 4주 |
| **완료 조건** | **두 원천 테이블 직접 조회 금지** — 인지는 `TrainingSessionCompleted`, 출석·참여는 `AttendanceRecorded`로 받은 스냅샷만 사용 · 출석 스냅샷은 `(elder_id, participation_date)` 멱등 적재 · 3색·스트릭은 **조회 시 계산** (배치 금지) · `accessibleElders()`로 목록 구성 · 어르신 간 비교·순위 기능 **미구현** · 정렬 🟠→🟡→🟢 |
| **문구 규칙** | 🟠도 "나쁨"이 아닌 관찰 신호 ("요즘 조금 어려워하세요") · 진단명·등수 미노출 |
| **참고** | `RPT-SUM-002`·`RPT-COG-004`는 존재하지 않는 ID ([부록 B](./v2-functional-spec.md#부록-b-명세-결손-목록)). 각각 `RPT-LST-002`·`RPT-ATT-004`로 해석 |

---

## 6. 진행 순서

| 단계 | 황정빈 | 김연호 | 동기화 지점 |
| --- | --- | --- | --- |
| Phase 0 선행 | **하네스·`BACKLOG.md` 정리**, 공통 토대, `auth` 주도, 배포 기반 | `auth` 협업 + 공통 토대 리뷰 | 공통 계약·인가·초기 스키마는 함께 확인 |
| 1 | 황정빈-1 `platform/media` | 김연호-1 `platform/content` | **황정빈이 `MemoryQuery` 스텁 머지** |
| 2 | 황정빈-2 `guardian/memory` | 김연호-2 세션 상태머신 | |
| 3 | 황정빈-3 `elder/memory`·`response` | 김연호-3 문항·난이도 | **황정빈의 `MemoryQuery` 실구현 완료** |
| 4 | 황정빈-4 `dailycare`·`inbox` | 김연호-4 `attendance` | **김연호가 `AttendanceQuery` 머지** |
| 5 | 황정빈-5 조합 화면 | 김연호-5 `report` | |
| 6 | 배포 최종 검증 + 함께 — `platform/notification`, 통합 테스트, QA | 함께 — `platform/notification`, 통합 테스트, QA | 릴리스 전 하네스·백로그와 실제 상태 일치 확인 |

---

## 7. 균형과 위험

| | 황정빈 | 김연호 |
| --- | --- | --- |
| 기능 수 | 10 | 12 |
| 모듈 수 | 8 | 4 |
| 무게 중심 | 통합·외부연동·인가 경로 | 알고리즘·시간 규칙 |

기능 수는 김연호가 많지만 모듈 수는 황정빈이 두 배입니다. 김연호의 12개 중 CIST 6개는 한 세션 안에서 유기적으로 묶여 있어 체감 작업량이 비슷합니다.

### 위험

| 위험 | 대응 |
| --- | --- |
| **Phase 0이 길어짐** | 전부 직렬이라 2명이 붙어도 2배가 안 됩니다. 대충 넘기면 이후 병렬 구간 전체가 흔들리므로 조급하게 나누지 않습니다 |
| **황정빈의 4·5단계가 명세 부재로 막힘** | 어르신 홈·하루 한마디 수신 화면. **황정빈이 3단계에 들어갈 때 확정 작업을 시작**해야 늦지 않습니다 |
| **김연호가 시작하자마자 어려운 걸 만남** | 쿨다운·재투입이 첫 과제. Phase 0의 `common/time`·`common/test`를 CIST 시나리오에 재사용 |
| **두 에이전트가 같은 가정을 다르게 함** | §1.2 — 추측 금지, 멈추고 질문 |
| **하네스와 실제 작업 상태가 어긋남** | 황정빈이 하네스를 관리하되, 각 담당자는 PR과 동시에 자기 `BACKLOG.md` 카드를 갱신 |
| **인증이 늦어 스텁 테스트가 막힘** | 황정빈 주도로 진행하되 병목 발생 시 김연호가 함께 해결하고, 계약 변경은 즉시 동기화 |
| **배포 설정이 막판에 몰림** | 황정빈이 Phase 0부터 Docker·EC2 기반을 잡고 단계 6에서 최종 검증. 미확정 인프라는 임의 결정 금지 |
| **Flyway 충돌** | §1.4 대역 분리 |
