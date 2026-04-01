from pydantic import BaseModel
from typing import List, Optional, Union
from enum import Enum


class ChatRequest(BaseModel):
    # 사용자가 입력하는 질문
    question: str


class QuestionType(str, Enum):
    # 질문 의도 분류값
    recommendation = "recommendation"   # AI 도구/스킬 추천 요청
    information = "information"         # 개념/사용법/설명 요청
    follow_up = "follow_up"             # 이전 응답 기반 후속 질문
    out_of_scope = "out_of_scope"       # 서비스 범위를 벗어난 질문
    ambiguous = "ambiguous"             # 의도가 모호한 질문


class TargetType(str, Enum):
    # 실제 추천/설명 대상
    ai_model = "ai_model"
    skill = "skill"
    ai_information = "ai_information"
    none = "none"


class Card(BaseModel):
    # 추천 카드 1개
    id: Union[int, str]
    title: str
    owner: Optional[str] = None
    star: Optional[int] = None
    description: str
    uploadedAt: Optional[str] = None
    like: Optional[int] = None
    reason: str
    score: Optional[float] = None
    type: Optional[str] = None


class TopPick(BaseModel):
    # 가장 적합한 추천 1개
    id: Union[int, str]
    title: str
    description: str
    reason: str
    score: Optional[float] = None
    howToUse: str


class ChatResponse(BaseModel):
    # 공통 정상 응답 구조
    question: str
    questionType: QuestionType
    targetType: TargetType
    message: str
    cards: List[Card]
    topPick: Optional[TopPick] = None
    nextActions: List[str]
    outOfScope: bool


class ErrorResponse(BaseModel):
    # 에러 응답 구조
    errorCode: str
    message: str