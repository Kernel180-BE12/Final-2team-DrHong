from ..main import models
from ..schemas.validate import ValidateRequest, ValidateResponse

def cls_template(request: ValidateRequest) -> ValidateResponse:
    # TODO 템플릿 승인/반려 분류 함수 구현
    pass
    cls_model = models.get("cls_model")
    return ValidateResponse()