# FIN
청년들의 금융(Finance) 고민을 끝(Finish)내다.

## 기술스택
- Backend : Spring Boot
- Database : PostgreSQL

## API 명세
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

### /template
| 기능         | Method | API Path                |
| ---------- | ------ | ----------------------- |
| 로그아웃  | POST    | `/logout` |

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

