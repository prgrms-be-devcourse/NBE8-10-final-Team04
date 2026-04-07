import json
from sqlalchemy import text
from app.db.database import SessionLocal
from app.services.gemini_service import call_gemini

EMPTY_MESSAGE = [{
    "id": "empty-ai-model",
    "title": "데이터 준비 중",
    "description": "AI 데이터가 아직 없어요! 곧 업데이트 예정이니 조금만 기다려주세요!",
    "reason": "현재 조건에 맞는 모델이 없음",
    "type": "ai_model"
}]


def _generate_reasons(question: str, models: list) -> list:
    try:
        prompt = f"""
사용자 질문: "{question}"

아래 AI 모델 각각에 대해 왜 이 질문에 적합한지
비전공자가 이해할 수 있게 한 문장씩만 설명해라.

출력 형식 (JSON 배열만, 다른 말 하지 말것):
["이유1", "이유2", "이유3"]

모델 목록:
{[m["title"] for m in models]}
"""
        result = call_gemini(prompt)
        cleaned = result.strip().replace("```json", "").replace("```", "")
        reasons = json.loads(cleaned)
        if isinstance(reasons, list):
            return reasons
    except Exception:
        pass
    return ["요청하신 작업에 적합한 모델이에요."] * len(models)


def retrieve_ai_models(question: str):
    db = SessionLocal()
    try:
        # ✅ 실제 컬럼명에 맞게 수정: family_name, common_description, input_types, output_types
        rows = db.execute(text("""
            SELECT 
                f.id,
                f.family_name,
                f.common_description,
                f.input_types,
                f.output_types,
                v.name AS vendor_name
            FROM ai_model_families f
            LEFT JOIN ai_vendors v ON v.id = f.vendor_id
            WHERE v.is_active = true
            LIMIT 50;
        """)).fetchall()

        if not rows:
            return EMPTY_MESSAGE

        # Gemini에게 질문에 맞는 모델 top3 순위 선정
        rank_prompt = f"""
사용자 질문: "{question}"

아래 AI 모델 중에서 이 질문에 가장 적합한 순서대로 3개만 골라서
id만 JSON 배열로 응답하세요. 다른 말은 절대 하지 마세요.

모델 목록:
{json.dumps([{
    "id": r.id,
    "name": r.family_name,
    "description": r.common_description,
    "output_types": str(r.output_types)
} for r in rows], ensure_ascii=False)}

응답 예시: [3, 7, 12]
"""
        try:
            raw = call_gemini(rank_prompt)
            cleaned = raw.strip().replace("```json", "").replace("```", "")
            ranked_ids = json.loads(cleaned)

            row_map = {r.id: r for r in rows}
            top3_rows = [row_map[rid] for rid in ranked_ids if rid in row_map][:3]

        except Exception as e:
            print(f"[ModelRecommend] Gemini 순위 선정 실패, fallback: {e}")
            top3_rows = list(rows[:3])

        if not top3_rows:
            return EMPTY_MESSAGE

        # 카드 구성 - ✅ 실제 컬럼명 사용
        top3 = [{
            "id": r.id,
            "title": r.family_name,
            "description": r.common_description or "",
            "reason": "",
            "type": "ai_model",
            "model_name": r.family_name,
            "vendor_name": r.vendor_name or "",
            "input_types": r.input_types,
            "output_types": r.output_types,
        } for r in top3_rows]

        reasons = _generate_reasons(question, top3)
        for i, m in enumerate(top3):
            m["reason"] = reasons[i] if i < len(reasons) else "요청에 적합한 모델이에요."

        return top3

    except Exception as e:
        print(f"[ModelRecommend Error] {e}")
        return EMPTY_MESSAGE

    finally:
        db.close()