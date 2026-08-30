# 해미 API 명세서

> 확정 디자인(33화면) 반영 완료본. 서버에만 존재하는 API도 그대로 포함한다.
> 최종 수정일: 2026-08-27

## 공통 규약

- **Base URL**: `/api/v1`
- **응답 봉투**: `{ "data": <T|null>, "error": <null | { "code","message","field" }> }` (null 필드는 직렬화 제외)
- **인증**: `Authorization: Bearer <accessToken>`. 보호자=`guardianId`, 어르신=`elderUserId`가 토큰에서 주입됨
- **케어 인가**: 보호자-어르신 링크가 없으면 `403 CARE_ACCESS_DENIED` (존재하지 않는 elderId 포함)
- **날짜/시각**: 날짜 `YYYY-MM-DD`, 시각 ISO-8601 `Instant`(UTC), 기준 타임존 KST(Asia/Seoul)
- **공통 에러코드**: `UNAUTHENTICATED(401)`, `ROLE_NOT_ALLOWED(403)`, `CARE_ACCESS_DENIED(403)`, `NOT_RESOURCE_OWNER(403)`, `RESOURCE_NOT_FOUND(404)`, `INVALID_INPUT(400)`

### 열거형

| enum | 값 |
| --- | --- |
| `GuardianRole` | GUARDIAN(보호자), DAUGHTER(딸), SON(아들), GRANDDAUGHTER(손녀), GRANDSON(손자), OTHER(기타) |
| `Emotion` | LOVE(사랑), LONGING(그리움), MISS(보고싶음), HAPPY(행복), JOY(즐거움), SAD(슬픔), ANGRY |
| `ResponseType` | EMOTION, TEXT, IMAGE, VOICE |
| `ReportStatus` | GOOD(양호), NORMAL(주의), WATCH(관찰) |
| `CognitiveArea` | ORIENTATION(상황 파악), RECALL(기억 회상), LANGUAGE(언어), DELAYED_RECALL(지연 회상) |
| `CognitiveStatus` | GOOD, NORMAL, WATCH, NOT_AVAILABLE |
| `GuardianCondition` | GOOD(양호), CAUTION(주의), OBSERVE(관찰) |
| `SupportGuideAction` | SEND_DAILY_CARE, REGISTER_MEMORY, CALL_ELDER, PRAISE_ELDER |
| `CareType` | TEXT, VOICE |
| `SessionStatus` | IN_PROGRESS, COMPLETED |
| `QuestionType` | ORIENTATION, RECALL, LANGUAGE, DELAYED_RECALL |
| `AnswerMode` | CHOICE, TEXT_OR_VOICE |
| `MediaType` | 보호자: MEMORY_IMAGE, GREETING_VOICE, PROFILE_IMAGE / 어르신: RESPONSE_IMAGE, RESPONSE_VOICE |

---

# 1. 인증 `/auth`

## 1.1 아이디 중복 확인
`GET /auth/login-id/availability` · 인증 불필요

| Query | 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| loginId | string | ✅ | 4~50자 |

**200** `{ "loginId": "jeongeun", "available": true }`
**에러** `400 INVALID_INPUT`

## 1.2 보호자 회원가입
`POST /auth/guardians/register` · 인증 불필요 · **201**
```jsonc
{
  "name": "박승아",            // 필수 1~50
  "birthDate": "1985-06-10",   // 필수
  "loginId": "jeongeun",       // 필수 4~50
  "password": "pw12345678",    // 필수 8~50
  "pin": "123456",             // 필수 6자리 숫자
  "phone": "01012345678",      // 선택 ≤20
  "email": "user@ex.com",      // 선택, 이메일 형식
  "emailVerificationId": "uuid" // 선택. 제공 시 email도 필수이며 인증 완료된 1회용 ID여야 함
}
```
**응답** `{ "userId": "uuid" }`
디자인 화면에는 이메일·전화번호 입력이 없으므로 둘 다 선택 값으로 유지한다. `emailVerificationId`가 없는 가입은 이메일 인증을 소비하지 않는다.

**에러** `409 LOGIN_ID_ALREADY_TAKEN`, `409 EMAIL_ALREADY_TAKEN`, `400 INVALID_INPUT`, `400 EMAIL_VERIFICATION_REQUIRED`

## 1.3 이메일 인증번호 발송
`POST /auth/email-verifications` · **201** · body `{ "email": "user@ex.com" }` → `data: <verificationId>`
**에러** `429 AUTH_VERIFICATION_RESEND_LIMITED`, `503 EMAIL_DELIVERY_FAILED`

## 1.4 이메일 인증번호 확인
`POST /auth/email-verifications/{verificationId}/confirm` · body `{ "code": "123456" }` · **200**
**에러** `400 INVALID_INPUT`, `429 AUTH_VERIFICATION_LOCKED`

## 1.5 로그인
`POST /auth/login` · 인증 불필요 · **200**
```jsonc
{ "loginId": "jeongeun", "password": "pw12345678", "pin": "123456", "deviceId": "device-abc" }
```
- `password` 또는 `pin` 중 **하나 이상 필수**.
- 보호자는 **최초 로그인에서 `password`가 필수**이며, 비밀번호 로그인에 성공하면 PIN 로그인이 활성화된다. 그 이후에는 `password` 또는 `pin`으로 로그인할 수 있다.
- 어르신은 계정 생성 시부터 PIN 로그인이 활성화되어 `loginId + pin`으로 로그인할 수 있다. 선택 `password`가 등록된 경우 비밀번호 로그인도 가능하다.
**응답** `{ "accessToken": "...", "refreshToken": "..." }`
**에러** `401 INVALID_CREDENTIALS`, `423 AUTH_ACCOUNT_LOCKED`, `400 INVALID_INPUT`

## 1.6 토큰 재발급
`POST /auth/refresh` · body `{ "refreshToken", "deviceId" }` · **200** `{ accessToken, refreshToken }` (refresh 회전)
**에러** `401 AUTH_REFRESH_TOKEN_INVALID`

## 1.7 로그아웃
`POST /auth/logout` · 인증 필요 · body `{ "deviceId" }` · **200** `null`

---

# 2. 보호자 – 프로필 / 가족 / 어르신

## 2.1 프로필 조회
`GET /guardian/profile` · **200**
```jsonc
{
  "userId","name","loginId","phone","birthDate","profileImageUrl",
  "elders": [ { "elderId","name","birthDate","role","roleLabel" } ]
}
```

## 2.2 프로필 수정
`PATCH /guardian/profile` · **200** `null`
```jsonc
{ "loginId": "jeongeun", "profileImageMediaRefId": "uuid|null", "elderRoles": { "<elderId>": "DAUGHTER" } }
```
편집 대상: 아이디·프로필사진·어르신별 역할 (이름/생년월일은 표시 전용)
**에러** `409 LOGIN_ID_ALREADY_TAKEN`, `403 NOT_RESOURCE_OWNER`, `400 INVALID_INPUT`

## 2.3 가족 생성
`POST /guardian/families` · **201**
```jsonc
{ "name": "행복한 우리집", "memo": "…(≤30)", "profileImageMediaRefId": "uuid|null" }
```
**응답** `{ "familyId", "inviteCode" }` (생성과 동시 초대코드 발급)
**에러** `409 FAMILY_CAPACITY_EXCEEDED`

## 2.4 초대코드로 합류
`POST /guardian/families/join` · body `{ "inviteCode": "k4855520" }` · **204**
**에러** `404 RESOURCE_NOT_FOUND`, `409 FAMILY_CAPACITY_EXCEEDED`

## 2.5 내 가족 조회
`GET /guardian/families/my?elderId={optional}` · **200** (미소속 시 `data: null`)
```jsonc
{
  "familyId","name","memo","profileImageUrl","inviteCode",
  "guardians": [ { "userId","name","role","roleLabel","isMe" } ],
  "elders":    [ { "elderId","name","birthDate","myRole","myRoleLabel" } ]
}
```
`elderId` 지정 시 그 어르신 기준으로 guardians[].role 계산. 어르신 1명이면 자동 기준.

## 2.6 어르신 등록
`POST /guardian/elders` · **201** `{ "elderId" }`
```jsonc
{
  "familyId": "uuid",       // 필수
  "name": "김순자",          // 필수 1~30
  "phone": "01012345678",   // 필수 ≤20
  "gender": "F",            // 필수 문자열 ≤20
  "loginId": "sunja",       // 필수 4~50
  "pin": "123456",          // 필수 6자리 숫자. 어르신 로그인 크리덴셜
  "password": "pw12345678", // 선택 8~50. PIN과 별도로 저장하며 PIN 로그인을 기본으로 사용
  "birthDate": null          // 선택 (nullable)
}
```
**응답** `"uuid"`
**에러** `409 FAMILY_CAPACITY_EXCEEDED`, `409 LOGIN_ID_ALREADY_TAKEN`, `400 INVALID_INPUT`

## 2.7 링크 해제(본인 이탈)
`DELETE /guardian/elders/{elderId}/link` · **204**
**에러** `403 NOT_RESOURCE_OWNER`, `409 LAST_GUARDIAN_CANNOT_LEAVE`

## 2.8 어르신별 본인 역할 변경
`PATCH /guardian/elders/{elderId}/link/role` · body `{ "role": "DAUGHTER" }` · **204**
**에러** `403 NOT_RESOURCE_OWNER`

---

# 3. 보호자 – 홈

## 3.1 보호자 홈 조회
`GET /guardian/home` · **200**
```jsonc
{
  "elders": [
    {
      "elderId","name","age","role","roleLabel",
      "daysTogether": 96,
      "attendedToday": true,
      "greetingSentToday": false,
      "lastLoginAt": "2026-09-08T00:41:00Z",
      "condition": "GOOD",              // GOOD | CAUTION | OBSERVE | null(판정 데이터 없음)
      "weeklyActivities": [
        { "date","dayOfWeek","training","greetingRead","memoryViewed","replied" }
      ]
    }
  ],
  "challenge": { "greetingCompleted": false, "memoryCompleted": true }
}
```
주간 스택바=`weeklyActivities`(답변/추억열람/한마디읽음/인지훈련), 오늘의 할일=`challenge`.

## 3.2 오늘의 기록(활동 타임라인)
`GET /guardian/elders/{elderId}/activities?date={optional}` · **200**
```jsonc
{
  "date": "2026-08-27",
  "items": [
    { "occurredAt":"...T00:20:00Z", "type":"TRAINING_COMPLETED", "title":"인지 활동 완료",
      "detail": { "activityName":"인지 훈련", "durationMinutes":5, "accuracy":80 } },
    { "occurredAt":"...T02:05:00Z", "type":"RESPONSE_SENT", "title":"추억 답변 완료",
      "detail": { "memoryId":"uuid", "responseType":"VOICE" } }
  ]
}
```
`date`는 생략 또는 `today`면 KST 오늘, 그 외 `YYYY-MM-DD`이다. `items`는 `occurredAt` 오름차순이다.

| type | detail |
| --- | --- |
| `TRAINING_COMPLETED` | `activityName`, `durationMinutes`, `accuracy` |
| `GREETING_ARRIVED` | `medium`(TEXT/VOICE), `preview?`, `durationSeconds?` |
| `GREETING_READ` | `{}` |
| `MEMORY_VIEWED` | `memoryId`, `memoryTitle?` |
| `RESPONSE_SENT` | `memoryId`, `responseType` |

**에러** `403 CARE_ACCESS_DENIED`, `400 INVALID_INPUT`(잘못된 date)

---

# 4. 보호자 – 추억 앨범 `/guardian/memories`

## 4.1 추억 등록
`POST /guardian/memories` · **201** `{ "memoryId" }`
```jsonc
{
  "elderId": "uuid",              // 필수
  "title": "어린 시절 고향",        // 필수 ≤100
  "message": "이 사진, 기억나세요?", // 필수 ≤100 (어르신께 여쭤볼 한마디)
  "memo": "가족끼리 나들이…",       // 선택 ≤300 (보호자 메모)
  "memoryYear": 1975,             // 선택
  "memoryMonth": 4,               // 선택 1~12
  "place": "구지면",              // 선택 ≤50
  "mediaRefIds": ["uuid", …]       // 선택 최대 4장
}
```
**에러** `400 INVALID_INPUT`, `403 CARE_ACCESS_DENIED`

## 4.2 추억 목록 (전체 / 어르신별)
`GET /guardian/memories?elderId={optional}` · **200**
`elderId` 생략 시 접근 가능한 전 어르신 통합("전체" 탭).
```jsonc
[ { "id","elderId","title","thumbnailKey","responded","place","memoryYear","memoryMonth",
    "creatorName","creatorRole","creatorRoleLabel","isMine" } ]
```
**에러**(특정 elderId) `403 CARE_ACCESS_DENIED`

## 4.3 추억 상세
`GET /guardian/memories/{memoryId}` · **200**
```jsonc
{ "id","elderId","title","memo","message","memoryYear","memoryMonth","place",
  "imageKeys":[…],"responded","createdAt","creatorName","creatorRole","creatorRoleLabel","isMine" }
```
**에러** `403`, `404`

## 4.4 어르신 답변 조회
`GET /guardian/memories/{memoryId}/responses` · **200**
```jsonc
[ {
  "id","responseType":"VOICE","emotions":["LONGING","HAPPY"],
  "text":"그 냇가 참 좋았지…", "mediaKey","mediaUrl","durationSeconds",
  "transcriptionStatus":"COMPLETED",
  "createdAt":"2026-09-06T06:20:00Z"
} ]
```
음성 응답의 `text`는 Gemini STT 전사 결과다. 업로드 확정 뒤 비동기로 처리하므로 `transcriptionStatus`가 `PENDING`이면 `text=null`이며, `COMPLETED`일 때만 전사를 표시한다. 공급자 미설정·호출 실패·원본 읽기 실패는 음성 답변 자체를 실패시키지 않고 `FAILED`로 남긴다. 비음성 응답은 `NOT_APPLICABLE`이다. `mediaUrl`은 재생용 서빙 URL, `durationSeconds`는 음성에서만 제공된다. 응답은 타입별 레코드이고 감정+음성 한 카드는 프론트가 `createdAt`·작성자로 병합한다.

## 4.5 추억 수정 (생성자 본인)
`PUT /guardian/memories/{memoryId}` · **204** · body: 4.1과 동일(elderId 제외)
**에러** `403 NOT_RESOURCE_OWNER`, `404`

## 4.6 추억 삭제 (생성자 본인)
`DELETE /guardian/memories/{memoryId}` · **204**
**에러** `403 NOT_RESOURCE_OWNER`, `404`

---

# 5. 보호자 – 하루 한마디 `/guardian/elders/{elderId}/daily-care`

## 5.1 텍스트 전송
`POST …/daily-care/text` · body `{ "text": "…(≤100)" }` · **201** `{ "dailyCareId" }`
**에러** `403 CARE_ACCESS_DENIED`, `409 DAILY_CARE_ALREADY_SENT`

## 5.2 음성 전송
`POST …/daily-care/voice` · body `{ "mediaRefId": "uuid", "durationSeconds": 24 }` · **201** `{ "dailyCareId" }`

## 5.3 보낸 이력
`GET …/daily-care/sent` · **200**
```jsonc
[ { "id","careDate","type","text","mediaKey","durationSeconds","read" } ]
```

---

# 6. 보호자 – 리포트

## 6.1 어르신 리포트 목록
`GET /guardian/report/elders` · **200** (관찰필요→보통→좋음 정렬)
```jsonc
[ { "elderId","name","role","roleLabel","age","attendedToday","status":"WATCH" } ]
```

## 6.2 요약 카드
`GET /guardian/elders/{elderId}/report/summary` · **200**
```jsonc
{ "elderId","name","age","generation","daysTogether","attendedToday",
  "weeklyParticipationDays","weeklyGoalDays","status","currentStreak","bestStreak" }
```
**에러** `403 CARE_ACCESS_DENIED`

## 6.3 출석·참여
`GET /guardian/elders/{elderId}/report/attendance` · **200**
```jsonc
{
  "last7Days": [ { "date","dayOfWeek","participated","training","greetingRead","memoryViewed","replied" } ],
  "last4Weeks":[ { "weekStart","weekEnd","participatedDays" } ],
  "currentStreak","bestStreak","weeklyStatus"
}
```

## 6.4 인지 영역별 상태
`GET /guardian/elders/{elderId}/report/cognitive-status` · **200**
```jsonc
{ "elderId", "areas": [ { "area":"ORIENTATION","status":"GOOD","fourWeekDecline":false } ] }
```
4영역(ORIENTATION/RECALL/LANGUAGE/DELAYED_RECALL), 상태 3색 + NOT_AVAILABLE.

## 6.5 이번 주 하이라이트 조회
`GET /guardian/elders/{elderId}/report/highlight` · **200**
```jsonc
{ "elderId": "uuid", "items": [ { "id":"uuid", "title":"이번 주 하이라이트", "body":"…" } ] }
```
자동 생성 문구의 `item.id`는 같은 어르신·같은 주·같은 카드 순서에서 안정적으로 유지된다.

## 6.6 이번 주 하이라이트 편집
`PATCH /guardian/elders/{elderId}/report/highlight` · **200**
```jsonc
// 요청. item.id를 생략하면 서버가 생성한다.
{ "items": [
  { "id":"uuid", "title":"추억 회상", "body":"추억 회상에 좋은 반응 …" },
  { "title":"함께 해보기", "body":"지연 회상을 함께 도와주세요." }
] }
// 응답: 6.5와 동일 스키마
```
**에러** `403 CARE_ACCESS_DENIED`, `400 INVALID_INPUT`

## 6.7 서포트 가이드
`GET /guardian/elders/{elderId}/report/support-guide` · **200**
```jsonc
{ "elderName", "suggestions": [ { "action":"SEND_DAILY_CARE","message":"…" } ] }
```

## 6.8 리포트 PDF
`GET /guardian/elders/{elderId}/report/pdf` · **200** `application/pdf` (바이너리)
**에러** `403 CARE_ACCESS_DENIED`, `500 REPORT_PDF_RENDER_FAILED`

---

# 7. 어르신 – 홈 / 수신함 / 추억 / 답변

## 7.1 어르신 홈
`GET /elder/home` · **200**
```jsonc
{
  "greeting": { "totalToday": 1, "unread": 1 },
  "recentMemories": [ { "id","title","firstImageKey","responded" } ],
  "training": { "completedToday": false, "streak": 3 }
}
```

## 7.2 수신함(오늘 받은 하루 한마디)
`GET /elder/inbox` · **200**
```jsonc
[ { "id","guardianId","type":"VOICE","text":null,"mediaKey","durationSeconds":24,"read":false } ]
```

## 7.3 하루 한마디 읽음
`POST /elder/inbox/{dailyCareId}/read` · **200** `null`

## 7.4 추억 목록
`GET /elder/memories` · **200**
```jsonc
[ { "id","title","message","memoryYear","imageKeys":[…],"responded","createdAt",
    "creatorName","creatorRole","creatorRoleLabel" } ]
```

## 7.5 추억 상세
`GET /elder/memories/{memoryId}` · **200**
```jsonc
{ "id","title","memo","message","memoryYear","imageKeys":[…],"responded","createdAt",
  "creatorName","creatorRole","creatorRoleLabel" }
```

## 7.6 추억 열람 처리
`POST /elder/memories/{memoryId}/viewed` · **200** `null` (최초 1회 MemoryViewed 발행)
**에러** `404`

## 7.7 답변 – 마음 전하기(감정)
`POST /elder/memories/{memoryId}/responses/emotion` · **201** `{ "responseId" }`
body `{ "emotions": ["LONGING","HAPPY"] }` (최소 1, **최대 2개**)

## 7.8 답변 – 텍스트
`POST …/responses/text` · body `{ "text": "…(≤100)" }` · **201** `{ "responseId" }`

## 7.9 답변 – 이미지(사진 고르기)
`POST …/responses/image` · body `{ "mediaRefId": "uuid" }` · **201** `{ "responseId" }`

## 7.10 답변 – 음성(말하기)
`POST …/responses/voice` · body `{ "mediaRefId": "uuid" }` · **201** `{ "responseId" }`

## 7.11 내 답변 목록
`GET /elder/memories/{memoryId}/responses` · **200**
```jsonc
[ { "id","responseType","emotions":["…"],"text","mediaKey","createdAt" } ]
```

---

# 8. 어르신 – 인지 훈련(CIST) `/elder/training/session`

## 8.1 세션 진입/이어하기
`POST /training/session/enter` · **200**
```jsonc
{
  "id","status":"IN_PROGRESS","currentStep":"ORIENTATION",
  "currentQuestionNumber":1,"totalQuestionCount":10,
  "startedAt","completedAt":null,"inactivityReminderSeconds":90,"feedback":null,
  "currentQuestion": { "id","questionNumber","questionType","answerMode","prompt","imageKey","options":[…],"hint" },
  "result": null
}
```
**에러** `409 TRAINING_SESSION_ALREADY_STARTED`, `409 TRAINING_MATERIAL_UNAVAILABLE`

## 8.2 현재 문항 응답 제출
`POST /training/session/current-question/complete` · **200** (다음 문항 또는 결과 포함, 8.1 스키마)
```jsonc
{ "sessionId":"uuid","questionId":"uuid","questionNumber":1,
  "selectedOption":"…","textAnswer":"…","voiceMediaRefId":"uuid|null" }
```

## 8.3 오늘 결과 조회
`GET /training/session/result` · **200**
```jsonc
{ "id","status":"COMPLETED",
  "result": { "sessionId","participationSeconds","delayedRecallSuccessCount","completedAt","unlockedBadges":[…] } }
```

---

# 9. 어르신 – AI 회상

`GET /elder/reminiscence/today` · **200** `{ "date","content","aiGenerated" }`
매일 08:00 배치 생성, 미생성 시 즉석 생성.

---

# 10. 미디어 `/media`

## 10.1 업로드 URL 발급
`POST /media/upload-request` · **201**
```jsonc
// 요청
{ "mediaType":"MEMORY_IMAGE","originalFilename":"a.jpg","contentType":"image/jpeg",
  "declaredSizeBytes":123456,"declaredDurationSeconds":null,"contentHash":"<sha256 hex 64>" }
// 응답
{ "mediaRefId","presignedUrl","expiresAt","duplicate":false,"servingUrl":null }
```
- mediaType 권한: 보호자(MEMORY_IMAGE/GREETING_VOICE/PROFILE_IMAGE), 어르신(RESPONSE_IMAGE/RESPONSE_VOICE)
- `duplicate=true`면 presignedUrl=null, servingUrl 재사용(확정 불필요)
**에러** `400 INVALID_INPUT`, `401 UNAUTHENTICATED`, `403 ROLE_NOT_ALLOWED`

## 10.2 업로드 확정
`POST /media/{mediaRefId}/confirm` · **200** `data: "<servingUrl>"`
**에러** `403 NOT_RESOURCE_OWNER`, `404 RESOURCE_NOT_FOUND`

---

# 부록. 내부 전용(비공개) 엔드포인트

| Path | 용도 |
| --- | --- |
| `PUT /api/v1/internal/storage/upload`, `GET /api/v1/internal/storage/serve` | 로컬 스토리지(개발용) |
| `POST /api/v1/internal/ai/reminiscence/run` | 회상 배치 수동 트리거(테스트) |
| `POST /api/v1/internal/report/dispatch` | 리포트 발송 수동 트리거(테스트) |
