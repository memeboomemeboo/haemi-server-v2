# 해미 – 디자인 기준 API 명세서 (신규 · 수정)

> 근거: 확정 디자인 33화면 전수 + 현재 컨트롤러/DTO 대조 ([design-api-gap.md](./design-api-gap.md))
> 작성일: 2026-08-27
> 이 문서는 **디자인을 충족하기 위해 새로 만들거나 고쳐야 하는 API만** 다룬다. 이미 일치하는 API는 제외.
> 구현 상태: Part A·B의 계약은 2026-08-27 구현·테스트 완료. 변경 전 문제 설명은 결정 근거로 보존하며, 현재 상태는 [design-api-gap.md §6](./design-api-gap.md#6-구현-동기화-상태-2026-08-27)를 따른다.

## 공통 규약

- **응답 봉투**: 모든 응답은 `ApiResponse<T>` = `{ "data": <T|null>, "error": <null | {code, message, field}> }` (null 필드는 직렬화 제외)
- **인증**: `Authorization: Bearer <accessToken>`. 보호자 API는 `guardianId`, 어르신 API는 `elderUserId`가 토큰에서 주입됨
- **케어 인가**: 보호자가 대상 어르신에 링크되지 않으면 `403 CARE_ACCESS_DENIED` (존재하지 않는 elderId도 링크 부재로 403)
- **날짜/시각**: 날짜 `YYYY-MM-DD`, 시각은 ISO-8601 `Instant`(UTC). 타임존 기준 KST(Asia/Seoul)

---

# Part A. 신규 엔드포인트

## A1. 아이디 중복 확인  🔴신규

| 항목 | 값 |
| --- | --- |
| 화면 | 회원가입 / 프로필 수정 / 어르신 등록 의 **"중복 확인"** 버튼 |
| Method · Path | `GET /api/v1/auth/login-id/availability` |
| 인증 | **불필요** (회원가입 단계에서 호출) |

**Query**

| 이름 | 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| `loginId` | string | ✅ | 4~50자 |

**응답 200**
```json
{ "data": { "loginId": "jeongeun", "available": true }, "error": null }
```

**에러**
- `400 INVALID_INPUT` — 형식 위반(길이/문자)

**비고**: 사용 가능 여부만 반환하며 예약(선점)은 하지 않는다. 최종 중복 검증은 회원가입/등록 제출 시 `409 LOGIN_ID_ALREADY_TAKEN`으로 재확인.

---

## A2. 보호자 홈 – 오늘의 기록(활동 타임라인)  🔴신규

| 항목 | 값 |
| --- | --- |
| 화면 | 보호자 홈 "오늘의 기록" + `자세히 보기` (인지 활동 완료 09:20·기억력 게임 5분·정답률 80%, 음성 메시지 도착 11:05·"밥 잘 먹었다" …) |
| Method · Path | `GET /api/v1/guardian/elders/{elderId}/activities` |
| 인증 | 보호자 (해당 어르신 링크 필요) |

**Query**

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `date` | string(`YYYY-MM-DD`) | ❌ | 오늘(KST) | 조회 일자 |

**응답 200**
```json
{
  "data": {
    "date": "2026-09-08",
    "items": [
      {
        "type": "TRAINING_COMPLETED",
        "occurredAt": "2026-09-08T00:20:00Z",
        "title": "인지 활동 완료",
        "detail": { "activityName": "기억력 게임", "durationMinutes": 5, "accuracy": 80 }
      },
      {
        "type": "GREETING_ARRIVED",
        "occurredAt": "2026-09-08T02:05:00Z",
        "title": "음성 메시지 도착",
        "detail": { "medium": "VOICE", "preview": "밥 잘 먹었다", "durationSeconds": 24 }
      }
    ]
  },
  "error": null
}
```

**`type` 열거**

| type | 발생원 | detail 필드 |
| --- | --- | --- |
| `TRAINING_COMPLETED` | 인지 훈련 완료 | `activityName`, `durationMinutes`, `accuracy`(0~100) |
| `GREETING_ARRIVED` | 하루 한마디 수신 | `medium`(`TEXT`\|`VOICE`), `preview`, `durationSeconds?` |
| `GREETING_READ` | 하루 한마디 읽음 | — |
| `MEMORY_VIEWED` | 추억 열람 | `memoryId`, `memoryTitle` |
| `RESPONSE_SENT` | 어르신 추억 답변 | `memoryId`, `responseType` |

**정렬**: `occurredAt` 오름차순. **에러**: `403 CARE_ACCESS_DENIED`

---

## A3. 이번 주 하이라이트 편집  🔴신규

> **결정(둘 다 남김)**: 연필 아이콘을 편집 기능으로 보고 편집 API를 **구현해 둔다**(조회 + 편집 병행).

| 항목 | 값 |
| --- | --- |
| Method · Path | `PATCH /api/v1/guardian/elders/{elderId}/report/highlight` |
| 인증 | 보호자 (해당 어르신 링크 필요) |

**요청**
```json
{ "items": [ { "id": "b3f1…", "title": "추억 회상에 좋은 반응", "body": "고향 사진을 보고 이야기를 활발히 들려주셨어요." } ] }
```

**응답 200**: 수정된 `WeeklyHighlightResponse` 반환(기존 `GET /report/highlight` 응답과 동일 스키마)
**에러**: `403 CARE_ACCESS_DENIED`, `400 INVALID_INPUT`

---

# Part B. 기존 엔드포인트 수정

## B1. 보호자 회원가입 – 이메일/전화/이메일인증을 필수→선택으로 완화  🟡수정

> **결정(둘 다 남김)**: 이메일·전화·이메일 인증을 **제거하지 않고 옵셔널로 유지**한다. 디자인 회원가입은 이름/생년월일/아이디/비번/PIN만 받지만, 서버는 이메일/전화를 **선택 입력**으로 계속 받아 계정 복구·알림에 활용한다.

| 항목 | 값 |
| --- | --- |
| 화면 | 회원가입(1 정보입력 → 2 PIN 설정) — 이메일·전화 입력은 화면엔 없음(옵셔널) |
| Path | `POST /api/v1/auth/guardians/register` (경로 유지) |

**요청 — 변경 전(전부 필수)**
```json
{ "name","loginId","password","birthDate","phone","email","pin","emailVerificationId" }
```
**요청 — 변경 후(필수 축소 + 옵셔널 유지)**
```jsonc
{
  // 필수 (디자인이 수집하는 값)
  "name": "박승아",            // 1~50자
  "birthDate": "1985-06-10",   // YYYY-MM-DD
  "loginId": "jeongeun",       // 4~50자 (A1로 사전 중복확인)
  "password": "pw12345678",    // 8~50자
  "pin": "123456",             // 6자리 숫자 (2단계 PIN 설정)

  // 선택 (남겨둠 — 제공 시에만 처리)
  "phone": "01012345678",      // nullable
  "email": "user@ex.com",      // nullable — 제공 시 형식 검증
  "emailVerificationId": null   // email 인증을 거친 경우에만 전달
}
```
- **필수 → 선택 전환**: `phone`, `email`, `emailVerificationId` (모두 nullable). 디자인 흐름에선 미전달.
- **검증 규칙**: `email`이 있으면 형식 검증 + `emailVerificationId`가 있을 때만 인증 확인. 셋 다 없어도 가입 성공.
- **응답**: 기존과 동일 `{ "userId": "…" }` (201)

**연쇄(유지)**
- `POST /api/v1/auth/email-verifications`, `.../{id}/confirm` → **엔드포인트 유지**(이메일을 선택 입력할 때만 사용). 관련 `EMAIL_*`/`AUTH_VERIFICATION_*` 코드도 유지.

---

## B2. 어르신 자격증명 – 6자리 단일 크리덴셜로 통일  🟡수정

| 항목 | 값 |
| --- | --- |
| 화면 | 어르신 등록(성함/전화/성별/아이디/**비밀번호**), 어르신 로그인("**비밀번호 6자리**") |
| Path | `POST /api/v1/guardian/elders` (경로 유지) |

**문제**: 현재 `RegisterElderRequest`가 `password`(8자↑)와 `pin`(6자리)를 **둘 다** 요구하나, 디자인은 6자리 비밀번호 **하나**뿐(PIN 설정 단계 없음).

**결정(둘 다 남김)**: `password`·`pin` **두 컬럼 모두 유지**하되, 디자인이 주는 6자리를 **`pin`(필수)** 에 매핑하고 **`password`는 선택(nullable)** 으로 완화한다. 어르신 로그인은 `pin`으로 처리.

**요청 — 변경 후**
```jsonc
{
  "familyId": "…",          // 필수 (컨텍스트)
  "name": "김순자",          // 1~30자
  "phone": "01012345678",   // 최대 20자
  "gender": "F",            // 남/여
  "loginId": "sunja",       // 4~50자 (A1 중복확인)
  "pin": "123456",          // 필수·6자리 = 어르신 로그인 비밀번호(디자인 "비밀번호 6자리")
  "password": null           // 선택(유지) — 미전달 가능. 향후 웹/보조 로그인용
  // birthDate: 폼에 없음 → 계속 옵셔널(nullable) 유지
}
```
- **로그인**: `POST /auth/login` 은 이미 `pin` 지원 → 어르신은 `loginId + pin + deviceId`로 로그인. `password`도 병행 지원(둘 다 남김).
- 핵심: "어르신 로그인 = 6자리 `pin`", `password`는 옵셔널로 존치.

---

## B3. 추억 목록 – "전체"(전 어르신) 탭 지원  🟡수정

| 항목 | 값 |
| --- | --- |
| 화면 | 추억 앨범 상단 필터 **전체 / 김순자 / 박영호** |
| Path | `GET /api/v1/guardian/memories` |

**변경**: `elderId` 를 **필수 → 선택(optional)** 으로. 미지정 시 보호자가 접근 가능한 **전 어르신의 추억 통합** 반환.

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `elderId` | UUID | ❌(변경) | 지정 시 해당 어르신, 생략 시 전체 |

- 응답 스키마(`MemorySummaryResponse[]`)는 유지. 전체 조회 시 각 항목이 어느 어르신 것인지 구분이 필요하면 `elderId`, `elderName` 필드 **추가 권장**.
- **에러**: 특정 elderId 지정 시 `403 CARE_ACCESS_DENIED`

---

## B4. 어르신 답변 조회 – 작성시각·전사 추가  🟡수정

| 항목 | 값 |
| --- | --- |
| 화면 | 추억 상세 "주고 받은 이야기" — `작성자·2일전 오후 3:20` + `감정 태그` + `텍스트/전사` + `음성 재생` |
| Path | `GET /api/v1/guardian/memories/{memoryId}/responses` |

**응답 아이템 — 변경 전** `ResponseItem{ id, responseType, emotions[], text, mediaKey }`

**변경 후(필드 추가)**
```jsonc
{
  "id": "…",
  "responseType": "VOICE",      // EMOTION | TEXT | IMAGE | VOICE
  "emotions": ["LONGING","HAPPY"],
  "text": "그 냇가 참 좋았지... 친구들이랑 물고기 잡던 게 아직도 생생해.", // 음성 전사(STT)면 여기
  "mediaKey": "…",
  "mediaUrl": "https://…",       // 재생용 서빙 URL (권장)
  "durationSeconds": 2,           // 음성일 때
  "transcriptionStatus": "COMPLETED", // PENDING | COMPLETED | FAILED (비음성은 NOT_APPLICABLE)
  "createdAt": "2026-09-06T06:20:00Z"  // 추가: 디자인의 "2일전·오후 3:20"
}
```
- **추가 필드**: `createdAt`(필수), 음성 재생용 `mediaUrl`·`durationSeconds`, 음성 전사 표기용 `text`(전사), 전사 비동기 상태 `transcriptionStatus` — **모두 남김**. `PENDING`·`FAILED`에서는 `text`가 null일 수 있다.
- **그룹핑(둘 다 남김)**: 응답은 현행대로 **타입별 레코드로 저장**하고, 프론트가 `createdAt`·작성자로 **병합 표시**하는 것을 기본 전제로 한다(저장 구조 변경 없이 필드 추가만). 감정+음성 결합 저장이 필요해지면 이후 확장.

---

## B5. 보호자 홈 – 어르신 "오늘 컨디션" 필드 추가  🟡수정

| 항목 | 값 |
| --- | --- |
| 화면 | 보호자 홈 "오늘 컨디션 좋아요 · **양호**" 링(어르신별) |
| Path | `GET /api/v1/guardian/home` |

**변경**: `GuardianHomeResponse.ElderCardResponse` 에 컨디션 상태 추가.
```jsonc
{
  "elderId":"…","name":"박영호","age":85,"role":"…","roleLabel":"딸",
  "daysTogether":96,"attendedToday":true,"greetingSentToday":false,
  "lastLoginAt":"2026-09-08T00:41:00Z",
  "weeklyActivities":[…],
  "condition": "GOOD"   // 추가: GOOD(양호) | CAUTION(주의) | OBSERVE(관찰)
}
```
- `condition` 산출 기준은 리포트 인지영역 상태와 정합되게 정의(예: 최저 영역 등급).

---

## B6. 추억 장소·연월 지원  🟡수정

> **결정(둘 다 남김)**: 목록 카드의 "**구지면 · 1980.04.**"를 위해 장소·월을 **추가한다**. 기존 `memoryYear`(연도)는 그대로 두고 `place`·`memoryMonth`를 **선택 필드로 병행** 존치.

- `RegisterMemoryRequest` / `UpdateMemoryRequest` 에 추가:
  - `place`: string (선택, 최대 50자)
  - `memoryYear`: int (기존 유지) + `memoryMonth`: int 1~12 (선택, 추가)
- `MemorySummaryResponse` / `MemoryDetailResponse` 응답에 `place`, `memoryMonth` 추가

---

# Part C. 미사용 서버 기능 – 전부 유지(보류)

> **결정(둘 다 남김)**: 디자인에 대응 화면이 없어도 **제거하지 않고 전부 존치**한다. 향후 화면이 붙을 수 있으므로 API·enum·엔드포인트를 그대로 유지하고 노출만 하지 않는다.

| 서버 기능 | 처리 |
| --- | --- |
| 이메일 인증 2종 (`/auth/email-verifications`, `/confirm`) | **유지** (B1의 선택 이메일 입력 시 사용) |
| AI 회상 (`GET /elder/reminiscence/today`) | **유지** (향후 화면 대비) |
| 인지훈련(CIST) 문항 화면 | 서버 API **유지**. 문항 UI가 디자인에 없으므로 화면 디자인만 추후 보강 |
| 하루 한마디 보낸 이력 (`GET …/daily-care/sent`) | **유지** (향후 이력 화면 대비) |
| 어르신 텍스트 답변 (`POST …/responses/text`) | **유지** (비노출, 향후 재사용 대비) |
| 감정 `ANGRY` | **유지** (enum 존치, 미노출) |

---

# 구현 우선순위(제안)

1. **A1 중복확인** — 회원가입/등록 3화면 공통, 의존성 없음 → 최우선
2. **B1 회원가입 이메일 제거** + **B2 어르신 6자리 크리덴셜** — 온보딩 정합성(파급 큼)
3. **B5 홈 컨디션** + **A2 오늘의 기록** — 보호자 홈 완성
4. **B3 전체 탭** + **B4 답변 시각/전사** — 추억 화면 완성
5. **A3 하이라이트 편집 / B6 장소·연월** — 디자인 의도 확정 후
