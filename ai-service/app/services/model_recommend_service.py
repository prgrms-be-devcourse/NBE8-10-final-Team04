import json
from sqlalchemy import text
from app.db.database import SessionLocal
from app.core.prompt import build_model_requirements_prompt
from app.services.gemini_service import call_gemini


EMPTY_MESSAGE = [{
    "id": "empty-ai-model",
    "title": "데이터 준비 중",
    "description": "ai 데이터가 아직 없어요! 곧 업데이트 예정이니 조금만 기다려주세요!",
    "reason": "현재 조건에 맞는 모델이 없음",
    "type": "ai_model"
}]


def parse_requirements(raw: str):
    try:
        return json.loads(raw)
    except:
        return {
            "task_type": "unknown",
            "needs_image": False,
            "needs_code": False,
            "needs_writing": False
        }


def match_model(row, req):
    input_mod = str(row.input_modalities or "").lower()
    output_mod = str(row.output_modalities or "").lower()

    # image 필요한 경우
    if req["needs_image"]:
        return "image" in input_mod or "image" in output_mod

    # 코드 작업
    if req["needs_code"]:
        return "text" in output_mod  # 코드도 결국 text로 나옴

    # 글쓰기
    if req["needs_writing"]:
        return "text" in output_mod

    # 기본
    return True


def retrieve_ai_models(question: str):
    db = SessionLocal()

    try:
        # Gemini에게 판단 맡김
        raw = call_gemini(build_model_requirements_prompt(question))
        req = parse_requirements(raw)

        query = text("""
            SELECT
                id,
                model_name,
                category,
                context_window,
                input_price,
                output_price,
                max_output_tokens,
                input_modalities,
                output_modalities,
                is_preview
            FROM ai_models
            WHERE is_preview = false
            LIMIT 30;
        """)

        rows = db.execute(query).fetchall()

        filtered = []

        for row in rows:
            if not match_model(row, req):
                continue

            description = []
            if row.category:
                description.append(f"카테고리: {row.category}")
            if row.context_window:
                description.append(f"컨텍스트: {row.context_window}")
            if row.input_modalities:
                description.append(f"입력: {row.input_modalities}")
            if row.output_modalities:
                description.append(f"출력: {row.output_modalities}")

            filtered.append({
                "id": row.id,
                "title": row.model_name,
                "description": " / ".join(description),
                "reason": "요청한 작업에 맞는 모델",
                "type": "ai_model",
                "model_name": row.model_name,
                "category": row.category,
                "context_window": row.context_window,
                "input_modalities": row.input_modalities,
                "output_modalities": row.output_modalities,
            })

        # 없으면 안내 메시지
        if not filtered:
            return EMPTY_MESSAGE

        # 간단 정렬 (context 기준)
        filtered.sort(
            key=lambda x: x["context_window"] or 0,
            reverse=True
        )

        return filtered[:3]

    finally:
        db.close()