# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is "Final-2team-DrHong" - a dual-backend system implementing a notification/template management application ("notimo") with Spring Boot backend and Python AI backend.

### Architecture

**Hybrid Backend Architecture:**
- **Spring Boot Backend** (`backend-spring/`): Main application server handling user management, authentication, templates, and business logic
- **Python AI Backend** (`backend-ai/`): FastAPI-based AI services using OpenAI, LangChain, and vector databases for chat/AI features
- **Database**: MySQL (production) / H2 (testing) with Redis for caching and rate limiting
- **Authentication**: JWT-based stateless authentication with email verification

**Key Technologies:**
- Spring Boot 3.5.5 with Java 21 (virtual threads enabled)
- Spring Security, Spring Data JPA, Spring Session JDBC
- FastAPI with OpenAI, LangChain, ChromaDB
- MySQL/H2, Redis, JWT authentication
- Email service with Gmail SMTP
- Rate limiting with Bucket4j
- Swagger/OpenAPI documentation

## Development Commands

### Building and Running

**Spring Backend:**
```bash
# Build and run Spring Boot application
./gradlew build
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.jober.final2teamdrhong.service.UserServiceTest"

# Run tests with specific pattern
./gradlew test --tests "*UserService*"
```

**Python AI Backend:**
```bash
# Install dependencies
cd backend-ai
pip install -r requirements.txt

# Run FastAPI server (development)
cd backend-ai
uvicorn app.main:app --reload --port 8001
```

**Database Setup:**
```bash
# Start SSH tunnel to RDS (if using production DB)
./start-tunnel.sh    # macOS/Linux
# or
start-tunnel.ps1     # Windows

# The tunnel connects to RDS MySQL through bastion host
```

## Project Structure

### Spring Boot Backend (`backend-spring/src/main/java/com/jober/final2teamdrhong/`)

**Core Packages:**
- `config/` - Security, Redis, JWT, Rate Limiting, OpenAPI configurations
- `controller/` - REST API endpoints
- `service/` - Business logic layer
- `entity/` - JPA entities (14 entities including User, Template, ChatSession, etc.)
- `repository/` - Data access layer
- `dto/` - Data transfer objects
- `exception/` - Custom exceptions and global error handling
- `util/` - Utility classes

**Key Configuration Classes:**
- `SecurityConfig`: JWT stateless auth, CORS, endpoint security
- `JwtConfig`: JWT token generation/validation
- `RedisConfig`: Redis connection and caching
- `RateLimitConfig`: API rate limiting with Bucket4j
- `OpenApiConfig`: Swagger/OpenAPI documentation

### Python AI Backend (`backend-ai/`)

**Structure:**
- `app/main.py` - FastAPI application entry point
- `app/routers/` - API route handlers
- `app/services/` - AI/ML business logic
- `app/models/` - Data models
- `app/schemas/` - Pydantic schemas
- `app/config/` - Configuration management
- `app/utils/` - Utility functions

**Key Dependencies:**
- OpenAI API integration
- LangChain framework for LLM applications
- ChromaDB for vector storage
- Sentence transformers for embeddings
- MySQL connector for database access

## Environment Setup

**Required Environment Variables (.env):**
```bash
# AI Services
OPENAI_API_KEY=sk-...
HUGGING_FACE_HUB_TOKEN=hf_...

# Database
DB_PASSWORD=your_rds_password

# Email Service
MAIL_PASSWORD=your_gmail_app_password

# JWT Security
JWT_SECRET_KEY=your-super-long-secure-key

# SSH Tunnel (for RDS access)
DRHONG_PEM_KEY_PATH=/path/to/your/key.pem
```

## Database Configuration

**Development:** H2 in-memory database (current default)
**Production:** MySQL via RDS with SSH tunnel through bastion host

**Connection Details:**
- Local H2: `jdbc:h2:mem:testdb`
- RDS MySQL: `jdbc:mysql://localhost:3307/notimo` (through SSH tunnel)
- Redis: `localhost:6379`

## Key Features & APIs

**Authentication Flow:**
1. Email verification code sending (`/api/auth/send-verification-code`)
2. User signup with verification (`/api/auth/signup`)
3. JWT token-based authentication for all protected endpoints

**Rate Limiting:**
- Email verification: 3 requests per 5 minutes per IP
- Email validation: 5 requests per 10 minutes per email
- Signup: 10 requests per hour per IP

**API Documentation:**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Testing Strategy

**Spring Backend Testing:**
- Unit tests for services and utilities
- Integration tests for repositories and authentication
- Security tests for user services
- DTO validation tests
- Global exception handling tests

**Test Configuration:**
- Uses H2 in-memory database for tests
- TestVerificationConfig for email verification in tests
- Spring Security Test for authentication testing

## Development Notes

- **Virtual Threads**: Enabled in Spring Boot for improved performance
- **OSIV**: Disabled (`spring.jpa.open-in-view=false`) for better resource management
- **Encoding**: UTF-8 enforced throughout the application
- **Session Management**: Stateless JWT-based authentication
- **Docker**: Basic Dockerfiles present but not fully configured
- **CORS**: Configured to allow all origins in development

## Deployment Considerations

- SSH tunnel required for RDS database access in development
- Environment variables must be properly configured
- Redis server required for rate limiting and caching
- Gmail SMTP configured for email verification
- JWT secret key must be secure and consistent across instances