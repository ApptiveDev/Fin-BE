# 배포

| 브랜치 | 환경 | 서버 | Spring 프로필 |
| --- | --- | --- | --- |
| `main` | dev | GCP `login-test-488917` / `fin-be-tests` (`test-fin.duckdns.org`) | `dev,server` (Swagger 켜짐) |
| `release` | prod | 미정 | `server` |

`main`이나 `release`에 push되면 `.github/workflows/deploy.yml`이 돈다.

1. `ci`: `backend-ci.yml`(test, integrationTest)을 호출한다. 실패하면 여기서 멈춘다.
2. `build`: `bootJar` → `ghcr.io/apptivedev/fin-be:<커밋 SHA>` 이미지 푸시.
3. `deploy`: GCP에 OIDC로 인증하고, IAP 터널 SSH로 서버 `~/fin-be/`에 `deploy/` 파일과 `.env`를 복사한 뒤 `deploy/remote-deploy.sh`를 실행한다. nginx를 거쳐 `GET /api/categories`가 응답해야 성공이다.

`docs/**`, `*.md`만 바뀐 push는 배포하지 않는다. 배포 파일을 바꾸는 PR에서는 이미지 빌드까지만 돈다. 수동 실행(`workflow_dispatch`)도 `main`·`release`에서만 배포한다.

## 서버 구성

`deploy/docker-compose.yml` (compose 프로젝트 이름 `fin-be` 고정)

- `nginx`: 80 → 443 리다이렉트, 443에서 backend로 프록시. `deploy/nginx/templates/default.conf.template`의 `${SERVER_NAME}`을 기동 시 치환한다. 인증서는 서버의 `/etc/letsencrypt/live/<SERVER_NAME>/`.
- `backend`: 위 이미지. Flyway가 기동 때 마이그레이션을 적용한다. 배포 때 DB를 초기화하지 않는다.
- `postgres`: 볼륨 `fin-be_postgres_data`. 포트는 서버 로컬(`127.0.0.1:5678`)에만 열고, 수집기(Fin-API)는 IAP SSH 터널로 접근한다.

루트 `docker-compose.yml`은 로컬 개발 DB용이라 배포와 무관하다.

## GitHub Environment

Fin-BE 레포의 Environment `dev`, `prod`에 둔다.

| 종류 | 이름 | 내용 |
| --- | --- | --- |
| var | `GCP_PROJECT` | GCP 프로젝트 ID |
| var | `GCP_WIF_PROVIDER` | `projects/<번호>/locations/global/workloadIdentityPools/github/providers/github` |
| var | `GCP_SERVICE_ACCOUNT` | `github-deployer@<프로젝트>.iam.gserviceaccount.com` |
| var | `GCE_INSTANCE`, `GCE_ZONE` | VM 이름, 존 |
| var | `SERVER_NAME` | 도메인. nginx `server_name`과 인증서 경로에 쓴다 |
| var | `FRONTEND_URL` | 프론트 주소. CORS와 OAuth 로그인 후 리다이렉트에 쓴다 |
| secret | `POSTGRES_PASSWORD` | DB 비밀번호 |
| secret | `JWT_SECRET`, `GOOGLE_CLIENT_ID/SECRET`, `KAKAO_CLIENT_ID/SECRET` | 앱 시크릿 |

OAuth 콘솔(Google, Kakao)에는 `https://<SERVER_NAME>/login/oauth2/code/{google,kakao}`가 리다이렉트 URI로 등록돼 있어야 한다.

## GCP 1회 설정

GitHub Actions는 서비스 계정 키 없이 Workload Identity Federation(OIDC)으로 인증하고, VM에는 OS Login + IAP 터널로 SSH 한다. 공개 SSH 포트를 열 필요가 없다.

```bash
P=login-test-488917; PN=$(gcloud projects describe $P --format='value(projectNumber)')
SA=github-deployer@$P.iam.gserviceaccount.com
gcloud services enable iam.googleapis.com iamcredentials.googleapis.com sts.googleapis.com iap.googleapis.com --project $P

# GitHub OIDC 신뢰. ApptiveDev 조직 레포만 받는다.
gcloud iam workload-identity-pools create github --location=global --project $P
gcloud iam workload-identity-pools providers create-oidc github --location=global --workload-identity-pool=github --project $P \
  --issuer-uri=https://token.actions.githubusercontent.com \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
  --attribute-condition="assertion.repository_owner=='ApptiveDev'"

# 배포용 서비스 계정. IAP 터널·VM 조회는 프로젝트, OS Login(sudo)은 배포 대상 VM에만 준다.
gcloud iam service-accounts create github-deployer --project $P
for r in roles/iap.tunnelResourceAccessor roles/compute.viewer; do
  gcloud projects add-iam-policy-binding $P --member=serviceAccount:$SA --role=$r --condition=None; done
gcloud compute instances add-iam-policy-binding <VM> --zone=<ZONE> --project $P \
  --member=serviceAccount:$SA --role=roles/compute.osAdminLogin
gcloud iam service-accounts add-iam-policy-binding $PN-compute@developer.gserviceaccount.com --project $P \
  --member=serviceAccount:$SA --role=roles/iam.serviceAccountUser

# Fin-BE(배포)와 Fin-API(수집)만 이 계정을 쓸 수 있다.
for repo in Fin-BE Fin-API; do
  gcloud iam service-accounts add-iam-policy-binding $SA --project $P --role=roles/iam.workloadIdentityUser \
    --member="principalSet://iam.googleapis.com/projects/$PN/locations/global/workloadIdentityPools/github/attribute.repository/ApptiveDev/$repo"; done

# SSH는 IAP 대역에서, iap-ssh 태그가 붙은 VM으로만
gcloud compute instances add-tags <VM> --zone=<ZONE> --project $P --tags=iap-ssh
gcloud compute firewall-rules create allow-ssh-from-iap --project $P --network=default \
  --direction=INGRESS --allow=tcp:22 --source-ranges=35.235.240.0/20 --target-tags=iap-ssh
# 도메인이 가리키는 IP가 VM 재시작으로 바뀌지 않게 현재 IP를 고정 IP로 전환
gcloud compute addresses create <이름> --addresses=<현재 외부 IP> --region=<REGION> --project $P
gcloud compute instances add-metadata <VM> --zone=<ZONE> --project $P --metadata=enable-oslogin=TRUE
```

GCP에는 AWS SSM Run Command처럼 VM이 바깥으로 연결해 명령을 받는 방식이 없어서, IAP 프록시 대역에서 22번으로 들어오는 규칙 하나는 필요하다. 공인 IP의 22번은 그 외 대역에 닫혀 있고, IAM(IAP)과 OS Login을 둘 다 통과해야 접속된다. 워크플로가 등록하는 SSH 키는 1시간 뒤 만료된다(`--ssh-key-expire-after=1h`).

OS Login을 켜면 메타데이터 SSH 키로는 접속할 수 없다. 사람도 `gcloud compute ssh <VM> --zone <ZONE> --tunnel-through-iap`로 접속한다. 배포는 서비스 계정의 OS Login 사용자 홈(`~/fin-be/`)에서 돈다.

## 새 서버 준비 (prod)

1. VM 생성(Docker, compose 플러그인 설치), 방화벽 80/443, 위 OS Login·IAP 설정.
2. 도메인 연결 후 certbot으로 `/etc/letsencrypt/live/<도메인>/` 인증서 발급(80 포트를 쓰므로 nginx 컨테이너가 뜨기 전에). 갱신 시에는 `docker restart fin-nginx`.
3. Fin-BE Environment `prod`에 위 값 등록. `release` 브랜치를 만들면 첫 배포가 나간다.
4. 수집기도 돌리려면 Fin-API의 `prod` Environment와 `PROD_ENABLED` 변수를 설정한다.

## 롤백

문제 커밋을 `main`(또는 `release`)에서 revert하면 일반 배포로 이전 코드가 나간다. 급할 때는 서버 `~/fin-be/.env`의 `IMAGE_TAG`를 이전 SHA로 바꾸고 `sudo docker compose up -d && sudo docker compose restart nginx`. 이미지는 GHCR에 SHA별로 남아 있다. 이미 적용된 Flyway 마이그레이션은 되돌리지 않으므로, 스키마를 바꾼 배포는 새 마이그레이션으로 고친다.

## 문제 확인

- Actions 로그의 `Deploy` 단계 끝에 헬스체크 실패 시 backend 로그 200줄이 찍힌다.
- 서버에서: `cd ~/fin-be && sudo docker compose ps`, `sudo docker compose logs -f backend` (배포 계정 홈이므로 `sudo -i` 후 해당 사용자 홈으로 이동해야 할 수 있다).
