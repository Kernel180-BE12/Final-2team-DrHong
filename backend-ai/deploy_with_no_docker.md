네, 알겠습니다. Docker를 사용하지 않고 EC2 인스턴스에 직접 서버를 배포하고 실행했던 전체 과정을 단계별로 정리해 드리겠습니다.

-----

## \#\# EC2 직접 배포 전체 과정 요약

### 1단계: 초기 서버 및 코드 준비

1.  **EC2 인스턴스 접속**
    ```bash
    ssh -i [내 .pem 키] ec2-user@[인스턴스 IP]
    ```
2.  **필수 패키지 설치**
    ```bash
    yum install -y git git-lfs
    ```
3.  **프로젝트 리포지토리 클론**
    ```bash
    # 모델 파일이 있는 리포지토리
    git clone [파인튜닝 리포지토리 주소]

    # 서버 코드가 있는 리포지토리
    git clone -b [사용할 브랜치] [서버 리포지토리 주소]
    ```

-----

### \#\# 2단계: Python 및 Poetry 환경 설정

1.  **Python 3.11 및 개발 도구 설치**
    ```bash
    yum install -y python3.11 python3.11-devel
    ```
2.  **Poetry 설치**
    ```bash
    curl -sSL https://install.python-poetry.org | python3 -
    ```
3.  **Poetry 명령어 경로 설정** (터미널 재접속 또는 아래 명령어 실행)
    ```bash
    source "$HOME/.local/env"
    ```

-----

### \#\# 3단계: 모델 파일 준비

1.  **실제 모델 파일 다운로드 (Git LFS)**
    ```bash
    # 모델 파일이 있는 리포지토리로 이동
    cd [파인튜닝 리포지토리 이름]/

    # LFS 파일 다운로드
    git lfs pull
    ```
2.  **서버 프로젝트로 모델 파일 복사**
    ```bash
    # 예시 경로
    cp -r ./finetuned_model/* /root/Final-2team-DrHong/backend-ai/app/models/finetuned_model/
    ```
3.  **베이스 모델 다운로드 스크립트 실행**
    ```bash
    # AI 서버 프로젝트의 app 폴더로 이동
    cd /root/Final-2team-DrHong/backend-ai/app/

    # 스크립트 실행에 필요한 라이브러리 설치
    python3 -m pip install huggingface_hub

    # 다운로드 스크립트 실행
    python3 download_models.py
    ```

-----

### \#\# 4단계: 의존성 라이브러리 설치

1.  **Poetry가 사용할 Python 버전 지정**
    ```bash
    # AI 서버 프로젝트의 backend-ai 폴더로 이동
    cd /root/Final-2team-DrHong/backend-ai/

    # Poetry에 Python 3.11 사용하도록 설정
    poetry env use python3.11
    ```
2.  **`pyproject.toml` 파일 수정**
      * `nano pyproject.toml`로 파일을 열어 아래 내용이 올바르게 수정되었는지 확인합니다.
          * `python = ">=3.11,<3.12"`
          * PyTorch 소스 추가 (`[[tool.poetry.source]] ...`)
          * `torch` 버전을 실제로 존재하는 버전으로 수정 (`torch = {version = "~2.5.1", source = "pytorch"}`)
3.  **까다로운 패키지 선행 설치**
      * **`torch` 먼저 설치:**
        ```bash
        poetry run pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121
        ```
      * **`flash-attn` 빌드 격리 없이 설치:**
        ```bash
        poetry run pip install flash-attn --no-build-isolation
        ```
4.  **나머지 의존성 최종 설치**
      * 기존 `lock` 파일이 문제를 일으킬 수 있으므로 삭제 후 새로 생성하며 설치합니다.
    <!-- end list -->
    ```bash
    rm poetry.lock
    poetry lock
    poetry install --only main
    ```

-----

### \#\# 5단계: 서버 실행

1.  **CUDA 라이브러리 경로 설정**
    ```bash
    export LD_LIBRARY_PATH=/usr/local/cuda-12.9/extras/CUPTI/lib64:$LD_LIBRARY_PATH
    ```
2.  **.env 파일 생성**
    ```bash
    nano .env
    ```
    `GOOGLE_API_KEY="실제_API_키"` 와 같은 내용을 추가하고 저장합니다.
3.  **Uvicorn 서버 실행**
    ```bash
    poetry run uvicorn app.main:app --host 0.0.0.0 --port 8000
    ```

이 과정을 모두 마치면 Docker 없이 EC2 인스턴스에서 직접 서버를 구동할 수 있습니다.