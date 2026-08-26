# 클라이언트 ↔ 백엔드 담당 매핑

> 기준일: 2026-08-25 / 전제: 현재 열린 PR·이슈가 모두 머지·해결된 상태
> 근거: [v2-work-assignment.md](./v2-work-assignment.md) §1.3 파일 소유권, §3.3 컨트롤러 소유

## 요약

| 클라이언트 | 담당 화면 | 백엔드 카운터파트 |
| --- | --- | --- |
| **박승아** | 홈, 리포트, 인지 활동 | **김연호** (주) · 황정빈 (홈 조합만) |
| **문채원** | 추억 앨범, 인지활동 제외 어르신, 하루 한마디 | **황정빈** |
| **전수안** | 마이페이지, 보호자 회원가입 | **황정빈** |

---

## 박승아 — 홈 / 리포트 / 인지 활동

| 화면 | 백엔드 모듈 | 담당 |
| --- | --- | --- |
| 보호자 홈 (어르신 정보·도전과제) | `guardian/presentation/HomeController` | 황정빈 |
| ㄴ 홈의 출석·스트릭·함께한 일 수 | `elder/attendance`, `AttendanceQuery` | 김연호 |
| 리포트 (RPT 6기능) | `guardian/report`, `ReportController` | 김연호 |
| 인지 활동 (CIST 6기능) | `elder/training`, `platform/content`, `TrainingSessionController` | 김연호 |

> 홈만 두 사람 경계에 걸침. 화면 조합 API는 황정빈, 출석 수치는 김연호.

## 문채원 — 추억 앨범 / 어르신(인지활동 제외) / 하루 한마디

| 화면 | 백엔드 모듈 | 담당 |
| --- | --- | --- |
| 추억 앨범 (보호자 ALB) | `guardian/memory`, `platform/media` | 황정빈 |
| 추억 앨범 (어르신 E-ALB) 조회·답변 | `elder/memory`, `elder/response` | 황정빈 |
| 어르신 홈 | `elder/home` | 황정빈 |
| 하루 한마디 전송·이력 | `guardian/dailycare` | 황정빈 |
| 하루 한마디 수신함 | `elder/inbox` | 황정빈 |

> 어르신 홈에 출석·뱃지가 들어갈 경우 그 데이터만 김연호(`AttendanceQuery`).
> 말동무(FRI)는 명세·모듈 모두 미작성.

## 전수안 — 마이페이지 / 보호자 회원가입

| 화면 | 백엔드 모듈 | 담당 |
| --- | --- | --- |
| 프로필 조회·수정 | `guardian/presentation/ProfileController` | 황정빈 |
| 가족 생성·합류(초대 코드), 어르신 계정 생성 | `guardian/family`, `guardian/eldermanagement` | 황정빈 (Phase 0 확정 후 변경 담당) |
| 보호자 회원가입·로그인·인증코드 | `auth/**` | 황정빈 (주도) · 김연호 (협업) |
