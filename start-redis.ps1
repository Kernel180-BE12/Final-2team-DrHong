# 이 스크립트는 Docker를 사용하여 Redis 컨테이너를 시작합니다.

Write-Host "Starting Redis container in the background..."
docker-compose up -d redis

Write-Host ""
Write-Host "Checking container status:"
docker-compose ps
