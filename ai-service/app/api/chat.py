from fastapi import APIRouter
from app.schemas.chat import ChatRequest, ChatResponse
from app.services.recommend_service import get_welcome_message, handle_chat

router = APIRouter(prefix="/chat", tags=["chat"])

@router.get("/welcome", response_model=ChatResponse)
async def welcome():
    return get_welcome_message()

@router.post("", response_model=ChatResponse)
async def chat(request: ChatRequest):
    return handle_chat(request.question)