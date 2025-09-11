from fastapi import FastAPI
from routers.template_router import template_router
from routers.validate_router import validate_router
from core.model_loader import *
import uvicorn
from contextlib import asynccontextmanager

models = {}

@asynccontextmanager
async def lifespan(app: FastAPI):
    # TODO Load model to memory
    gen_model = load_gen_model()
    cls_model = load_cls_model()
    models["gen_model"] = gen_model
    models["cls_model"] = cls_model
    yield
    # TODO Unload model from memory
    unload_gen_model()
    unload_cls_model()

app = FastAPI(lifespan=lifespan)
app.include_router(template_router)
app.include_router(validate_router)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)
