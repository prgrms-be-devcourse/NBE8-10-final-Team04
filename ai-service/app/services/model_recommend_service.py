from sqlalchemy import text
from app.db.database import SessionLocal


def retrieve_ai_models(question: str):
    db = SessionLocal()

    try:
        question_lower = question

        wants_long_context = any(keyword in question_lower for keyword in ["긴 문서", "요약", "정리", "분석"])
        wants_image = any(keyword in question_lower for keyword in ["이미지", "사진", "스크린샷", "영상"])
        wants_fast = any(keyword in question_lower for keyword in ["빠르게", "간단히", "짧게"])
        wants_code = any(keyword in question_lower for keyword in ["코드", "개발", "에러", "디버깅", "프로그래밍"])
        wants_writing = any(keyword in question_lower for keyword in ["자소서", "자기소개서", "글쓰기", "문장", "첨삭", "이력서"])

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
            ORDER BY context_window DESC NULLS LAST
            LIMIT 30;
        """)

        rows = db.execute(query).fetchall()

        scored_models = []

        for row in rows:
            score = 0
            reasons = []

            input_modalities = (row.input_modalities or "")
            output_modalities = (row.output_modalities or "")
            category = (row.category or "")

            if wants_long_context and row.context_window and row.context_window >= 100000:
                score += 3
                reasons.append("긴 문서나 많은 내용을 처리하기 좋음")

            if wants_image and ("image" in input_modalities or "image" in output_modalities):
                score += 3
                reasons.append("이미지 관련 작업에 활용하기 좋음")

            if wants_fast:
                if row.input_price is not None and row.output_price is not None:
                    if row.input_price < 1 and row.output_price < 5:
                        score += 2
                        reasons.append("비용 부담이 비교적 적고 가볍게 쓰기 좋음")

            if wants_code and ("code" in category or "reason" in category or "chat" in category):
                score += 2
                reasons.append("코드나 문제 해결 질문에 잘 맞음")

            if wants_writing and ("chat" in category or "reason" in category):
                score += 2
                reasons.append("글쓰기나 초안 작성에 활용하기 좋음")

            if "chat" in category:
                score += 1
                reasons.append("대화형 작업에 무난함")

            if row.max_output_tokens and row.max_output_tokens >= 8000:
                score += 1
                reasons.append("긴 답변 생성에 유리함")

            description_parts = []

            if row.category:
                description_parts.append(f"카테고리: {row.category}")
            if row.context_window:
                description_parts.append(f"컨텍스트 크기: {row.context_window}")
            if row.input_modalities:
                description_parts.append(f"입력: {row.input_modalities}")
            if row.output_modalities:
                description_parts.append(f"출력: {row.output_modalities}")

            description = " / ".join(description_parts) if description_parts else "AI 작업에 활용 가능한 모델"

            scored_models.append({
                "id": row.id,
                "title": row.model_name,
                "owner": None,
                "star": None,
                "description": description,
                "uploadedAt": None,
                "like": None,
                "score": score,
                "reason": ", ".join(dict.fromkeys(reasons)) if reasons else "일반적인 대화형 작업에 무난함",
                "type": "ai_model",
                "howToUse": "먼저 간단한 요청으로 결과를 확인한 뒤, 원하는 방향으로 다시 수정 요청해보세요.",

                # 필요하면 나중에 쓸 원본 데이터
                "model_name": row.model_name,
                "category": row.category,
                "context_window": row.context_window,
                "input_price": float(row.input_price) if row.input_price is not None else None,
                "output_price": float(row.output_price) if row.output_price is not None else None,
                "max_output_tokens": row.max_output_tokens,
                "input_modalities": row.input_modalities,
                "output_modalities": row.output_modalities,
            })

        scored_models.sort(
            key=lambda x: (x["score"], x["context_window"] or 0),
            reverse=True
        )

        return scored_models[:3]

    finally:
        db.close()