# =====================================================
# Final-2team-DrHong 개발 환경 설정 스크립트 (Windows)
# =====================================================
# 이 스크립트는 다음 작업을 수행합니다:
# 1. SSH 터널을 통해 AWS RDS MySQL에 연결 (포트 3307)
# 2. Docker Compose를 통해 Redis 컨테이너 시작 (포트 6379)
# 3. 개발에 필요한 모든 인프라 서비스를 한 번에 시작
#
# 사용법:
# 1. PowerShell을 관리자 권한으로 실행
# 2. 실행 정책 설정 (최초 1회): Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
# 3. 스크립트 실행: .\start-tunnel.ps1
#
# 필요 조건:
# - OpenSSH Client (Windows 10 1809+ 기본 포함)
# - Docker Desktop for Windows
# - .env 파일에 DRHONG_PEM_KEY_PATH 설정
# =====================================================

# PowerShell 실행 정책 확인
$currentPolicy = Get-ExecutionPolicy -Scope CurrentUser
if ($currentPolicy -eq "Restricted") {
    Write-Host "[경고] PowerShell 실행 정책이 제한되어 있습니다." -ForegroundColor Yellow
    Write-Host "다음 명령어를 관리자 권한 PowerShell에서 실행하세요:" -ForegroundColor Yellow
    Write-Host "Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser" -ForegroundColor Cyan
    Write-Host ""
    Read-Host "실행 정책을 변경한 후 Enter 키를 눌러 계속하세요"
}

Write-Host "=== Final-2team-DrHong 개발 환경 시작 ===" -ForegroundColor Green

# 현재 스크립트 디렉토리 경로를 가져와서 .env 파일 위치 설정
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$envPath = Join-Path $scriptDir ".env"

Write-Host "환경변수 파일 확인 중..." -ForegroundColor Yellow

# .env 파일이 존재하면 환경변수 로드
if (Test-Path $envPath) {
    Write-Host ".env 파일에서 환경변수를 로드합니다..." -ForegroundColor Cyan
    Get-Content $envPath -Encoding UTF8 | ForEach-Object {
        # DRHONG_PEM_KEY_PATH 환경변수 추출 및 설정
        if ($_ -match '^\s*DRHONG_PEM_KEY_PATH\s*=\s*(.*)') {
            $path = $matches[1].Trim().Trim('"').Trim("'")
            $env:DRHONG_PEM_KEY_PATH = $path
            Write-Host "PEM 키 경로 설정: $path" -ForegroundColor Green
        }
    }
} else {
    Write-Host "경고: .env 파일을 찾을 수 없습니다." -ForegroundColor Yellow
}

# PEM 키 경로가 설정되어 있는지 확인
if (-not (Test-Path env:DRHONG_PEM_KEY_PATH -PathType Any)) {
    Write-Host "[ERROR] DRHONG_PEM_KEY_PATH가 설정되지 않았습니다." -ForegroundColor Red
    Write-Host ".env.example 파일을 참고하여 .env 파일을 생성하고 경로를 설정해주세요." -ForegroundColor Yellow
    Read-Host "계속하려면 Enter 키를 누르세요"
    exit 1
}

Write-Host "`n=== 1. SSH 터널 설정 (RDS MySQL 연결) ===" -ForegroundColor Blue

# 기존에 3307 포트를 사용하는 프로세스가 있는지 확인하고 종료
try {
    $existingProcess = Get-NetTCPConnection -LocalPort 3307 -ErrorAction SilentlyContinue
    if ($existingProcess) {
        Write-Host "기존 3307 포트 연결을 종료합니다..." -ForegroundColor Yellow
        Stop-Process -Id $existingProcess.OwningProcess -Force -ErrorAction SilentlyContinue
    }
} catch {
    # 포트를 사용하는 프로세스가 없는 경우 무시
}

# SSH 터널 시작 (백그라운드에서 실행)
Write-Host "SSH 터널을 시작합니다..." -ForegroundColor Cyan
$sshArgs = "-N -L 3307:drhong-db.cny6cmeagio6.ap-northeast-2.rds.amazonaws.com:3306 -i `"$env:DRHONG_PEM_KEY_PATH`" ec2-user@43.202.67.248"

try {
    Start-Process -FilePath 'ssh.exe' -ArgumentList $sshArgs -WindowStyle Hidden
    Write-Host "SSH 터널이 성공적으로 시작되었습니다. (포트 3307)" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] SSH 터널 시작 실패: $_" -ForegroundColor Red
    Read-Host "계속하려면 Enter 키를 누르세요"
    exit 1
}

# SSH 터널이 안정화될 때까지 잠시 대기
Write-Host "터널 연결 안정화 대기 중..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

Write-Host "`n=== 2. Redis 컨테이너 시작 ===" -ForegroundColor Blue

# Docker Compose를 통해 Redis 컨테이너 시작
try {
    Write-Host "Redis 컨테이너를 백그라운드에서 시작합니다..." -ForegroundColor Cyan
    docker-compose up -d redis

    if ($LASTEXITCODE -eq 0) {
        Write-Host "Redis 컨테이너가 성공적으로 시작되었습니다. (포트 6379)" -ForegroundColor Green
    } else {
        Write-Host "[WARNING] Redis 컨테이너 시작 중 문제가 발생했습니다." -ForegroundColor Yellow
    }
} catch {
    Write-Host "[ERROR] Redis 컨테이너 시작 실패: $_" -ForegroundColor Red
}

# SSH 터널이 안정화될 때까지 잠시 대기
Write-Host "터널 연결 안정화 대기 중..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

Write-Host "`n=== 3. Python-AI 컨테이너 시작 ===" -ForegroundColor Blue

# Docker Compose를 통해 Python-AI 컨테이너 시작
try {
    Write-Host "Python-AI 컨테이너를 백그라운드에서 시작합니다..." -ForegroundColor Cyan
    docker-compose up -d python-ai

    if ($LASTEXITCODE -eq 0) {
        Write-Host "Python-AI 컨테이너가 성공적으로 시작되었습니다. (포트 8000)" -ForegroundColor Green
    } else {
        Write-Host "[WARNING] Python-AI 컨테이너 시작 중 문제가 발생했습니다." -ForegroundColor Yellow
    }
} catch {
    Write-Host "[ERROR] Python-AI 컨테이너 시작 실패: $_" -ForegroundColor Red
}

Write-Host "`n=== 4. 컨테이너 상태 확인 ===" -ForegroundColor Blue
try {
    docker-compose ps
} catch {
    Write-Host "[WARNING] Docker Compose 상태 확인 실패" -ForegroundColor Yellow
}

Write-Host "`n=== 개발 환경 설정 완료 ===" -ForegroundColor Green
Write-Host "다음 서비스가 사용 가능합니다:" -ForegroundColor Cyan
Write-Host "- MySQL (RDS): localhost:3307" -ForegroundColor White
Write-Host "- Redis (Docker): localhost:6379" -ForegroundColor White
Write-Host "- Python-AI (Docker): localhost:8000" -ForegroundColor White
Write-Host "`nSpring Boot 애플리케이션을 시작할 수 있습니다." -ForegroundColor Green
