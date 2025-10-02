# 🔔 notimo - AI 기반 알림톡 템플릿 생성 및 관리 플랫폼

## 프로젝트 소개 Introduction
Spring Boot와 AI 기술을 결합한 지능형 알림톡 템플릿 관리 시스템으로, **AI 기반 자동 템플릿 생성**과 **검증 기능**을 제공하여 효율적인 마케팅 메시지 관리를 지원합니다.

### 주요 특징
- 🤖 **AI 기반 템플릿 자동 생성** (OpenAI/Gemini)
- ✅ **AI 템플릿 검증 시스템** (파인튜닝된 분류 모델)
- 🏢 **다중 워크스페이스 지원**
- 📱 **주소록 및 수신자 그룹 관리**
- ⭐ **템플릿 즐겨찾기 기능**
- 🔐 **이메일 인증 + OAuth2 소셜 로그인**
- 🛡️ **JWT 인증 + Rate Limiting 보안**
- 🚀 **Virtual Threads 성능 최적화** (JDK 21)

## 📋 목차
- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [API 명세서](#api-명세서)
- [설치 및 실행](#설치-및-실행)
- [데이터베이스 설정](#데이터베이스-설정)
- [환경 설정](#환경-설정)
- [트러블슈팅](#트러블슈팅)

## 🎯 프로젝트 개요

### 아키텍처
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │  Spring Boot    │    │   Python AI     │    │   Database      │
│   (React)       │◄──►│   (Backend)     │◄──►│   (FastAPI)     │    │   (MySQL/Redis) │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
│                  │                      │                      │                      │
│  - React 19      │  - Spring Boot 3.5   │  - FastAPI          │  - AWS RDS MySQL    │
│  - Material-UI   │  - Spring Security   │  - LangChain        │  - Redis (캐시)     │
│  - React Router  │  - JWT Auth          │  - ChromaDB         │  - H2 (테스트)      │
│  - Axios         │  - JPA/Hibernate     │  - PyTorch          │                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

## 🛠 기술 스택

### Backend (Java Spring Boot)
- **Java 21** - Virtual Threads 지원
- **Spring Boot 3.5.5** - 웹 애플리케이션 프레임워크
- **Spring Security + OAuth2 Client** - 인증/인가
- **Spring Data JPA** - ORM 및 데이터 액세스
- **Spring Session JDBC** - 세션 관리
- **JWT (jjwt 0.12.6)** - 토큰 기반 인증
- **Redis + Bucket4j** - Rate Limiting
- **MySQL 8.0** - 관계형 데이터베이스 (AWS RDS)
- **H2 Database** - 테스트 환경

### AI Service (Python FastAPI)
- **Python 3.12** - AI 서비스 백엔드
- **FastAPI** - AI API 서버 프레임워크
- **LangChain** - LLM 오케스트레이션
- **MLP-KTLim/llama-3-Korean-Bllossom-8B** - 템플릿 생성 베이스 모델
- **klue/bert-base** - 템플릿 검증 베이스 모델
- **Google Gemini** - 대체 AI 모델
- **ChromaDB** - 벡터 데이터베이스 (가이드라인 검색)
- **PyTorch 2.5.1** - 딥러닝 프레임워크
- **Transformers (Hugging Face)** - 사전학습 모델
- **PEFT, BitsAndBytes** - 모델 파인튜닝
- [Fine-Tuning Repository](https://github.com/Kernel180-BE12/Final-2team-DrHong-Finetuning)

### Frontend
- **React 19.1.1** - 프론트엔드 프레임워크
- **Material-UI (MUI) 7.3.2** - UI 컴포넌트 라이브러리
- **React Router DOM 7.8.2** - 라우팅
- **Axios 1.12.2** - HTTP 클라이언트
- **Emotion** - CSS-in-JS 스타일링

### Infrastructure & DevOps
- **Docker & Docker Compose** - 컨테이너 기반 배포
- **Nginx** - 웹 서버 (프론트엔드)
- **AWS EC2** - 애플리케이션 서버
- **AWS RDS** - 데이터베이스 서버
- **Redis** - 캐시 및 세션 저장소
- **SSH Tunnel** - 보안 연결

### 개발 도구
- **IntelliJ IDEA** - 통합 개발 환경
- **Gradle** - 의존성 관리 및 빌드 도구
- **Git & GitHub** - 버전 관리
- **SpringDoc OpenAPI (Swagger)** - API 문서화
- **PyCharm** - Python AI 서비스 개발

## ✨ 주요 기능

### 👤 사용자 기능

#### 🔐 **회원 관리**
- ✅ 이메일 인증 기반 회원가입
- ✅ 로그인/로그아웃 (JWT 토큰)
- ✅ OAuth2 소셜 로그인 (Google)
- ✅ 소셜 계정에 로컬 인증 추가 (같은 이메일 일시 이메일 인증 후) 
- ✅ 로컬 계정에 소셜 로그인시 자동 추가 (같은 이메일 일시)
- ✅ 비밀번호 재설정 (이메일 코드 발송)
- ✅ 비밀번호 변경
- ✅ 회원 탈퇴 (Soft Delete)
- ✅ 계정 잠금 (로그인 실패 5회 시 30분)

#### 🏢 **워크스페이스 관리**
- ✅ 워크스페이스 생성/조회/수정/삭제
- ✅ 워크스페이스별 템플릿 관리
- ✅ 워크스페이스별 주소록 관리
- ✅ 고유 URL 기반 워크스페이스 접근

#### 📝 **템플릿 관리**

**개인 템플릿**
- ✅ 빈 템플릿 생성 (동기/비동기)
- ✅ 공용 템플릿 기반 개인 템플릿 생성
- ✅ 템플릿 수정 (AI 채팅 이력 저장)
- ✅ 템플릿 삭제 (Soft Delete)
- ✅ 템플릿 상태 관리 (DRAFT, PENDING, APPROVED, REJECTED)
- ✅ 템플릿 목록 조회 (워크스페이스별)

**공용 템플릿**
- ✅ 공용 템플릿 등록
- ✅ 공용 템플릿 검색 및 필터링
- ✅ 조회수/공유수 통계
- ✅ 정렬 기능 (최신순, 인기순, 공유순)

#### 🤖 **AI 기능**
- ✅ 사용자 프롬프트 기반 알림톡 템플릿 자동 생성
- ✅ AI 기반 템플릿 승인/거부 분류
- ✅ 가이드라인 기반 벡터 검색 (ChromaDB)
- ✅ 다중 AI 모델 지원 (OpenAI/Gemini)
- ✅ 파인튜닝된 분류 모델 활용

#### ⭐ **즐겨찾기**
- ✅ 개인 템플릿 즐겨찾기 추가/삭제
- ✅ 공용 템플릿 즐겨찾기 추가/삭제
- ✅ 즐겨찾기 목록 조회
- ✅ 템플릿 타입별 구분 (INDIVIDUAL/PUBLIC)

#### 📱 **주소록 및 수신자 관리**

**주소록 (그룹)**
- ✅ 주소록 생성/수정/삭제
- ✅ 주소록에 수신자 추가/제거
- ✅ 주소록 목록 조회
- ✅ 주소록 상세 조회 (포함된 수신자 목록)

**수신자**
- ✅ 수신자 생성/수정/삭제
- ✅ 수신자 목록 조회 (페이징 지원)
- ✅ 수신자 검색
- ✅ 다중 그룹 소속 지원

### 👨‍💼 관리자 기능

#### 🔒 **관리자 권한 제어**
- ✅ ADMIN 권한 기반 접근 제어
- ✅ 공용 템플릿 삭제 권한

## 🌐 API 명세서

### 📋 **인증 관련 API** (`/api/auth`)

#### 이메일 인증
| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| POST | `/api/auth/send-verification-code` | 이메일 인증 코드 발송 | `email` | String |
| POST | `/api/auth/verify-code` | 인증 코드 검증 | `email`, `code` | String |

#### 회원가입/로그인
| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| POST | `/api/auth/signup` | 회원가입 | `SignupRequest` | `AuthResponse` |
| POST | `/api/auth/login` | 로그인 | `LoginRequest` | `AuthResponse` |
| POST | `/api/auth/refresh` | 토큰 갱신 | `refreshToken` | `AuthResponse` |
| POST | `/api/auth/logout` | 로그아웃 | - | String |

#### 비밀번호 관리
| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| POST | `/api/auth/password-reset/send-code` | 비밀번호 재설정 코드 발송 | `email` | String |
| POST | `/api/auth/password-reset/confirm` | 비밀번호 재설정 확인 | `PasswordResetConfirmRequest` | String |

#### 소셜 로그인
| Method | Endpoint | Description | Parameters | Response |
|--------|----------|-------------|------------|----------|
| GET | `/api/auth/social/login/google` | Google OAuth2 로그인 | - | Redirect to Google |
| GET | `/api/auth/social/callback/google` | Google OAuth2 콜백 | `code`, `state` | Redirect with tokens |
| POST | `/api/auth/social/signup` | 소셜 회원가입 완료 | `SocialSignupRequest` | `AuthResponse` |
| POST | `/api/auth/add-local` | 소셜 계정에 로컬 인증 추가 | `AddLocalAuthRequest` | String |
| GET | `/api/auth/methods/{email}` | 사용 가능한 인증 방법 조회 | `email` | `List<AuthMethodResponse>` |

### 👤 **사용자 관리 API** (`/api/users`)
| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| GET | `/api/users/profile` | 사용자 프로필 조회 | - | `UserProfileResponse` |
| PUT | `/api/users/password` | 비밀번호 변경 | `PasswordChangeRequest` | String |
| DELETE | `/api/users` | 회원 탈퇴 | - | String |

### 🏢 **워크스페이스 API** (`/api/workspaces`)
| Method | Endpoint | Description | Request                                                  | Response                  |
|--------|----------|-------------|----------------------------------------------------------|---------------------------|
| POST | `/api/workspaces` | 워크스페이스 생성 | `WorkspaceRequest.CreateDTO, jwtClaims` | `WorkspaceResponse.SimpleDTO` |
| GET | `/api/workspaces` | 워크스페이스 목록 조회 | `jwtClaims` | `List<WorkspaceResponse.SimpleDTO>` |
| GET | `/api/workspaces/{workspaceId}` | 워크스페이스 상세 조회 | `workspaceId`, `jwtClaims` | `WorkspaceResponse.DetailDTO` |
| PUT | `/api/workspaces/{workspaceId}` | 워크스페이스 수정 | `WorkspaceRequest.UpdateDTO`, `workspaceId`, `jwtClaims` | `WorkspaceResponse.DetailDTO` |
| DELETE | `/api/workspaces/{workspaceId}` | 워크스페이스 삭제 | `workspaceId`, `jwtClaims` | `WorkspaceResponse.SimpleDTO` |

### 📝 **개인 템플릿 API** (`/api/templates`)
| Method | Endpoint                                                          | Description     | Request | Response                          |
|--------|-------------------------------------------------------------------|-----------------|---------|-----------------------------------|
| POST | `/api/templates/{workspaceId}`                                    | 빈 템플릿 생성        | `workspaceId`, `TemplateCreateRequest` | `IndividualTemplateResponse`      |
| POST | `/api/templates/{workspaceId}/async`                              | 빈 템플릿 생성 (비동기)  | `workspaceId`, `TemplateCreateRequest` | `IndividualTemplateResponse`                           |
| POST | `/api/templates/{workspaceId}/from-public/{publicTemplateId}`     | 공용 템플릿 복사       | `workspaceId`, `publicTemplateId` | `IndividualTemplateResponse`      |
| POST | `/api/templates/{workspaceId}/from-public/{publicTemplateId}/async` | 공용 템플릿 복사 (비동기) | `workspaceId`, `publicTemplateId` | `IndividualTemplateResponse`      |
| GET | `/api/templates/{workspaceId}`                                    | 템플릿 목록 조회       | `workspaceId` | `List<IndividualTemplateResponse>` |
| GET | `/api/templates/{workspaceId}/{templateId}`                       | 템플릿 상세 조회       | `workspaceId`, `templateId` | `IndividualTemplateResponse`      |
| GET | `/api/templates/{workspaceId}/{templateId}/async`                 | 템플릿 상세 조회 (비동기) | `workspaceId`, `templateId` | `IndividualTemplateResponse`      |
| PUT | `/api/templates/{workspaceId}/{templateId}`                       | 템플릿 수정          | `workspaceId`, `templateId`, `TemplateUpdateRequest` | `IndividualTemplateResponse`      |
| PUT | `/api/templates/{workspaceId}/{templateId}/status`                | 템플릿 상태 수정 | `workspaceId, templateId, IndividualTemplateStatusUpdateRequest` | `IndividualTemplateResponse`    |
| DELETE | `/api/templates/{workspaceId}/{templateId}`                       | 템플릿 삭제          | `workspaceId`, `templateId` | String                            |

### 🌐 **공용 템플릿 API** (`/api/public-templates`)
| Method | Endpoint | Description | Parameters | Response |
|--------|----------|-------------|------------|----------|
| GET | `/api/public-templates` | 공용 템플릿 목록 | `page`, `size`, `keyword`, `sort` | `Page<PublicTemplateResponse>` |
| POST | `/api/public-templates` | 공용 템플릿 생성 | `PublicTemplateCreateRequest` | `PublicTemplateResponse` |

**정렬 옵션**: `latest` (최신순), `popular` (인기순), `mostShared` (공유순)

### ⭐ **즐겨찾기 API**
| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| POST | `/api/individual/favorite` | 개인 템플릿 즐겨찾기 추가 | `FavoriteCreateRequest` | `FavoriteResponse` |
| POST | `/api/public/favorite` | 공용 템플릿 즐겨찾기 추가 | `FavoriteCreateRequest` | `FavoriteResponse` |
| GET | `/api/favorites` | 즐겨찾기 목록 조회 | `workspaceId` | `List<FavoriteResponse>` |
| DELETE | `/api/favorites/{favoriteId}` | 즐겨찾기 삭제 | `favoriteId` | String |

### 📱 **주소록 API** (`/api/workspaces/{workspaceId}/phonebooks`)
| Method | Endpoint | Description | Request                                                                        | Response |
|--------|----------|-------------|--------------------------------------------------------------------------------|----------|
| POST | `/api/workspaces/{workspaceId}/phonebooks` | 주소록 생성 | `PhoneBookRequest.CreateDTO`, `workspaceId`, `jwtClaims` | `PhoneBookResponse.SimpleDTO` |
| GET | `/api/workspaces/{workspaceId}/phonebooks` | 주소록 목록 | `workspaceId`, `jwtClaims` | `List<PhoneBookResponse.SimpleDTO>` |
| GET | `/api/workspaces/{workspaceId}/phonebooks/{phoneBookId}` | 주소록 상세 | `workspaceId`, `phoneBookId`, `pageable`, `jwtClaims` | `Page<RecipientResponse.SimpleDTO>` |
| PUT | `/api/workspaces/{workspaceId}/phonebooks/{phoneBookId}` | 주소록 수정 | `PhoneBookRequest.UpdateDTO`, `workspaceId`, `phoneBookId`, `jwtClaims` | `PhoneBookResponse.SimpleDTO` |
| DELETE | `/api/workspaces/{workspaceId}/phonebooks/{phoneBookId}` | 주소록 삭제 | `workspaceId`, `phoneBookId`, `jwtClaims` | `PhoneBookResponse.SimpleDTO` |
| POST | `/api/workspaces/{workspaceId}/phonebooks/{phoneBookId}/recipients` | 수신자 추가 | `PhoneBookRequest.RecipientIdListDTO`, `workspaceId`, `phoneBookId`, `jwtClaims` | `PhoneBookResponse.ModifiedRecipientsDTO` |
| DELETE | `/api/workspaces/{workspaceId}/phonebooks/{phoneBookId}/recipients` | 수신자 제거 | `PhoneBookRequest.RecipientIdListDTO`, `workspaceId`, `phoneBookId`, `jwtClaims` | `PhoneBookResponse.ModifiedRecipientsDTO` |

### 📇 **수신자 API** (`/api/workspaces/{workspaceId}/recipients`)
| Method | Endpoint | Description | Request                                                           | Response                      |
|--------|----------|-------------|-------------------------------------------------------------------|-------------------------------|
| POST | `/api/workspaces/{workspaceId}/recipients` | 수신자 생성 | `RecipientRequest.CreateDTO`, `workspaceId`, `jwtClaims` | `RecipientResponse.SimpleDTO` |
| GET | `/api/workspaces/{workspaceId}/recipients` | 수신자 목록 | `workspaceId`, `pageable`, `jwtClaims` | `Page<RecipientResponse.SimpleDTO>` |
| PUT | `/api/workspaces/{workspaceId}/recipients/{recipientId}` | 수신자 수정 | `RecipientRequest.UpdateDTO`, `workspaceId`, `recipientId`, `jwtClaims` | `RecipientResponse.SimpleDTO` |
    | DELETE | `/api/workspaces/{workspaceId}/recipients/{recipientId}` | 수신자 삭제 | `workspaceId`, `recipientId`, `jwtClaims` | `RecipientResponse.SimpleDTO` |

### 🤖 **AI API** (Python FastAPI)
| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| POST | `/ai/template` | AI 템플릿 생성 | `TemplateRequest` (사용자 프롬프트) | `TemplateResponse` (생성된 템플릿) |
| POST | `/ai/validate` | 템플릿 검증 | `ValidateRequest` (템플릿 내용) | `ValidateResponse` (APPROVE/REJECT) |

### 👨‍💼 **관리자 API** (`/api/admin`) - ADMIN 권한 필요
| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| DELETE | `/api/admin/public-templates/{publicTemplateId}` | 공용 템플릿 삭제 | `publicTemplateId` | String |

## 🚀 설치 및 실행

### 사전 요구사항
- **Java 21** 이상 (Virtual Threads 지원)
- **Python 3.12** 이상
- **Gradle 8.x** 이상
- **MySQL 8.0** 이상 (AWS RDS 또는 로컬)
- **Redis 7.x** 이상
- **Node.js 22.17.0** 이상
- **npm 10.9.2** 이상
- **Docker & Docker Compose** (배포 시)
- **Git**
- **OpenAI API Key** (RAG 기능 필수)
- **Google API Key** (LangChain 기능 필수)

### 1. 프로젝트 클론
```bash
git clone https://github.com/your-username/Final-2team-DrHong.git
cd Final-2team-DrHong
```

### 2. 환경 변수 설정

`.env.example` 파일을 복사하여 `.env` 파일 생성:
```bash
cp .env.example .env
```

`.env` 파일 내용:
```bash
# AI API 키
OPENAI_API_KEY=sk-your-openai-api-key
GOOGLE_API_KEY=AIyour-google-api-key
HUGGING_FACE_HUB_TOKEN=hf_your-huggingface-token

# 데이터베이스
DB_PASSWORD=your-rds-db-password

# 이메일 (Gmail SMTP)
MAIL_PASSWORD=your-gmail-app-password

# JWT 시크릿 (최소 32자 권장)
JWT_SECRET_KEY=your-super-long-secret-key-at-least-32-characters

# OAuth2 (Google)
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret

# SSH 키 경로 (AWS 접속용)
DRHONG_PEM_KEY_PATH=C:\Users\YourUser\keys\drhong-bastion-key.pem
DRHONG_APP_KEY_PATH=your-drhong-app-key-path

# GPU 설정
IS_GPU_AVAILABLE=FALSE  # GPU 있으면 TRUE

AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-access-key
AWS_DEFAULT_REGION=ap-northeast-2
```

### 3. 로컬 개발 환경 실행

#### 방법 1: 스크립트 사용 (권장)

**Step 1: 디렉토리 초기화 (최초 1회)**
```bash
# Linux/macOS
chmod +x init_dirs.sh
./init_dirs.sh

# Windows
init_dirs.bat
```

**Step 2: Backend 인프라 시작**
```bash
# Linux/macOS
chmod +x start-backend.sh
./start-backend.sh

# Windows PowerShell
.\start-backend.ps1
```

이 스크립트는 다음을 실행합니다:
- SSH 터널로 AWS RDS MySQL 연결 (`localhost:3307`)
- Redis 컨테이너 시작 (`localhost:6379`)
- Python AI 서비스 시작 (`localhost:8000`)

**Step 3: Spring Boot 애플리케이션 실행**
```bash
# Gradle로 실행
./gradlew bootRun

# 또는 IDE에서 Final2teamDrHongApplication.java 실행
```
- Spring Boot API: http://localhost:8080/api/
- Swagger UI: http://localhost:8080/api/swagger-ui.html

**Step 4: Frontend 실행**
```bash
cd frontend
npm install
npm start
```
- React 앱: http://localhost:3000

#### 방법 2: Docker Compose 사용

**개발 환경 (dev)**
```bash
docker-compose -f docker-compose-dev.yml up -d
```
- Spring Boot: http://localhost:8081
- Python AI: http://localhost:8001
- Frontend: http://localhost:3001
- Redis: localhost:6380

**운영 환경 (prod)**
```bash
docker-compose -f docker-compose-prod.yml up -d
```
- Spring Boot: http://localhost:8082
- Python AI: http://localhost:8002
- Frontend: http://localhost:3002
- Redis: localhost:6381

### 4. Docker Compose 서비스 관리

#### 서비스 시작
```bash
# 개발 환경
docker-compose -f docker-compose-dev.yml up -d

# 운영 환경
docker-compose -f docker-compose-prod.yml up -d
```

#### 로그 확인
```bash
# 전체 로그
docker-compose -f docker-compose-dev.yml logs -f

# 특정 서비스 로그
docker-compose -f docker-compose-dev.yml logs -f spring-app
docker-compose -f docker-compose-dev.yml logs -f ai-service
```

#### 서비스 중지
```bash
docker-compose -f docker-compose-dev.yml down
```

#### 서비스 재시작
```bash
docker-compose -f docker-compose-dev.yml restart
```

#### 볼륨 및 네트워크 정리
```bash
docker-compose -f docker-compose-dev.yml down -v
```

## 🗄 데이터베이스 설정

### MySQL 설정 (AWS RDS)

#### RDS 엔드포인트 확인
AWS RDS 콘솔에서 엔드포인트 확인 후 `application.properties`에 설정

#### SSH 터널을 통한 로컬 접속
```bash
# start-backend.sh 스크립트가 자동으로 SSH 터널 생성
# localhost:3307 -> RDS MySQL 3306 포트로 포워딩
```

#### 직접 접속 (로컬 개발)
```bash
mysql -h localhost -P 3307 -u admin -p
# 비밀번호: .env 파일의 DB_PASSWORD
```

### 데이터베이스 초기화

Spring Boot 애플리케이션 실행 시 JPA가 자동으로 테이블 생성:
```properties
# application.properties
spring.jpa.hibernate.ddl-auto=update
```

### 샘플 데이터 생성

#### 관리자 계정 생성
```sql
INSERT INTO users (user_email, user_name, user_role, is_deleted)
VALUES ('admin@notimo.com', '관리자', 'ADMIN', false);

INSERT INTO user_auth (user_id, auth_type, password_hash)
VALUES (1, 'LOCAL', '$2a$10$...');  -- BCrypt 암호화된 비밀번호
```

#### 테스트 사용자 생성
```sql
INSERT INTO users (user_email, user_name, user_role, is_deleted)
VALUES ('test@example.com', '테스트유저', 'USER', false);
```

### H2 Database (테스트 환경)

테스트 시 자동으로 H2 인메모리 DB 사용:
```properties
# application.properties (test profile)
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

## ⚙️ 환경 설정

### Spring Boot 설정 파일

`backend-spring/src/main/resources/application.properties`:

```properties
# 애플리케이션 이름
spring.application.name=notimo

# 서버 포트
server.port=8080

# 데이터베이스 설정 (AWS RDS)
spring.datasource.url=jdbc:mysql://localhost:3307/notimo_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=admin
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA 설정
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

# UTF-8 인코딩
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.force=true

# JWT 설정
jwt.secret=${JWT_SECRET_KEY}
jwt.access-token-expiration=900000        # 15분
jwt.refresh-token-expiration=604800000    # 7일

# 이메일 설정 (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Redis 설정
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Spring Session 설정
spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=always

# OAuth2 설정 (Google)
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8080/api/auth/social/callback/google
spring.security.oauth2.client.registration.google.scope=profile,email

# Swagger 설정
springdoc.api-docs.path=/api/api-docs
springdoc.swagger-ui.path=/api/swagger-ui.html

# Virtual Threads (JDK 21)
spring.threads.virtual.enabled=true

# 비동기 처리
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=20
```

### Rate Limiting 설정 (Bucket4j)

현재 설정된 제한:
- **이메일 발송**: 5분간 3회
- **로그인 시도**: 15분간 5회
- **회원가입**: 1시간간 10회

변경은 `RateLimitService.java`에서 가능:
```java
Bandwidth emailLimit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(5)));
```

## 🚨 트러블슈팅

프로젝트에는 다음 트러블슈팅 문서가 포함되어 있습니다:

### 문서 목록
- [H2 시퀀스 리셋 문제](TROUBLESHOOTING_H2_SEQUENCE_RESET.md)
- [데이터베이스 제약 조건 문제](database-constraint-troubleshooting-report.md)
- [엔티티 리팩토링 이슈](entity-refactoring-troubleshooting-report.md)
- [타임스탬프 동기화 문제](timestamp-sync-troubleshooting-report.md)

### 자주 발생하는 문제

#### 1. SSH 터널 연결 실패
```
Could not establish SSH tunnel
```
**해결방법:**
- PEM 키 파일 경로 확인 (.env의 `DRHONG_PEM_KEY_PATH`)
- PEM 키 파일 권한 확인: `chmod 400 your-key.pem`
- AWS 보안 그룹에서 SSH(22) 포트 허용 확인

#### 2. Redis 연결 실패
```
Unable to connect to Redis
```
**해결방법:**
- Redis 컨테이너 실행 확인: `docker ps | grep redis`
- Redis 포트 확인: `redis-cli -p 6379 ping`
- Docker 재시작: `docker-compose restart redis`

#### 3. AI 서비스 연결 실패
```
Connection refused to AI service
```
**해결방법:**
- Python AI 컨테이너 실행 확인: `docker ps | grep ai-service`
- 로그 확인: `docker logs ai-service`
- 포트 충돌 확인: `netstat -ano | findstr :8000`

#### 4. JWT 토큰 오류
```
Invalid JWT signature
```
**해결방법:**
- `.env` 파일의 `JWT_SECRET_KEY` 확인
- 최소 32자 이상의 안전한 키 사용
- Redis에 저장된 토큰 확인: `redis-cli keys "*refresh*"`

#### 5. 이메일 발송 실패
```
Failed to send email
```
**해결방법:**
- Gmail 앱 비밀번호 확인 (.env의 `MAIL_PASSWORD`)
- Gmail 2단계 인증 활성화 확인
- SMTP 포트 확인 (587)

#### 6. OpenAI API 오류
```
OpenAI API rate limit exceeded
```
**해결방법:**
- API 키 할당량 확인
- Google Gemini로 대체 사용
- 요청 빈도 조절

## 📂 프로젝트 구조

```
Final-2team-DrHong/
├── backend-spring/                       # Spring Boot 백엔드
│   ├── src/main/java/com/jober/final2teamdrhong/
│   │   ├── config/                       # 설정 클래스
│   │   │   ├── SecurityConfig.java       # Spring Security 설정
│   │   │   ├── JwtConfig.java            # JWT 설정
│   │   │   ├── RedisConfig.java          # Redis 설정
│   │   │   ├── AsyncConfig.java          # 비동기 설정
│   │   │   └── SwaggerConfig.java        # Swagger 설정
│   │   ├── controller/                   # REST API 컨트롤러
│   │   │   ├── AuthController.java
│   │   │   ├── SocialAuthController.java
│   │   │   ├── UserController.java
│   │   │   ├── WorkspaceController.java
│   │   │   ├── IndividualTemplateController.java
│   │   │   ├── PublicTemplateController.java
│   │   │   ├── FavoriteController.java
│   │   │   ├── PhoneBookController.java
│   │   │   ├── RecipientController.java
│   │   │   └── AdminController.java
│   │   ├── dto/                          # 데이터 전송 객체
│   │   ├── entity/                       # JPA 엔티티
│   │   │   ├── BaseEntity.java           # 공통 엔티티 (Soft Delete)
│   │   │   ├── User.java
│   │   │   ├── UserAuth.java
│   │   │   ├── Workspace.java
│   │   │   ├── IndividualTemplate.java
│   │   │   ├── PublicTemplate.java
│   │   │   ├── Favorite.java
│   │   │   ├── PhoneBook.java
│   │   │   ├── Recipient.java
│   │   │   ├── GroupMapping.java
│   │   │   ├── TemplateModifiedHistory.java
│   │   │   └── EmailVerification.java
│   │   ├── repository/                   # JPA 리포지토리
│   │   ├── service/                      # 비즈니스 로직
│   │   │   ├── AuthService.java
│   │   │   ├── EmailService.java
│   │   │   ├── UserService.java
│   │   │   ├── WorkspaceService.java
│   │   │   ├── IndividualTemplateService.java
│   │   │   ├── PublicTemplateService.java
│   │   │   ├── FavoriteService.java
│   │   │   ├── PhoneBookService.java
│   │   │   ├── RecipientService.java
│   │   │   ├── RateLimitService.java
│   │   │   └── storage/                  # 인증 저장소
│   │   ├── filter/                       # 보안 필터
│   │   │   └── JwtAuthenticationFilter.java
│   │   ├── exception/                    # 예외 처리
│   │   └── util/                         # 유틸리티
│   └── src/main/resources/
│       └── application.properties        # 설정 파일
├── backend-ai/                           # Python AI 서비스
│   ├── app/
│   │   ├── config/                       # 설정
│   │   ├── core/                         # 핵심 모듈
│   │   │   ├── model_loader.py           # 모델 로더
│   │   │   └── agent_initializer.py      # AI Agent 초기화
│   │   ├── models/                       # AI 모델 저장소
│   │   │   └── finetuned_model/          # 파인튜닝 모델
│   │   ├── routers/                      # API 라우터
│   │   │   ├── template_router.py        # 템플릿 생성 API
│   │   │   └── validate_router.py        # 검증 API
│   │   ├── schemas/                      # Pydantic 스키마
│   │   ├── services/                     # 비즈니스 로직
│   │   │   ├── gen_service.py            # 템플릿 생성
│   │   │   ├── cls_service.py            # 템플릿 분류
│   │   │   └── guidelines_service.py     # 가이드라인 관리
│   │   └── utils/                        # 유틸리티
│   ├── template_guide_db/                # 벡터 DB (ChromaDB)
│   ├── main.py                           # FastAPI 진입점
│   ├── requirements.txt                  # Python 의존성
│   └── Dockerfile                        # AI 서비스 Docker
├── frontend/                             # React 프론트엔드
│   ├── src/
│   │   ├── pages/                        # 페이지 컴포넌트
│   │   │   ├── Login.js
│   │   │   ├── SignUp.js
│   │   │   ├── SocialLoginCallback.js
│   │   │   ├── WorkspaceSelection.js
│   │   │   ├── MyTemplatePage.js
│   │   │   ├── PublicTemplatePage.js
│   │   │   ├── TemplateGeneratorPage.js
│   │   │   └── ContactManagementPage.js
│   │   ├── components/                   # 재사용 컴포넌트
│   │   ├── services/                     # API 서비스
│   │   ├── router.js                     # 라우팅
│   │   └── App.js                        # 앱 진입점
│   ├── public/                           # 정적 파일
│   ├── package.json                      # npm 의존성
│   ├── Dockerfile                        # Frontend Docker
│   └── nginx.conf                        # Nginx 설정
├── scripts/                              # 운영 스크립트
│   ├── start-backend.sh                  # Backend 시작 (Linux/Mac)
│   ├── start-backend.ps1                 # Backend 시작 (Windows)
│   ├── init_dirs.sh                      # 디렉토리 초기화 (Linux/Mac)
│   └── init_dirs.bat                     # 디렉토리 초기화 (Windows)
├── docker-compose.yml                    # Docker Compose (기본)
├── docker-compose-dev.yml                # Docker Compose (개발)
├── docker-compose-prod.yml               # Docker Compose (운영)
├── build.gradle                          # Gradle 빌드 설정
├── .env.example                          # 환경 변수 예시
├── .gitignore                            # Git 제외 파일
└── README.md                             # 프로젝트 문서
```

## 🔧 성능 최적화

### Virtual Threads (JDK 21 Project Loom)
```properties
spring.threads.virtual.enabled=true
```
- 경량 스레드로 동시성 처리 성능 향상
- 블로킹 I/O 작업 효율화

### OSIV 비활성화
```properties
spring.jpa.open-in-view=false
```
- 데이터베이스 커넥션 리소스 절약
- 트랜잭션 범위 명확화

### 비동기 처리
```java
@Async
public CompletableFuture<IndividualTemplateResponse> createBlankTemplateAsync(...)
```
- 템플릿 생성 등 시간이 오래 걸리는 작업 비동기 처리

### Redis 캐싱
- JWT 토큰 저장
- 이메일 인증 코드 저장 (5분 TTL)
- Rate Limiting 카운터

### 페이징
```java
@GetMapping
public Page<RecipientResponse> getRecipients(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
)
```
- 대용량 데이터 효율적 처리

### AI 모델 최적화
- **PEFT (Parameter-Efficient Fine-Tuning)**: 효율적인 모델 파인튜닝
- **BitsAndBytes**: 모델 양자화로 메모리 사용량 감소
- **ChromaDB**: 벡터 검색으로 빠른 가이드라인 조회

## 🚀 배포 정보

### 개발 환경
- **Java**: OpenJDK 21
- **Spring Boot**: 3.5.5
- **Python**: 3.12
- **Node.js**: 22.17.0
- **MySQL**: 8.0 (AWS RDS)
- **Redis**: 7.x
- **Port**: 8080 (Spring), 8000 (AI), 3000 (React)

### 운영 환경 (Docker Compose)
```bash
# 운영 환경 배포
docker-compose -f docker-compose-prod.yml up -d

# 서비스 URL
# Spring Boot: http://localhost:8082
# Python AI: http://localhost:8002
# Frontend: http://localhost:3002
```

### 보안 체크리스트 (운영 환경)
- ✅ `.env` 파일 보안 강화 (Git에 커밋 금지)
- ✅ JWT Secret Key 강력한 키로 변경
- ✅ Database 비밀번호 변경
- ✅ OAuth2 Redirect URI 운영 도메인으로 변경
- ✅ CORS 허용 Origin 제한
- ✅ HTTPS 적용
- ✅ Rate Limiting 설정 검토
- ✅ 로그 레벨 조정 (INFO 이상)
- ✅ 에러 메시지 최소화

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 👥 팀 정보

**Final 2팀 - DrHong**
- 프로젝트 기간: 2025/08~2025/10
- 팀원: 
  - [홍성훈](https://github.com/callmehoon) Seonghoon Hong
  - [이강현](https://github.com/bill291104) Ganghyun Lee
  - [이상수](https://github.com/constant0841) Sangsu Lee
  - [나영문](https://github.com/ymn-7584) Youngmun Na
  - [이주열](https://github.com/TakeJanus) Jooyeol Lee
  - [최상민](https://github.com/ddddaq) Sangmin Choi
  - [방혜준](https://github.com/june-ve) Hyejune Bang
  - [박서원](https://github.com/Seowon-Park) Seowon Park

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 📚 참고 자료

### API 문서
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs**: http://localhost:8080/api/api-docs

### 기술 문서
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [React Documentation](https://react.dev/)
- [Material-UI Documentation](https://mui.com/)
- [LangChain Documentation](https://python.langchain.com/)

### AI 관련
- [OpenAI API Reference](https://platform.openai.com/docs/api-reference)
- [Google Gemini API](https://ai.google.dev/)
- [ChromaDB Documentation](https://docs.trychroma.com/)
