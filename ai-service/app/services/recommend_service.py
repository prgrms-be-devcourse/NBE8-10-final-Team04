from app.schemas.chat import ChatResponse, QuestionType, TargetType
from app.services.embedding_service import create_embedding
from app.services.retrieval_service import retrieve_recommendations
from app.services.model_recommend_service import retrieve_ai_models
from app.services.question_type_service import (classify_question, 
    classify_skill_intent )
from app.services.gemini_service import (
    generate_summary,
    generate_information_answer,
    generate_followup_answer,
)

# === 초기화면 멘트

def get_welcome_message():
    return {
        "question": "INIT",
        "message": "안녕하세요! AI 툴 추천, 스킬 추천을 도와드립니다!",
        "nextActions": ["AI 툴 추천해줘", "내게 맞는 스킬은?"], 
        "outOfScope": False,
        "cards": [], 
        "topPick": None
    }


def handle_chat(question: str) -> ChatResponse:
    q = str(question).strip()

    if not q:
        return _empty_response(q, "질문을 조금 더 구체적으로 입력해주세요.")

    classification = classify_question(q)

    q_type = classification["questionType"]
    t_type = classification["targetType"]

    # =========================
    # 1. out_of_scope
    # =========================
    if q_type == "out_of_scope":
        return _simple_response(
            q,
            QuestionType.out_of_scope,
            "이 서비스는 AI 추천 서비스입니다 🙂",
            out_of_scope=True
        )

    # =========================
    # 2. ambiguous
    # =========================
    if q_type == "ambiguous":
        return _simple_response(
            q,
            QuestionType.ambiguous,
            "조금 더 구체적으로 말해주면 정확하게 추천해줄 수 있어요!",
        )

    # =========================
    # 3. information
    # =========================
    if q_type == "information":
        try:
            msg = generate_information_answer(q)
        except:
            msg = "설명 생성 중 문제가 발생했어요 😢"

        return _simple_response(q, QuestionType.information, msg)

    # =========================
    # 4. follow_up
    # =========================
    if q_type == "follow_up":
        try:
            msg = generate_followup_answer(q)
        except:
            msg = "답변 생성 중 문제가 발생했어요 😢"

        return _simple_response(q, QuestionType.follow_up, msg)

    # =========================
    # 5. recommendation
    # =========================
    if t_type == "ai_model":
        return _handle_ai_model(q)

    if t_type == "skill":
        return _handle_skill(q)

    return _simple_response(q, QuestionType.ambiguous, "어떤걸 추천해드릴까요? 🙂")


# =========================
# 공통 처리
# =========================

def _empty_response(question, msg):
    return ChatResponse(
        question=question,
        questionType=QuestionType.ambiguous,
        targetType=TargetType.none,
        message=msg,
        cards=[],
        topPick=None,
        nextActions=[],
        outOfScope=False
    )


def _simple_response(question, q_type, msg, out_of_scope=False):
    return ChatResponse(
        question=question,
        questionType=q_type,
        targetType=TargetType.none,
        message=msg,
        cards=[],
        topPick=None,
        nextActions=[],
        outOfScope=out_of_scope
    )


# =========================
# AI 모델 추천
# =========================
def _handle_ai_model(question):
    models = retrieve_ai_models(question)

    if not models:
        return _simple_response(
            question,
            QuestionType.recommendation,
            "ai 데이터가 아직 없어요! 곧 업데이트 예정이에요 🙏"
        )

    top = models[0]

    # topPick 변환
    top_pick = {
        "id": top.get("id"),
        "title": str(top.get("title")),
        "description": str(top.get("description", "")),
        "reason": str(top.get("reason", "")),
        "howToUse": str(
            top.get("howToUse", "간단한 질문부터 입력해서 사용해보세요.")
        ),
    }

    # cards 변환
    cards = [{
        "id": c.get("id"),
        "title": str(c.get("title")),
        "description": str(c.get("description", "")),
        "reason": str(c.get("reason", "")),
        "type": "ai_model"
    } for c in models[:3]]


    try:
        message = generate_summary(question, top, models[:3], models)
    except:
        message = f"{top['title']} 추천!"   

    return ChatResponse(
        question=question,
        questionType=QuestionType.recommendation,
        targetType=TargetType.ai_model,
        message=message,
        cards=cards,
        topPick=top_pick,
        nextActions=["다른 AI 추천받기"],
        outOfScope=False
    )


# =========================
# Skill 추천
# =========================
def _handle_skill(question):
    try:
        skill_intent = classify_skill_intent(question)
        emb = create_embedding(question)
        result = retrieve_recommendations(question, emb, skill_intent)

        raw_cards = result.get("cards", [])
        top = result.get("top_tool")

        ai_models = retrieve_ai_models(question)

    except Exception as e:
        print("Skill Retrieval Error:", e)
        return _simple_response(
            question,
            QuestionType.recommendation,
            "스킬 조회 중 문제가 발생했어요 😢"
        )

    if not raw_cards or not top:
        return _simple_response(
            question,
            QuestionType.recommendation,
            "관련 스킬을 찾지 못했어요 😢"
        )

    cards = [{
        "id": c.get("id"),
        "title": str(c.get("title")),
        "owner": c.get("owner"),        
        "star": c.get("star"),         
        "description": str(c.get("description", "")),
        "uploadedAt": c.get("uploadedAt"),  
        "like": c.get("like", False),   
        "reason": str(c.get("reason", "")),
        "score": c.get("score"),    
        "type": "skill"
    } for c in raw_cards]

    top_pick = {
        "id": top.get("id"),
        "title": str(top.get("title")),
        "description": str(top.get("description", "")),
        "reason": str(top.get("reason", "")),
        "howToUse": str(
            top.get("howToUse", "README를 참고해서 사용해보세요.")
        ),
    }

    try:
        message = generate_summary(question, top, cards, ai_models)
    except:
        message = f"{top['title']} 추천!" 
    
    ai_hint = f"💡 이 스킬은 {ai_models[0]['title']}와 함께 사용하면 더 좋아요!" if ai_models else ""

    return ChatResponse(
        question=question,
        questionType=QuestionType.recommendation,
        targetType=TargetType.skill,
        message=message + ("\n\n" + ai_hint if ai_hint else ""),
        cards=cards,
        topPick=top_pick,
        nextActions=["AI 추천받기"],
        outOfScope=False
    )