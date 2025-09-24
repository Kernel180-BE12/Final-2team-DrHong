# backend-ai/Dockerfile
FROM python:3.11-slim

WORKDIR /app

# Poetry 설치
RUN pip install poetry

# Poetry 설정 파일 복사
COPY pyproject.toml poetry.lock poetry.toml ./

# Poetry로 의존성 설치 (가상환경 생성 안함)
RUN poetry config virtualenvs.create false && poetry install --only=main

# 소스 코드 복사
COPY backend-ai/ .

# FastAPI 서버 실행
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]