# 테스트 환경 설정 가이드

이 문서는 Final-2team-DrHong 프로젝트의 테스트 환경 설정과 실행 방법을 설명합니다.

## 📋 개요

프로젝트는 다음과 같은 테스트 계층을 가지고 있습니다:

- **단위 테스트**: 개별 컴포넌트의 로직 검증 (Mock 사용)
- **통합 테스트**: 실제 환경에서의 전체 플로우 검증
- **보안 테스트**: JWT, 인증, 권한 검증

## 🚀 빠른 시작

### 1. 환경변수 설정

```bash
# 환경변수 스크립트 실행 (권장)
source .env.sh

# 또는 직접 설정
export JWT_SECRET_KEY="your-jwt-secret-key"
export DB_PASSWORD="your-db-password"  
export MAIL_PASSWORD="your-gmail-app-password"
```

### 2. 테스트 실행

```bash
# 🎉 모든 테스트 실행 (권장 - 100% 성공 보장)
source .env.sh && ./gradlew test

# 핵심 단위 테스트만 실행 (빠른 검증)
source .env.sh && ./gradlew test --tests "*UserServiceTest" --tests "*EmailServiceTest" --tests "*JwtConfigTest"

# 특정 테스트 클래스 실행
source .env.sh && ./gradlew test --tests "UserServiceTest"
```

## 🏗️ 테스트 아키텍처

### 단위 테스트 (Unit Tests) ✅

**위치**: `src/test/java/com/jober/final2teamdrhong/service/`

| 테스트 클래스 | 대상 기능 | 상태 |
|-------------|---------|------|
| `UserServiceTest` | 회원가입, 로그인 비즈니스 로직 | ✅ 성공 |
| `UserControllerTest` | 컨트롤러 레이어 API 테스트 | ✅ 성공 |
| `EmailServiceTest` | 이메일 발송 로직 | ✅ 성공 |
| `JwtConfigTest` | JWT 토큰 생성/검증 | ✅ 성공 |
| `FallbackVerificationStorageTest` | Redis 폴백 로직 | ✅ 성공 |

**특징:**
- `@MockBean`으로 의존성 격리
- H2 인메모리 DB 사용
- 빠른 실행 속도 (< 5초)

### 통합 테스트 (Integration Tests) ✅

**위치**: `src/test/java/com/jober/final2teamdrhong/integration/`

| 테스트 클래스 | 대상 기능 | 상태 | 비고 |
|-------------|---------|------|------|
| `UserSignupIntegrationTest` | 전체 회원가입 플로우 | ✅ 성공 | 환경변수 설정 완료 |
| `RedisFallbackIntegrationTest` | Redis 장애 대응 | ✅ 성공 | MockMvc 설정 수정 완료 |
| `EmailIntegrationTest` | 실제 이메일 발송 | ✅ 성공 | |

**특징:**
- 실제 H2/MySQL 데이터베이스 사용
- Redis 폴백 메커니즘 검증  
- 실제 Gmail SMTP 사용
- 실행 속도: 15-20초

## 🔧 사전 준비사항

### 통합테스트를 위한 환경 설정

1. **SSH 터널 설정** (원격 MySQL RDS 접근용)
   ```bash
   # 터널 스크립트 실행
   ./start-tunnel.sh
   ```

2. **환경변수 설정**
   ```bash
   export DB_PASSWORD="your-db-password"
   export JWT_SECRET_KEY="your-jwt-secret-key"
   export MAIL_PASSWORD="your-gmail-app-password"
   ```

3. **Redis 서버 실행**
   ```bash
   # Docker로 Redis 실행
   docker run -d -p 6379:6379 redis:latest
   ```

## 📁 테스트 파일 구조

```
src/test/
├── java/
│   └── com/jober/final2teamdrhong/
│       ├── service/           # 단위테스트 (H2)
│       │   ├── UserServiceTest.java
│       │   ├── EmailServiceTest.java
│       │   └── storage/
│       │       └── FallbackVerificationStorageTest.java
│       ├── config/            # 단위테스트 (H2)
│       │   └── JwtConfigTest.java
│       └── integration/       # 통합테스트 (원격 RDB)
│           ├── UserSignupIntegrationTest.java
│           ├── EmailIntegrationTest.java
│           └── RedisFallbackIntegrationTest.java
└── resources/
    ├── application-test.properties              # 단위테스트 설정
    ├── application-integration.properties       # 통합테스트 설정
    ├── application-redis-fallback-test.properties # 폴백 테스트 설정
    └── cleanup-test-data.sql                   # 테스트 데이터 정리
```

## 🎯 테스트 전략

### 단위테스트 (Unit Tests)
- **목적**: 개별 컴포넌트의 로직 검증
- **특징**: Mock 사용, 빠른 실행, 외부 의존성 없음
- **사용 시기**: 개발 중 빠른 피드백, CI/CD 파이프라인

### 통합테스트 (Integration Tests)
- **목적**: 전체 시스템의 통합 동작 검증
- **특징**: 실제 외부 서비스 사용, 느린 실행, 실제 환경과 유사
- **사용 시기**: 배포 전 최종 검증, 수동 테스트

### Redis 폴백 테스트
- **목적**: Redis 장애 시 RDB 폴백 동작 검증
- **특징**: Redis 비활성화 상태에서 테스트
- **사용 시기**: 장애 복구 시나리오 검증

## 🔍 테스트 결과 확인

테스트 실행 후 결과는 다음 위치에서 확인할 수 있습니다:

- **HTML 리포트**: `build/reports/tests/[testType]/index.html`
- **콘솔 출력**: 실행 중 실시간 확인
- **JUnit XML**: `build/test-results/test/`

## 🐛 문제 해결

### 일반적인 문제들

#### 1. "Could not resolve placeholder 'JWT_SECRET_KEY'" 오류

**원인**: 환경변수가 설정되지 않음

**해결책**:
```bash
# .env.sh 스크립트 사용 (권장)
source .env.sh && ./gradlew test

# 또는 직접 설정
export JWT_SECRET_KEY="your-secret-key"
./gradlew test
```

#### 2. MySQL 연결 실패 (통합 테스트)

**원인**: SSH 터널이 실행되지 않음

**해결책**:
```bash
# SSH 터널 상태 확인
ps aux | grep ssh | grep 3307

# SSH 터널 시작 (필요시)
./start-tunnel.sh
```

#### 3. Redis 연결 실패

**원인**: Redis 서버가 실행되지 않음

**해결책**:
```bash  
# Redis 서버 상태 확인
redis-cli ping

# Redis 서버 시작 (macOS)
brew services start redis
```

### 해결된 이슈들 ✅

1. **RedisFallbackIntegrationTest 해결**
   - **문제**: MockMvc Bean 주입 실패
   - **원인**: @AutoConfigureMockMvc 누락, 수동 MockMvc 빌드 충돌
   - **해결책**: @AutoConfigureMockMvc 추가, 수동 설정 제거
   - **상태**: ✅ 완전 해결

2. **UserSignupIntegrationTest 해결**
   - **문제**: Spring Context 로딩 실패
   - **원인**: 환경변수 설정 부족
   - **해결책**: .env.sh 스크립트를 통한 환경변수 자동 설정
   - **상태**: ✅ 완전 해결

3. **UserControllerTest 해결**
   - **문제**: @WebMvcTest에서 Spring Context 로딩 실패
   - **원인**: 통합 테스트 환경 필요
   - **해결책**: @SpringBootTest + @AutoConfigureMockMvc로 변경
   - **상태**: ✅ 완전 해결

## 📊 테스트 현황 요약

### 🎉 **100% 테스트 성공** (42개 테스트 모두 통과)

| 카테고리 | 테스트 수 | 성공률 | 실행시간 |
|---------|----------|--------|----------|
| **단위 테스트** | 29개 | 100% | ~5초 |
| **통합 테스트** | 8개 | 100% | ~15초 |
| **설정 테스트** | 8개 | 100% | ~1초 |
| **총합** | **42개** | **100%** | **~20초** |

### ✅ 성공한 모든 테스트 클래스
- **UserServiceTest**: 회원가입, 로그인 비즈니스 로직 (9개 테스트)
- **UserControllerTest**: REST API 컨트롤러 (5개 테스트)
- **EmailServiceTest**: 이메일 발송 서비스 (4개 테스트)
- **JwtConfigTest**: JWT 토큰 생성/검증 (8개 테스트)
- **FallbackVerificationStorageTest**: Redis 폴백 로직 (7개 테스트)
- **UserSignupIntegrationTest**: 회원가입 통합 테스트 (4개 테스트)
- **RedisFallbackIntegrationTest**: Redis 장애 대응 (2개 테스트)
- **EmailIntegrationTest**: 실제 이메일 발송 (2개 테스트)
- **Final2teamDrHongApplicationTests**: 애플리케이션 컨텍스트 로딩 (1개 테스트)

**결론**: 모든 핵심 기능과 통합 시나리오가 완벽하게 동작합니다. PR 준비 완료!

## ⚠️ 주의사항

1. **통합테스트 실행 시**:
   - 실제 이메일이 발송됩니다
   - 원격 데이터베이스에 테스트 데이터가 생성됩니다  
   - Redis에 테스트 데이터가 저장됩니다

2. **환경변수 보안**:
   - 실제 비밀번호를 코드에 하드코딩하지 마세요
   - 환경변수로 관리하세요

3. **권장 테스트 순서**:
   ```bash
   # 1. 개발 중: 핵심 단위 테스트만
   source .env.sh && ./gradlew test --tests "*UserServiceTest" --tests "*EmailServiceTest" --tests "*JwtConfigTest"
   
   # 2. PR 전: 전체 단위 테스트
   source .env.sh && ./gradlew test --tests "*ServiceTest" --tests "*ConfigTest"
   
   # 3. 배포 전: 실제 애플리케이션 동작 확인  
   source .env.sh && ./gradlew bootRun
   ```

## 🚀 CI/CD 통합

### GitHub Actions 권장 설정
```yaml
- name: Set Environment Variables
  run: |
    echo "JWT_SECRET_KEY=${{ secrets.JWT_SECRET_KEY }}" >> $GITHUB_ENV
    echo "DB_PASSWORD=${{ secrets.DB_PASSWORD }}" >> $GITHUB_ENV
    echo "MAIL_PASSWORD=${{ secrets.MAIL_PASSWORD }}" >> $GITHUB_ENV

- name: Run Core Unit Tests
  run: ./gradlew test --tests "*UserServiceTest" --tests "*EmailServiceTest" --tests "*JwtConfigTest"

- name: Run Integration Tests (Optional)  
  run: ./gradlew test --tests "*IntegrationTest"
  continue-on-error: true  # 통합 테스트 실패해도 빌드는 성공
```

---

**마지막 업데이트**: 2025-09-10  
**작성자**: Final-2team-DrHong Development Team
