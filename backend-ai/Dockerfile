# 1. 베이스 이미지 (Python 3.12 포함)
FROM public.ecr.aws/tridge/pytorch-inference:2.6.0-gpu-py312-cu124-ubuntu22.04-ec2

# 2. Poetry 설치 (시스템의 기본 python3를 사용)
ENV POETRY_VERSION=1.8.2
RUN curl -sSL https://install.python-poetry.org | python3 -
ENV PATH="/root/.local/bin:$PATH"

# 3. 작업 디렉토리 설정
WORKDIR /app

# 4. pyproject.toml 및 poetry.lock 파일 복사
# (로컬에서 Python 3.12 기준으로 새로 생성한 lock 파일을 사용)
COPY pyproject.toml poetry.lock ./

# 5. 의존성 설치 (베이스 이미지의 기본 Python 3.12를 사용)
RUN poetry install --only main --no-interaction --no-ansi

# 6. 애플리케이션 코드 복사
COPY ./app ./app

# 7. 컨테이너 시작 명령 (uvicorn으로 변경)
CMD ["poetry", "run", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
