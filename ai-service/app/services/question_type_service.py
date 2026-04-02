from app.core.prompt import (
    build_question_category_prompt,
    build_recommendation_target_prompt,
    build_skill_intent_prompt
)
from app.services.gemini_service import call_gemini


# =========================
# 1. 유효 값 정의
# =========================
VALID_QUESTION_TYPES = {
    "recommendation",
    "information",
    "follow_up",
    "out_of_scope",
    "ambiguous",
}

VALID_TARGET_TYPES = {
    "ai_model",
    "skill",
    "ai_information",
    "none",
}


# =========================
# 2. 공통 유틸
# =========================
def safe_to_text(value) -> str:
    if isinstance(value, list):
        value = value[0] if value else ""
    if value is None:
        return ""
    return str(value).strip()


def normalize_question_type(text) -> str:
    text = safe_to_text(text).lower()

    for t in VALID_QUESTION_TYPES:
        if t in text:
            return t

    return "ambiguous"


def normalize_target_type(text) -> str:
    text = safe_to_text(text).lower()

    for t in VALID_TARGET_TYPES:
        if t in text:
            return t

    return "none"


# =========================
# 3. 메인 분류 함수
# =========================
def classify_question(question: str) -> dict:
    try:
        # 1. 질문 유형
        question_category_raw = call_gemini(
            build_question_category_prompt(question)
        )
        question_category = normalize_question_type(question_category_raw)

        # 2. 추천 대상
        target_raw = call_gemini(
            build_recommendation_target_prompt(question)
        )
        recommendation_target = normalize_target_type(target_raw)

        # 3. 예외 처리
        if question_category in {"follow_up", "out_of_scope", "ambiguous"}:
            recommendation_target = "none"

        return {
            "questionType": question_category,
            "targetType": recommendation_target,
        }

    except Exception as e:
        print("classify_question error:", e)

        # 최소 fallback
        return {
            "questionType": "ambiguous",
            "targetType": "none",
        }
    

def classify_question_category(question: str) -> str:
    prompt = build_question_category_prompt(question)
    result = call_gemini(prompt).strip()
    return result


def classify_recommendation_target(question: str) -> str:
    prompt = build_recommendation_target_prompt(question)
    result = call_gemini(prompt).strip()
    return result


def classify_skill_intent(question: str) -> str:
    prompt = build_skill_intent_prompt(question)
    result = call_gemini(prompt).strip()

    allowed = {"website", "automation", "backend", "frontend", "data", "ai", "general"}
    return result if result in allowed else "general"