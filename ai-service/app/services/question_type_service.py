import json
from app.core.prompt import build_question_category_prompt
from app.services.gemini_service import call_gemini

VALID_QUESTION_TYPES = {"recommendation", "information", "follow_up", "out_of_scope", "ambiguous"}
VALID_TARGET_TYPES = {"ai_model", "skill", "ai_information", "none"}


# 개선 - Gemini에게 JSON으로 강제 응답받기
def classify_question(question: str) -> dict:
    try:
        raw = call_gemini(build_question_category_prompt(question))
        cleaned = raw.strip().replace("```json", "").replace("```", "")
        result = json.loads(cleaned)

        q_type = result.get("questionType", "ambiguous")
        t_type = result.get("targetType", "none")

        if q_type not in VALID_QUESTION_TYPES:
            q_type = "ambiguous"
        if t_type not in VALID_TARGET_TYPES:
            t_type = "none"

        return {"questionType": q_type, "targetType": t_type}

    except Exception as e:
        print("classify_question error:", e)
        return {"questionType": "ambiguous", "targetType": "none"}

def classify_skill_intent(question: str) -> str:
    from app.core.prompt import build_skill_intent_prompt
    result = call_gemini(build_skill_intent_prompt(question)).strip()
    allowed = {"website", "automation", "backend", "frontend", "data", "ai", "general"}
    return result if result in allowed else "general"