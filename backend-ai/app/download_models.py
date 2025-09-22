import os
import logging
from huggingface_hub import snapshot_download
from huggingface_hub.utils import RepositoryNotFoundError

# 실행 과정을 확인할 수 있도록 간단한 로거 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
log = logging.getLogger(__name__)

# ModelLoader 클래스에서 사용하는 설정값과 동일하게 구성
GEN_MODEL_ID = "MLP-KTLim/llama-3-Korean-Bllossom-8B"
CLS_MODEL_ID = "klue/bert-base"

# ModelLoader의 상대 경로("../")를 현재 경로("./") 기준으로 변경
# 이 스크립트를 실행하면 ./models/downloaded_model/ 경로가 생성됩니다.
DOWNLOADED_PATH = "./models/downloaded_model/"


def download_model_from_hub(model_id: str, base_path: str):
    """지정된 모델을 Hugging Face Hub에서 다운로드합니다."""

    # ModelLoader의 경로 생성 방식과 동일하게 '/'를 '--'로 변경
    model_folder_name = model_id.replace("/", "--")
    local_dir = os.path.join(base_path, model_folder_name)

    log.info(f"'{model_id}' 모델을 '{local_dir}' 경로에 다운로드합니다...")

    # 다운로드 전에 폴더가 존재하지 않으면 생성
    os.makedirs(local_dir, exist_ok=True)

    try:
        # snapshot_download는 이미 파일이 있으면 건너뛰므로 안전하게 여러 번 실행 가능
        snapshot_download(
            repo_id=model_id,
            local_dir=local_dir
        )
        log.info(f"✅ '{model_id}' 모델 준비 완료!")
    except RepositoryNotFoundError:
        log.error(f"❌ 오류: 모델 ID '{model_id}'를 찾을 수 없습니다. ID를 확인해주세요.")
    except Exception as e:
        log.error(f"❌ '{model_id}' 다운로드 중 예상치 못한 오류 발생: {e}")


if __name__ == "__main__":
    log.info("===== 모델 다운로드를 시작합니다 =====")

    # 1. 생성 모델(베이스) 다운로드
    download_model_from_hub(GEN_MODEL_ID, DOWNLOADED_PATH)

    # 2. 분류 모델(베이스) 다운로드
    download_model_from_hub(CLS_MODEL_ID, DOWNLOADED_PATH)

    log.info("🎉 모든 베이스 모델 다운로드가 완료되었습니다.")
    log.info("이제 Docker 컨테이너를 빌드하고 실행할 수 있습니다.")