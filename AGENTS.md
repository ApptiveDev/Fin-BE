# 작업 안내

## 문서

- 기획과 구현 문서: [docs/README.md](docs/README.md)
- API 명세: [Fin API 명세](https://yuyeol3.github.io/fin-api-docs/) (원문 마크다운은 [yuyeol3/fin-api-docs](https://github.com/yuyeol3/fin-api-docs))

## API 명세 갱신

API 명세는 이 저장소의 코드와 자동으로 맞춰지지 않는다. API 계약(경로, 요청과 응답 필드, 인증 수준, 오류 코드, 동작 규칙)을 바꾸는 PR은 본문에 영향을 받는 명세 문서를 적고, 머지 후 fin-api-docs의 해당 `docs/*.md`와 `README.md`의 기준 커밋을 갱신한다. 형식과 Swagger(`/v3/api-docs`) 대조 절차는 [fin-api-docs의 AGENTS.md](https://github.com/yuyeol3/fin-api-docs/blob/main/AGENTS.md)를 따른다.
