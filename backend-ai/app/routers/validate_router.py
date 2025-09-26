from fastapi import APIRouter
from ..services.cls_service import cls_template
from ..schemas.validate import ValidateRequest, ValidateResponse

validate_router = APIRouter(tags=["AI classifies a template as approve or reject"])

@validate_router.post("/validate")
async def validate_template(request: ValidateRequest) -> ValidateResponse:
    # 요청을 통해 템플릿 검증 service 함수를 호출하고 결과를 응답
    response = await cls_template(request)
    return response