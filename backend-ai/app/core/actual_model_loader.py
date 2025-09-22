from pathlib import Path
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig, AutoModelForSequenceClassification
from peft import PeftModel
import torch
from huggingface_hub import snapshot_download
from huggingface_hub.utils import RepositoryNotFoundError
import gc

import logging
log = logging.getLogger("uvicorn")

BASE_DIR = Path(__file__).resolve().parent.parent.parent

class ModelLoader:
    GEN_MODEL_ID = "MLP-KTLim/llama-3-Korean-Bllossom-8B"
    CLS_MODEL_ID = "klue/bert-base"

    # 경로를 절대 경로로 재정의
    DOWNLOADED_PATH = BASE_DIR / "app/models/downloaded_model/"
    FINETUNED_PATH = BASE_DIR / "app/models/finetuned_model/"

    GEN_MODEL_PATH = DOWNLOADED_PATH / GEN_MODEL_ID.replace("/", "--")
    CLS_DOWNLOADED_PATH = DOWNLOADED_PATH / CLS_MODEL_ID.replace("/", "--")
    CLS_FINETUNED_PATH = FINETUNED_PATH / CLS_MODEL_ID.replace("/", "--")

    models = {}

    @classmethod
    def download_gen_model(cls):
        cls.download_model_from_hub(cls.GEN_MODEL_ID, cls.DOWNLOADED_PATH + cls.GEN_MODEL_PATH)
        return

    @classmethod
    def download_cls_model(cls):
        cls.download_model_from_hub(cls.CLS_MODEL_ID, cls.DOWNLOADED_PATH + cls.CLS_MODEL_PATH)
        return

    @classmethod
    def download_model_from_hub(cls, model_id, local_dir):
        log.info(f"'{model_id}' 모델을 '{local_dir}' 경로에 다운로드합니다...")
        try:
            # snapshot_download는 알아서 기존 파일을 체크하고 필요한 것만 다운로드합니다.
            model_path = snapshot_download(
                repo_id=model_id,
                local_dir=local_dir
                # resume_download=True, # 기본값이 True이므로 명시하지 않아도 됨
            )
            log.info("✅ 모델 준비 완료!")
            return model_path
        except RepositoryNotFoundError as e:
            log.error(f"❌ 오류: 모델 ID '{model_id}'를 찾을 수 없습니다.")
            raise e
        except Exception as e:
            log.error(f"❌ 다운로드 중 오류가 발생했습니다: {e}")
            raise e

    @classmethod
    def load_gen_model(cls):
        """서버 구동시에 호출할 템플릿 생성 모델을 메모리로 로드하는 함수"""

        #cls.download_gen_model()

        if "gen" not in cls.models or cls.models["gen"] is None:
            base_model_path = cls.GEN_MODEL_PATH
            lora_adapter_path = cls.FINETUNED_PATH / cls.GEN_MODEL_ID.replace("/", "--")

            bnb_config = BitsAndBytesConfig(
                load_in_8bit=True,
                llm_int8_threshold=6.0
            )

            tokenizer = AutoTokenizer.from_pretrained(base_model_path)
            base_model = AutoModelForCausalLM.from_pretrained(
                base_model_path,
                quantization_config=bnb_config,
                device_map="auto",
                low_cpu_mem_usage=True
            )
            model = PeftModel.from_pretrained(base_model, lora_adapter_path)

            cls.models["gen"] = model, tokenizer
            log.info("생성 모델 로드 완료")
            cls.check_gpu_memory_usage()
            return cls.models
        else:
            log.warning("생성 모델이 이미 로드 되어 있습니다")
            cls.check_gpu_memory_usage()
            return cls.models

    @classmethod
    def unload_gen_model(cls):
        """생성 모델 메모리에서 정리"""
        cls.check_gpu_memory_usage()

        gen_model, gen_tokenizer = cls.models.pop("gen")
        del gen_model
        del gen_tokenizer
        torch.cuda.empty_cache()
        gc.collect()

        cls.check_gpu_memory_usage()
        return

    @classmethod
    def load_cls_model(cls):
        """서버 구동시에 호출할 템플릿 검증 모델을 메모리로 로드하는 함수"""

        #cls.download_cls_model()

        if "cls" not in cls.models or cls.models["cls"] is None:
            base_model_path = cls.CLS_DOWNLOADED_PATH
            finetuned_model_path = cls.CLS_FINETUNED_PATH

            tokenizer = AutoTokenizer.from_pretrained(base_model_path)
            model = AutoModelForSequenceClassification.from_pretrained(finetuned_model_path)
            model.eval()

            cls.models["cls"] = model, tokenizer
            log.info("분류 모델 로드 완료")
            cls.check_gpu_memory_usage()
            return cls.models
        else:
            log.warning("분류 모델이 이미 로드 되어 있습니다")
            return cls.models

    @classmethod
    def unload_cls_model(cls):
        """메모리 정리"""
        cls.check_gpu_memory_usage()

        cls_model, cls_tokenizer = cls.models.pop("cls")
        del cls_model
        del cls_tokenizer
        torch.cuda.empty_cache()
        gc.collect()

        cls.check_gpu_memory_usage()
        return

    @staticmethod
    def check_gpu_memory_usage():
        # GPU가 사용 가능한지 확인
        if torch.cuda.is_available():
            # 현재 사용 중인 메모리 (바이트)
            allocated_bytes = torch.cuda.memory_allocated(device=0)
            # 캐시된 메모리 (바이트)
            reserved_bytes = torch.cuda.memory_reserved(device=0)

            # GB 단위로 변환
            gb_factor = 1024 * 1024 * 1024
            allocated_gb = allocated_bytes / gb_factor
            reserved_gb = reserved_bytes / gb_factor

            log.info(f"현재 사용 중인 GPU 메모리: {allocated_gb:.2f} GB")
            log.info(f"현재 캐시된 GPU 메모리: {reserved_gb:.2f} GB")
        else:
            log.error("GPU를 사용할 수 없습니다.")
