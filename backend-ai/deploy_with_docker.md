네, 알겠습니다. 그동안의 모든 과정을 종합하여, 다른 개발자가 EC2에 Docker를 사용해 서버를 배포할 수 있도록 단계별 가이드를 정리해 드리겠습니다.

-----

## **EC2 Docker 배포 가이드 (AI 추론 서버)**

이 문서는 AI 모델 추론 서버를 AWS EC2 인스턴스에 Docker를 사용하여 배포하는 과정을 안내합니다.

### **1. 사전 준비사항**

1.  **AWS 계정:** EC2 인스턴스 생성 및 IAM 역할 설정 권한이 있는 AWS 계정.
2.  **GitHub 접근:** 아래 두 리포지토리에 접근 가능한 GitHub 계정 및 Personal Access Token(PAT).
      * `Final-2team-DrHong` (서버 애플리케이션)
      * `dr-hong-finetuning` (파인튜닝된 모델)
3.  **`.env` 파일:** `GOOGLE_API_KEY` 등 필요한 환경 변수가 담긴 `.env` 파일의 내용.

-----

### **2. EC2 인스턴스 생성 및 설정**

1.  **인스턴스 시작:**

      * **AMI:** 최신 `AWS Deep Learning AMI (Amazon Linux 2023)`를 선택합니다. (예: `Deep Learning OSS Nvidia Driver AMI GPU ...`)
      * **인스턴스 타입:** `g5.xlarge` 또는 동급의 GPU 인스턴스를 선택합니다.
      * **키 페어:** SSH 접속을 위한 키 페어를 생성하고 `.pem` 파일을 다운로드합니다.
      * **보안 그룹 (방화벽):** 인바운드 규칙을 다음과 같이 설정합니다.
          * **유형:** `SSH`, **포트:** `22`, **소스:** `내 IP`
          * **유형:** `사용자 지정 TCP`, **포트:** `8000`, **소스:** `위치 무관(0.0.0.0/0)` (테스트용)

2.  **초기 서버 설정:**

      * 생성된 인스턴스에 SSH로 접속합니다.
        ```bash
        ssh -i "your-key.pem" ec2-user@<인스턴스_퍼블릭_IP>
        ```
      * 필수 도구(Git, Git LFS, Docker)를 설치합니다.
        ```bash
        sudo yum update -y
        sudo yum install -y git git-lfs docker
        ```
      * Docker를 실행하고, `ec2-user`가 `sudo` 없이 Docker를 사용하도록 권한을 설정합니다.
        ```bash
        sudo systemctl start docker
        sudo systemctl enable docker
        sudo usermod -aG docker ec2-user
        ```
      * **터미널을 `exit`로 나갔다가 다시 접속하여** 그룹 설정을 적용합니다.

-----

### **3. 프로젝트 및 모델 파일 준비**

1.  **리포지토리 클론:** GitHub PAT를 사용하여 두 리포지토리를 클론합니다.

    ```bash
    git clone https://<YOUR_PAT>@github.com/Kernel180-BE12/Final-2team-DrHong.git
    git clone https://<YOUR_PAT>@github.com/bill291104/dr-hong-finetuning.git
    ```

2.  **모델 파일 준비:**

      * `git lfs`로 실제 모델 파일을 다운로드합니다.
        ```bash
        cd dr-hong-finetuning/
        git lfs pull
        cd ..
        ```
      * 서버 프로젝트에 모델을 저장할 폴더를 만들고, 파인튜닝된 모델을 복사합니다.
        ```bash
        mkdir -p ./Final-2team-DrHong/backend-ai/app/models/
        cp -r ./dr-hong-finetuning/finetuned_model/* ./Final-2team-DrHong/backend-ai/app/models/
        ```
      * 베이스 모델 다운로드 스크립트를 실행합니다.
        ```bash
        cd ./Final-2team-DrHong/backend-ai/app/
        pip3 install huggingface_hub
        python3 download_models.py
        ```

3.  **`.env` 파일 생성:**

      * Docker 빌드를 실행할 폴더로 이동합니다.
        ```bash
        cd /home/ec2-user/Final-2team-DrHong/backend-ai/
        ```
      * `nano .env` 명령어로 `.env` 파일을 생성하고, 준비된 키 값을 붙여넣습니다.
        ```
        GOOGLE_API_KEY="AIzaSy..."
        ```

-----

### **4. Docker 이미지 빌드 및 컨테이너 실행**

1.  **Docker 이미지 빌드:** (`backend-ai` 폴더에서 실행)
    ```bash
    docker build -t my-inference-server .
    ```
2.  **Docker 컨테이너 실행:**
    ```bash
    docker run -d \
      --name my-server \
      --restart always \
      -p 8000:8000 \
      --gpus all \
      -v $(pwd)/app/models:/app/models \
      --env-file .env \
      my-inference-server
    ```
      * `--restart always`: 서버가 재부팅되거나 컨테이너가 멈췄을 때 자동으로 다시 시작하는 옵션입니다.
      * `--env-file .env`: `.env` 파일의 모든 변수를 컨테이너에 안전하게 주입합니다.

-----

### **5. 배포 확인**

1.  **컨테이너 실행 상태 확인:**

    ```bash
    docker ps
    ```

2.  **서버 로그 확인:**

    ```bash
    docker logs my-server
    ```

    로그에서 `Uvicorn running...` 메시지를 확인합니다.

3.  **API 테스트:** `curl` 명령어를 사용하여 서버가 정상적으로 응답하는지 확인합니다.

    ```bash
    curl -X POST "http://127.0.0.1:8000/template/template" -H "Content-Type: application/json" -d '{...}'
    ```