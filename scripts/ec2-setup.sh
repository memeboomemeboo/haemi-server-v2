#!/bin/bash
# EC2 초기 설정 스크립트 (Ubuntu 24.04 기준)
# sudo 권한으로 실행: bash ec2-setup.sh

set -e

echo "=== Docker 설치 ==="
apt-get update -q
apt-get install -y -q ca-certificates curl
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update -q
apt-get install -y -q docker-ce docker-ce-cli containerd.io docker-compose-plugin

echo "=== docker 그룹에 현재 사용자 추가 ==="
usermod -aG docker "${SUDO_USER:-ubuntu}"

echo "=== 앱 디렉토리 생성 ==="
mkdir -p /opt/haemi
chown "${SUDO_USER:-ubuntu}:${SUDO_USER:-ubuntu}" /opt/haemi

echo "=== docker-compose.yml / .env 파일 복사 안내 ==="
echo "다음 파일을 EC2 /opt/haemi/ 에 업로드하세요:"
echo "  scp docker-compose.yml .env ec2-user@<HOST>:/opt/haemi/"
echo ""
echo "그 후 최초 배포:"
echo "  cd /opt/haemi && docker compose up -d"
echo ""
echo "=== 완료 (재로그인 후 docker 명령 사용 가능) ==="
