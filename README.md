# FIN
청년들의 금융(Finance) 고민을 끝(Finish)내다.

---

## 1. 기술스택
- Backend : Spring Boot
- Database : PostgreSQL


## 2. API 명세
### /auth
| 기능      | Method | API Path   |
| ------- | ------ | ---------- |
| 로그아웃    | POST   | `/logout`  |
| 토큰 리프레시 | POST   | `/refresh` |

### /oauth2
| 기능         | Method | API Path                |
| ---------- | ------ | ----------------------- |
| 구글 계정 로그인  | GET    | `/authorization/google` |
| 카카오 계정 로그인 | GET    | `/authorization/kakao`  |


### /user
| 기능       | Method | API Path |
| -------- | ------ | -------- |
| 유저 정보 조회 | GET    | `/me`    |
| 유저 정보 수정 | PATCH  | `/me`    |

### /term
| 기능       | Method | API Path |
| -------- | ------ | -------- |
| 약관 목록 조회 | GET    | `/`      |
| 약관 동의    | POST   | `/agree` |


## 3. 개발현황
Update : 2026/03/14
| 도메인       | 진행상황 | 비고 |
| -------- | ------ | -------- |
| Auth | 완료    | OAuth2 기반 SNS 로그인, JWT(Access Token + Refresh Token)     |
| User   | 완료   | 유저 정보 조회 및 수정 |
| Term   | 완료   | 약관 조회 및 약관 동의 기록 |
