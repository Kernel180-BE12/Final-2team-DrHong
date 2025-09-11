# 테스트 가이드 / Test Guide

## 🎯 테스트 현황 / Test Status

**전체 테스트: 84개 (100% 통과)** / **Total Tests: 84 (100% Pass)**

| 카테고리 / Category | 테스트 수 / Test Count | 설명 / Description |
|-------|--------|------------|
| 통합 테스트 / Integration | 15 | 실제 Redis + MySQL RDS 환경 / Real Redis + MySQL RDS |
| 서비스 테스트 / Service | 33 | 비즈니스 로직 검증 / Business Logic Validation |
| 설정 테스트 / Config | 17 | 설정 클래스 검증 / Configuration Class Validation |
| 스토리지 테스트 / Storage | 13 | Redis/Fallback 저장소 / Redis/Fallback Storage |
| 컨트롤러 테스트 / Controller | 5 | REST API 엔드포인트 / REST API Endpoints |
| 애플리케이션 / Application | 1 | Spring Boot 컨텍스트 / Spring Boot Context |

## 🚀 빠른 시작 / Quick Start

### 1. 환경 설정 / Environment Setup

```bash
# 환경변수 설정 / Set environment variables
source .env.sh

# Redis 실행 확인 / Verify Redis is running
redis-cli ping
# Expected: PONG

# SSH 터널 시작 (원격 DB 사용 시) / Start SSH tunnel (for remote DB)
./start-tunnel.sh
```

### 2. 테스트 실행 / Run Tests

```bash
# 모든 테스트 실행 / Run all tests
./gradlew test

# 특정 테스트 클래스 실행 / Run specific test class
./gradlew test --tests "com.jober.final2teamdrhong.integration.UserLoginIntegrationTest"

# 패턴으로 테스트 실행 / Run tests by pattern
./gradlew test --tests "*Integration*"

# 테스트 리포트 확인 / View test report
open build/reports/tests/test/index.html
```

## 📋 테스트 전략 / Test Strategy

### 1. 통합 테스트 (Integration Tests)
**위치 / Location**: `src/test/java/.../integration/`

#### 핵심 테스트 / Core Tests:
- `UserLoginIntegrationTest` - JWT 인증, 리프레시 토큰, 블랙리스트
- `UserSignupIntegrationTest` - 회원가입, 이메일 인증
- `EmailIntegrationTest` - 이메일 발송 (Gmail SMTP)
- `RedisFallbackIntegrationTest` - Redis 장애 시 폴백

**특징 / Features**:
- 실제 Redis 및 MySQL RDS 연결
- `@SpringBootTest` + `@Transactional` 롤백
- 테스트 간 격리 (`@BeforeEach`에서 Redis 초기화)

### 2. 단위 테스트 (Unit Tests)
**위치 / Location**: `src/test/java/.../service/`, `src/test/java/.../config/`

#### 주요 테스트 / Main Tests:
- `UserServiceTest` - 사용자 서비스 로직
- `BlacklistServiceTest` - 토큰 블랙리스트
- `EmailServiceTest` - 이메일 서비스
- `SecurityAuditServiceTest` - 보안 감사
- `JwtConfigTest` - JWT 토큰 생성/검증
- `AuthPropertiesTest` - 인증 설정

**특징 / Features**:
- Mockito를 사용한 의존성 격리
- 빠른 실행 속도
- 세밀한 로직 검증

## 🔧 테스트 설정 / Test Configuration

### 프로파일 / Profiles

| 프로파일 / Profile | 용도 / Purpose | 데이터베이스 / Database | Redis |
|---------|--------|----------|-------|
| `test` (기본) | 단위 테스트 / Unit tests | H2 In-Memory | Mock |
| `integration` | 통합 테스트 / Integration tests | MySQL RDS (SSH 터널) | 실제 Redis |
| `redis-fallback-test` | 폴백 테스트 / Fallback tests | H2 In-Memory | Dummy Redis |

### 환경 변수 / Environment Variables

```bash
# .env.sh 파일 내용 / .env.sh file contents
export JWT_SECRET_KEY="your-secret-key"
export DB_PASSWORD="your-db-password"
export MAIL_PASSWORD="your-gmail-app-password"
```

## 🛠️ 주요 테스트 시나리오 / Key Test Scenarios

### 인증 플로우 / Authentication Flow
1. **이메일 인증 코드 발송** → Redis 저장 (5분 TTL)
2. **회원가입** → 코드 검증 → 사용자 생성
3. **로그인** → JWT 액세스 토큰 + 리프레시 토큰 발급
4. **토큰 갱신** → 리프레시 토큰으로 새 액세스 토큰 발급
5. **로그아웃** → 토큰 블랙리스트 등록

### Rate Limiting 검증 / Rate Limiting Validation
- 이메일 인증: IP당 5분에 3회
- 이메일 검증: 이메일당 10분에 5회
- 회원가입: IP당 시간당 10회

### 보안 테스트 / Security Tests
- 타이밍 공격 방지 (일정한 응답 시간)
- 잘못된 인증 시 더미 해시 사용
- 토큰 rotation 정책 (1회 사용)
- 블랙리스트 검증

## 📊 테스트 커버리지 / Test Coverage

| 영역 / Area | 커버리지 / Coverage | 설명 / Description |
|------|----------|-------------|
| 인증 / Auth | 100% | 회원가입, 로그인, JWT |
| 이메일 / Email | 100% | 인증 코드 발송/검증 |
| Rate Limiting | 100% | 모든 제한 정책 |
| Redis 폴백 / Fallback | 100% | 장애 시 메모리 저장 |
| 보안 / Security | 95% | 주요 보안 시나리오 |

## 🐛 문제 해결 / Troubleshooting

### Redis 연결 실패 / Redis Connection Failed
```bash
# Redis 실행 확인 / Check Redis status
docker ps | grep redis
redis-cli ping

# Redis 재시작 / Restart Redis
docker restart redis
```

### SSH 터널 연결 실패 / SSH Tunnel Failed
```bash
# 터널 프로세스 확인 / Check tunnel process
ps aux | grep ssh | grep 3307

# 터널 재시작 / Restart tunnel
pkill -f "ssh.*3307"
./start-tunnel.sh
```

### 테스트 격리 문제 / Test Isolation Issues
```java
@BeforeEach
void setUp() {
    // Redis 데이터 초기화 / Clear Redis data
    redisTemplate.getConnectionFactory()
        .getConnection()
        .serverCommands()
        .flushAll();
}
```

## 📝 테스트 작성 가이드 / Test Writing Guide

### 통합 테스트 템플릿 / Integration Test Template
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("integration")
class YourIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        // Redis 초기화
    }
    
    @Test
    @DisplayName("한글 설명 / English description")
    void testMethod() throws Exception {
        // Given
        // When
        // Then
    }
}
```

### 단위 테스트 템플릿 / Unit Test Template
```java
@ExtendWith(MockitoExtension.class)
class YourServiceTest {
    
    @Mock
    private DependencyService dependency;
    
    @InjectMocks
    private YourService service;
    
    @Test
    @DisplayName("테스트 설명")
    void testMethod() {
        // Given
        given(dependency.method()).willReturn(value);
        
        // When
        var result = service.method();
        
        // Then
        assertThat(result).isEqualTo(expected);
    }
}
```

## 🚦 CI/CD 통합 / CI/CD Integration

```yaml
# GitHub Actions 예시 / GitHub Actions example
test:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 21
      uses: actions/setup-java@v2
      with:
        java-version: '21'
    - name: Start Redis
      run: docker run -d -p 6379:6379 redis:7-alpine
    - name: Run tests
      run: ./gradlew test
      env:
        JWT_SECRET_KEY: ${{ secrets.JWT_SECRET_KEY }}
        DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
        MAIL_PASSWORD: ${{ secrets.MAIL_PASSWORD }}
```

## 📚 참고 자료 / References

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-testing)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [TestContainers](https://www.testcontainers.org/) - 추후 도입 고려

---

**마지막 업데이트 / Last Updated**: 2025-01-11
**관리자 / Maintainer**: DrHong Team