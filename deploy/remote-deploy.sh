#!/usr/bin/env bash
# deploy.yml이 서버에서 실행한다. 표준 입력으로 GHCR 읽기 토큰(GITHUB_TOKEN)을 받는다.
set -euo pipefail
cd "$(dirname "$0")"
chmod 600 .env

sudo docker login ghcr.io -u github-actions --password-stdin
sudo docker compose pull backend
sudo docker logout ghcr.io

sudo docker compose up -d
# backend가 새로 만들어지면 IP가 바뀐다. nginx는 기동 시 backend 주소를 잡으므로 다시 띄운다.
sudo docker compose restart nginx

# nginx를 거쳐 공개 API가 응답할 때까지 기다린다(e2-micro에서 기동이 느리다).
for _ in $(seq 1 40); do
  if sudo docker compose exec -T nginx \
      wget -q -O /dev/null --no-check-certificate https://127.0.0.1/api/categories; then
    sudo docker image prune -f
    exit 0
  fi
  sleep 5
done

echo "health check failed" >&2
sudo docker compose logs --tail 200 backend >&2
exit 1
