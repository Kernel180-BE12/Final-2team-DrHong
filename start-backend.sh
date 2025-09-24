#!/bin/bash

# =====================================================
# Final-2team-DrHong 개발 환경 설정 스크립트 (macOS/Linux)
# =====================================================
# 이 스크립트는 다음 작업을 수행합니다:
# 1. SSH 터널을 통해 AWS RDS MySQL에 연결 (포트 3307)
# 2. Docker Compose를 통해 Redis 컨테이너/Python-AI 컨테이너 시작 (포트 6379/8000)
# 3. 개발에 필요한 모든 인프라 서비스를 한 번에 시작
# =====================================================

echo "=== Final-2team-DrHong 개발 환경 시작 ==="

echo "환경변수 파일 확인 중..."

# .env 파일이 존재하면 환경변수 로드
if [ -f .env ]; then
  echo ".env 파일에서 환경변수를 로드합니다..."
  set -a  # 모든 변수를 환경변수로 자동 export
  source .env
  set +a  # 자동 export 해제
else
  echo "경고: .env 파일을 찾을 수 없습니다."
fi

# PEM 키 경로가 설정되어 있는지 확인
echo "PEM 키 경로 확인 중..."
if [ -z "$DRHONG_PEM_KEY_PATH" ]; then
  echo "[ERROR] DRHONG_PEM_KEY_PATH가 설정되지 않았습니다."
  echo ".env.example 파일을 참고하여 .env 파일을 생성하고 경로를 설정해주세요."
  exit 1
fi

echo "PEM 키 경로 설정: $DRHONG_PEM_KEY_PATH"

echo ""
echo "=== 1. SSH 터널 설정 (RDS MySQL 연결) ==="

# 기존에 3307 포트를 사용하는 프로세스가 있는지 확인하고 종료
echo "기존 SSH 터널 프로세스 확인 및 정리 중..."
if lsof -i :3307 >/dev/null 2>&1; then
  echo "기존 3307 포트 연결을 종료합니다..."
  lsof -i :3307 | awk 'NR>1 {print $2}' | xargs -r kill -9
  sleep 1
fi

# SSH 터널 시작 (백그라운드에서 실행)
echo "SSH 터널을 시작합니다..."
ssh -fN -L 3307:drhong-db.cny6cmeagio6.ap-northeast-2.rds.amazonaws.com:3306 -i "$DRHONG_PEM_KEY_PATH" ec2-user@43.202.67.248

# SSH 터널 시작 결과 확인
if [ $? -eq 0 ]; then
  echo "SSH 터널이 성공적으로 시작되었습니다. (포트 3307)"
else
  echo "[ERROR] SSH 터널 시작 실패"
  exit 1
fi

# SSH 터널이 안정화될 때까지 잠시 대기
echo "터널 연결 안정화 대기 중..."
sleep 3

echo ""
echo "=== 2. Redis 컨테이너 시작 ==="

# Docker Compose를 통해 Redis 컨테이너 시작
echo "Redis 컨테이너를 백그라운드에서 시작합니다..."
if docker-compose up -d redis; then
  echo "Redis 컨테이너가 성공적으로 시작되었습니다. (포트 6379)"
else
  echo "[WARNING] Redis 컨테이너 시작 중 문제가 발생했습니다."
fi

# SSH 터널이 안정화될 때까지 잠시 대기
echo "터널 연결 안정화 대기 중..."
sleep 3

echo ""
echo "=== 3. Python-AI 컨테이너 시작 ==="

# Docker Compose를 통해 Python-AI 컨테이너 시작
echo "Python-AI 컨테이너를 백그라운드에서 시작합니다..."
if docker-compose up -d python-ai; then
  echo "Python-AI 컨테이너가 성공적으로 시작되었습니다. (포트 8000)"
else
  echo "[WARNING] Python-AI 컨테이너 시작 중 문제가 발생했습니다."
fi

echo ""
echo "=== 4. 컨테이너 상태 확인 ==="
docker-compose ps

echo ""
echo "=== 개발 환경 설정 완료 ==="
echo "다음 서비스가 사용 가능합니다:"
echo "- MySQL (RDS): localhost:3307"
echo "- Redis (Docker): localhost:6379"
echo "- Python-AI (Docker): localhost:8000"
echo ""
echo "Spring Boot 애플리케이션을 시작할 수 있습니다."

exit 0