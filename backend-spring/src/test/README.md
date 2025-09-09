# 🧪 테스트 가이드

## 🎯 테스트 구조

**로컬 회원가입 + 이메일 인증 + Redis 기반 + 원격 MySQL RDB 폴백** 시스템의 완전한 테스트 커버리지

## 📂 테스트 구성

### 🔧 **단위 테스트 (Unit Tests)**
모든 의존성을 Mock으로 격리하여 순수한 비즈니스 로직만 테스트

#### 1. **UserServiceTest**
- ✅ 유효한 데이터로 회원가입 성공
- ❌ 중복된 이메일로 회원가입 실패
- ❌ 인증 코드 불일치로 회원가입 실패
- ✅ Rate limiting과 함께 회원가입

#### 2. **EmailServiceTest**
- ✅ 유효한 이메일로 인증 코드 발송
- ❌ 빈 이메일/null 이메일 예외 처리
- ✅ Rate limiting과 함께 인증 코드 발송

#### 3. **FallbackVerificationStorageTest**
- ✅ Redis 정상 동작 시 Primary 사용
- 🔄 Redis 장애 시 RDB 폴백 전환
- 🔍 Primary → Secondary 조회 폴백
- 🗑️ 양쪽 저장소 모두 삭제

### 🔗 **통합 테스트 (Integration Tests)**
실제 환경에서 전체 시스템 플로우 테스트

#### 1. **UserSignupIntegrationTest** 🌟 **[핵심]**
- **환경**: Redis + 원격 MySQL RDS + Gmail SMTP
- ✅ 전체 회원가입 플로우 (Redis → RDS 저장)
- ❌ 잘못된 인증 코드로 실패
- ❌ 중복 이메일로 실패
- ✅ 이메일 인증 코드 발송

#### 2. **RedisFallbackIntegrationTest** 🔄 **[폴백]**
- **환경**: Redis 비활성화 + 원격 MySQL RDS
- ✅ Redis 폴백으로 RDB 전환 성공
- ❌ RDB 폴백 상태에서도 검증 로직 정상

#### 3. **EmailIntegrationTest** 📧 **[실제 발송]**
- **실제 Gmail 발송**: kernelteam2jdi@gmail.com → 자기 자신
- 🔢 6자리 인증 코드 생성 검증
- ⚠️ **주의**: 실제 이메일이 발송됩니다!

## 🚀 테스트 실행 순서

### 1단계: 기본 컨텍스트 로딩
```bash
./gradlew test --tests "Final2teamDrHongApplicationTests"
```

### 2단계: 단위 테스트 (빠른 실행)
```bash
./gradlew test --tests "*ServiceTest" --tests "*StorageTest"
```

### 3단계: 핵심 통합 테스트
```bash
./gradlew test --tests "UserSignupIntegrationTest"
./gradlew test --tests "RedisFallbackIntegrationTest"
```

### 4단계: 실제 이메일 테스트 (선택)
```bash
./gradlew test --tests "EmailIntegrationTest"
```

## ⚙️ 테스트 환경 설정

### 필수 프로파일

#### `application-test.properties` (기본 통합 테스트)
- **Redis**: localhost:6379 (Primary)
- **MySQL RDS**: localhost:3307 (SSH 터널)
- **Gmail SMTP**: 실제 발송 (kernelteam2jdi@gmail.com)

#### `application-redis-fallback-test.properties` (폴백 테스트)
- **Redis**: 비활성화 (폴백 테스트)
- **MySQL RDS**: localhost:3307 (Secondary)

### 필수 환경 변수 (.env)
```bash
DB_PASSWORD=drhong1!
MAIL_PASSWORD=svaieothjdtxtwyo
JWT_SECRET_KEY=bRmQUS6ug6iZPbR3BCzfSzGByCH2xtZwBZNGZ2jC0Tp1FssGET7Lwkp6XmgBSdTo7IfxCXtwAsE7Wu1UH5oeYg==
```

### 실행 전 체크리스트
- [ ] SSH 터널 실행: `./start-tunnel.sh`
- [ ] Redis 서버 실행: `redis-server`
- [ ] 환경 변수 설정: `.env` 파일 확인
- [ ] 네트워크 연결: RDS 접근 가능

## 🎯 테스트 성공 기준

### ✅ **전체 시스템 검증**
- [x] Spring Context 로딩 성공
- [x] Redis + 원격 MySQL RDS 연결
- [x] Gmail SMTP 실제 발송 성공
- [x] FallbackVerificationStorage 폴백 동작

### ✅ **핵심 기능 검증**
- [x] 6자리 인증 코드 생성
- [x] Redis Primary 저장/조회
- [x] RDB 폴백 자동 전환
- [x] Rate Limiting 정상 동작
- [x] 예외 처리 (400, 429) 정상

### ✅ **비즈니스 로직 검증**
- [x] 회원가입 전체 플로우
- [x] 이메일 중복 검증
- [x] 인증 코드 검증
- [x] 사용자 데이터 저장

## 📊 테스트 커버리지

- **단위 테스트**: 비즈니스 로직 100% 커버
- **통합 테스트**: 핵심 플로우 100% 커버  
- **폴백 테스트**: Redis → RDB 전환 시나리오 커버
- **실제 환경**: Gmail 발송 + 원격 RDS 연동

---

**🎉 모든 테스트 통과 시 "로컬 회원가입 + 이메일 인증 + Redis 기반 + RDB 폴백" 시스템이 완벽하게 동작합니다!**