from ..main import models
from ..schemas.template import TemplateRequest, TemplateResponse

def gen_template(request: TemplateRequest) -> TemplateResponse:
    # TODO 템플릿 생성 함수 구현
    pass
    gen_model = models.get("gen_model")
    return TemplateResponse()