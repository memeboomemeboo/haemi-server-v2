# 해미 v2 — 모듈 구조 (확정)

> 기준 문서: [v2-funcctional-spec.md](./v2-funcctional-spec.md) (기능 30개)
> 아키텍처 결정: [v2-architecture.md](./v2-architecture.md)
> 확정일: 2026-08-22

---

## 1. 패키지 구조

```
com.haemi
│
├── common
│   ├── web                     # 공통 API 응답, 페이징, 요청 ID
│   │   ├── ApiResponse
│   │   ├── PageResponse
│   │   └── RequestIdFilter
│   ├── error                   # 공통 예외 형식
│   │   ├── DomainException
│   │   ├── ErrorCode
│   │   └── GlobalExceptionHandler
│   ├── security                # JWT Principal, 인증 필터 공통부
│   │   ├── JwtPrincipal
│   │   ├── JwtAuthenticationFilter
│   │   └── SecurityConfig
│   ├── persistence             # JPA Auditing, UUID 생성 등
│   │   ├── BaseEntity
│   │   ├── UuidGenerator
│   │   └── AuditingConfig
│   ├── time                    # KST Clock — 자정 리셋·스트릭·쿨다운 기준
│   │   ├── HaemiClock
│   │   └── KstClock
│   ├── event                   # 도메인 이벤트 발행·아웃박스
│   └── test                    # 테스트 공통 도구 (고정 Clock, 픽스처)
│
├── auth
│   ├── account                 # 계정, 로그인 ID, 계정 상태
│   │   ├── domain              # User, LoginId, AccountStatus, Role
│   │   ├── application         # 회원가입, 계정 조회
│   │   └── infrastructure
│   ├── credential              # 비밀번호, PIN
│   │   ├── domain              # Password, Pin, PinPolicy
│   │   ├── application         # PIN 설정·검증·재설정
│   │   └── infrastructure
│   ├── verification            # SMS 본인 인증
│   │   ├── domain              # VerificationCode, Purpose
│   │   ├── application
│   │   └── infrastructure      # SMS 발송 어댑터
│   ├── session                 # 로그인·토큰·로그아웃
│   │   ├── domain              # Session, RefreshToken, DeviceInfo
│   │   ├── application         # 로그인, 갱신, 로그아웃, 얼굴 인증
│   │   └── infrastructure
│   ├── api                     # 다른 그룹이 호출하는 인터페이스
│   │   ├── AccountCommand      # 어르신 User 생성 (guardian이 호출)
│   │   └── AccountQuery
│   └── presentation
│       ├── SignUpController            # ACC-REG-001, 002
│       ├── LoginController             # ACC-LGN-001, 002
│       ├── LogoutController            # ACC-LGN-003
│       ├── FaceAuthController          # ACC-LGN-004
│       └── dto
│
├── guardian
│   ├── family                  # 가족 생성, 가족 멤버, 가족 프로필
│   │   ├── domain              # Family, FamilyMember, FamilyProfile
│   │   ├── application
│   │   └── infrastructure
│   ├── eldermanagement         # 어르신 계정 생성, 연결, 어르신별 보호자 역할
│   │   ├── domain              # Elder, GuardianElderLink, GuardianRole
│   │   ├── application         # 어르신 등록, 연결, 역할 지정
│   │   ├── access              # canAccess(보호자, 어르신) — 전 모듈 인가 관문
│   │   └── infrastructure
│   ├── memory                  # 앨범 생성·수정·공개, 사진 최대 4장
│   │   ├── domain              # Memory, MemoryImage, MemoryYear, Retention
│   │   ├── application
│   │   └── infrastructure
│   ├── dailycare               # 하루 메시지, 음성 메시지
│   │   ├── domain              # DailyGreeting, GreetingType, DailyChallenge
│   │   ├── application         # 하루 한마디 전송, 도전과제 제시·완료 판정
│   │   └── infrastructure
│   ├── report                  # 보호자용 리포트·대시보드
│   │   ├── domain              # ReportSnapshot, DomainStatus, Highlight, Support
│   │   ├── application         # 3색 판정, 하이라이트 생성, 서포트 제안
│   │   ├── listener            # elder.training 이벤트 수신 → 스냅샷 적재
│   │   └── infrastructure
│   ├── api                     # elder 그룹이 조회하는 인터페이스
│   │   ├── CareAccessQuery     # 인가 판정
│   │   ├── MemoryQuery         # 어르신에게 전달된 추억 조회
│   │   └── GreetingQuery
│   └── presentation
│       ├── FamilyController            # 가족 생성
│       ├── ProfileController           # 프로필 조회·수정
│       ├── ElderController             # 어르신 계정 생성·연결
│       ├── MemoryController            # 추억 등록·조회·어르신 답변 조회
│       ├── DailyCareController         # 하루 한마디, 도전과제
│       ├── ReportController            # RPT-LST-001,002 / ATT-003~006
│       ├── HomeController              # 어르신 정보 + 과제 + 한마디 조합
│       └── dto
│
├── elder
│   ├── home                    # 어르신 홈
│   │   └── application         # 오늘 훈련·받은 추억·한마디 조합 조회
│   ├── memory                  # 받은 앨범 목록·상세 조회
│   │   ├── application         # guardian.api.MemoryQuery 호출
│   │   └── infrastructure
│   ├── response                # 감정, 글, 사진, 음성 답변
│   │   ├── domain              # ElderResponse, Emotion, Comment, VoiceReply
│   │   ├── application         # 마음 전하기(최대 2개), 댓글 100자, 음성 1분
│   │   └── infrastructure
│   ├── inbox                   # 하루 한마디 수신·읽음 처리
│   │   ├── domain              # ReceivedGreeting, ReadState
│   │   └── application
│   ├── training                # 인지 훈련 (CIST-TRN-001~006)
│   │   ├── domain
│   │   │   ├── session         # TrainingSession, SessionState, Progress
│   │   │   ├── question        # Orientation, Recall, Language 문항 생성
│   │   │   ├── difficulty      # Lv.1~3, 영역별 독립, 상향 80% / 하향 40%
│   │   │   └── delayedrecall   # 지연 회상, 질문 프레임 전환
│   │   ├── application         # 세션 시작·이어하기·응답·완료·결과 조회
│   │   ├── event               # TrainingSessionCompleted → guardian.report
│   │   └── infrastructure
│   ├── attendance              # 출석·일일 활동
│   │   ├── domain              # DailyParticipation, Streak, Badge
│   │   ├── application         # 스트릭 계산, 7·30·100일 마일스톤
│   │   └── infrastructure
│   ├── companion               # 말동무 (FRI, 대기)
│   └── presentation
│       ├── HomeController              # 어르신 홈
│       ├── MemoryController            # 받은 추억 조회·상세
│       ├── ResponseController          # 마음 전하기·댓글·음성
│       ├── InboxController             # 하루 한마디 수신
│       ├── TrainingController          # CIST 세션·응답·결과
│       └── dto                         # ※ 점수·정답률 필드 금지
│
└── platform
    ├── media                   # 사진·음성 파일 검증·저장·서빙
    │   ├── domain              # MediaRef, MediaType, UploadPolicy
    │   ├── application         # presigned URL 발급, 키 확정
    │   └── infrastructure      # 스토리지 어댑터
    ├── content                 # 큐레이션 콘텐츠 풀 (CIST 재료)
    │   ├── domain              # ContentItem, ContentTag, ExposureHistory
    │   ├── application         # 쿨다운 7일, 재투입 14~30일, 고갈 임계 20개
    │   └── infrastructure
    ├── notification            # FCM 발송, 발송 실패·재시도
    │   ├── domain              # Notification, DeviceToken, DeliveryResult
    │   ├── application
    │   ├── listener            # 각 그룹 이벤트 구독
    │   └── infrastructure      # FCM 어댑터
    └── ai                      # Gemini 호출, 초안 생성·실패 기록
        ├── domain              # Prompt, Draft, FailureLog
        ├── application
        └── infrastructure
```

---

## 2. 의존 방향

```
elder ──────────▶ guardian ──────▶ auth ──────▶ common
  │                   │              │
  └───────┬───────────┴──────────────┘
          ▼
      platform ──────────────────────▶ common
```

| # | 규칙 |
| --- | --- |
| 1 | 그룹 간 호출은 **`api` 패키지를 통해서만**. `domain`·`infrastructure` 직접 import 금지 |
| 2 | 역방향(`guardian` → `elder`)은 **도메인 이벤트로만**. `elder/training` 완료 → `guardian/report`가 유일한 역방향 경로 |
| 3 | 엔티티를 그룹 밖으로 넘기지 않음. **ID와 DTO만** |
| 4 | `common`에 **엔티티 금지** |
| 5 | 트랜잭션 경계는 `application`. `domain`·`presentation`에 `@Transactional` 금지 |
| 6 | 모듈 간 FK 금지. 모듈 내부 FK는 허용 |

### 그룹 간 통로 (`api` 패키지)

| 제공 | 인터페이스 | 호출하는 쪽 |
| --- | --- | --- |
| `auth/api` | `AccountCommand` — 어르신 User 생성 | `guardian/eldermanagement` |
| `auth/api` | `AccountQuery` — 계정 조회 | `guardian` |
| `guardian/api` | `CareAccessQuery` — 인가 판정 | 전 모듈 |
| `guardian/api` | `MemoryQuery` — 전달된 추억 조회 | `elder/memory` |
| `guardian/api` | `GreetingQuery` — 하루 한마디 조회 | `elder/inbox` |

### 이벤트 (역방향)

| 발행 | 이벤트 | 구독 |
| --- | --- | --- |
| `elder/training` | `TrainingSessionCompleted` | `guardian/report`, `elder/attendance` |
| `elder/response` | `ElderResponded` | `platform/notification` |
| `guardian/dailycare` | `GreetingSent` | `elder/inbox`, `platform/notification` |
| `guardian/memory` | `MemoryRegistered` | `platform/notification` |

---

## 3. 모듈 내부 아키텍처

**그룹 간은 헥사고날(포트 강제), 모듈 내부는 계층형 기본.** 통일할 것은 경계이지 내부 스타일이 아닙니다.

| 모듈 | 스타일 | 이유 |
| --- | --- | --- |
| `elder/training` | **헥사고날** (in/out 포트 전면) | 출제 재료를 `memory` → `content` 순으로 갈아끼움. 소스를 포트로 추상화해야 규칙 테스트 가능 |
| `platform/content` | **헥사고날** | 쿨다운 7일·재투입 14~30일·고갈 임계가 자주 바뀔 값 |
| `guardian/report` | **헥사고날** (out 포트만) | 판정 규칙(70%/40%, 4주 하락)이 바뀔 여지가 큼 |
| `auth/*` · `guardian/family` · `guardian/eldermanagement` · `guardian/memory` · `elder/response` | **계층형** | 관계·검증 중심. 포트를 끼우면 보일러플레이트만 늘어남 |
| `guardian/dailycare` · `elder/inbox` · `elder/attendance` · `platform/notification` | **계층형 (얇게)** | 사실상 CRUD |

**근거**: v1은 13개 모듈 전부에 `port/` + `repository/`를 두어 CRUD 모듈에도 인터페이스가 2겹씩 생겼습니다.

---

## 4. 엔티티를 갖지 않는 모듈

| 모듈 | 역할 | 주의 |
| --- | --- | --- |
| `guardian/presentation/HomeController` | 조합 조회 | `eldermanagement` + `elder/attendance`를 합칠 뿐 |
| `elder/home` | 조합 조회 | 훈련·추억·한마디를 합칠 뿐 |
| `elder/memory` | 조회 전용 | 추억의 소유자는 `guardian/memory`. **양쪽에 엔티티를 두면 "이미지 4장" 규칙이 어디 있어야 할지 애매해짐** |
| `common/*` | 기술 공통 | 엔티티 금지 |

---

## 5. 명세 30기능 → 모듈 매핑

기능별 상세 표기는 [기능명세서](./v2-funcctional-spec.md)의 각 항목 `**모듈**` 줄, 역방향 조회는 [부록 A](./v2-funcctional-spec.md#부록-a-모듈별-담당-기능) 참조.

**표기 규칙**

| 기호 | 의미 |
| --- | --- |
| `A + B` | 두 모듈이 함께 처리 |
| `A → B` | A가 B를 호출 (동기) |
| `A ← B` | A가 B의 결과를 받음 (조회 또는 이벤트) |

### 짚어둘 매핑

- **ACC-REG-002 어르신 회원가입** — `guardian/eldermanagement`가 `auth/api/AccountCommand`에 User 생성을 요청하고, 반환받은 ID로 `GuardianElderLink`를 맺습니다. 가입 플로우 문서의 *"보호자 계정 아래에 종속시키지 않는다"* 를 지키는 순서입니다. 반대로 하면 종속 구조가 됩니다.
- **CIST-TRN-003/004** — 소스 우선순위(추억앨범 → 큐레이션)의 판단 주체는 `elder/training`입니다. `guardian/api/MemoryQuery`와 `platform/content`를 순서대로 조회합니다.
- **RPT-ATT-004** — `guardian/report`는 `elder/training`의 테이블을 직접 조회하지 않습니다. 이벤트로 받은 영역별 결과만 스냅샷에 쌓습니다.

---

## 6. 경계 강제

**Spring Modulith**로 검증을 자동화합니다. 관례로는 반드시 무너집니다.

```java
@ApplicationModule(allowedDependencies = { "guardian", "auth", "platform", "common" })
package com.haemi.elder;
```

```java
@Test
void 모듈_경계를_지킨다() {
    ApplicationModules.of(HaemiApplication.class).verify();
}
```

추가로 **ArchUnit** 규칙 두 가지를 CI 게이트로 둡니다.

1. `application` 패키지의 public 메서드 중 `ElderId`를 파라미터로 받는 것은 `CareAccessQuery` 호출을 반드시 포함한다.
2. `elder/presentation/dto`의 클래스는 점수·정답률 관련 필드명을 가질 수 없다.

DB도 같은 원칙 — **테이블 접두사를 그룹·모듈명으로** 통일하고(`auth_*`, `guardian_memory_*`, `elder_training_*`), 모듈 간 FK는 걸지 않습니다.

---

## 7. 착수 전 확정 필요

| 미정 사항 | 영향받는 모듈 | 왜 먼저 정해야 하나 |
| --- | --- | --- |
| **인가 규칙** (보호자 최대 인원, 합류 시점 기준 접근) | `guardian/eldermanagement/access` | v2를 새로 짓는 근본 이유. 코드 첫 줄 전에 확정 |
| **어르신 홈 화면** | `elder/home` | 어르신 앱의 진입점. 정의 없으면 만들 수 없음 |
| **하루 한마디 수신 화면** | `elder/inbox` | 읽음 처리·보관 기간이 스키마에 직접 영향 |
| 미디어 정책 (용량·포맷·보관) | `platform/media` | 스토리지 비용 직결 |
| PIN 재설정 플로우 | `auth/credential` | 어르신 사용자층 특성상 실사용 1순위 이슈 |
| 탈퇴·연결 해제 시 데이터 처리 | `guardian/memory` · `elder/training` · `guardian/report` | 나중에 정하면 마이그레이션 필요 |

상세는 [기능명세서 부록 B](./v2-funcctional-spec.md#부록-b-명세-결손-목록) 참조.
