from sqlalchemy import text
from app.db.database import SessionLocal

def retrieve_ai_models(question: str):
    db = SessionLocal()

    try:
        # 1. 질문 키워드 추출 (이건 사용자의 의도를 파악하기 위해 필요함)
        keywords = {
            "code": ["코드", "개발", "에러", "코딩", "sql", "python", "programming"],
            "image": ["이미지", "사진", "스크린샷", "영상", "그림"],
            "fast": ["빠르게", "간단히", "짧게", "빨리", "가벼운"],
            "logic": ["논리", "어려운", "수학", "추론", "분석", "깊게"]
        }

        # 2. DB에서 모든 활성 패밀리 가져오기
        query = text("""
            SELECT f.id, f.family_name, f.common_description, v.name as vendor_name
            FROM ai_model_families f
            JOIN ai_vendors v ON f.vendor_id = v.id
            WHERE v.is_active = true
        """)
        rows = db.execute(query).fetchall()

        scored_families = []
        for row in rows:
            score = 0
            reasons = []
            
            # DB의 이름과 설명을 합쳐서 분석 대상으로 삼음
            target_text = f"{row.family_name} {row.common_description}".lower()

            # --- [자동 분석 로직] ---
            
            # 질문의 의도와 DB 텍스트가 매칭되는지 확인
            for category, words in keywords.items():
                if any(word in question for word in words): # 사용자가 이 카테고리를 원하는데
                    # DB 설명/이름에 해당 키워드가 포함되어 있다면 점수 대폭 상승
                    # (예: 설명에 'coding'이나 'vision' 같은 영문 키워드가 섞여 있을 확률이 높음)
                    if any(word in target_text for word in words) or \
                       (category == "code" and "code" in target_text) or \
                       (category == "image" and ("vision" in target_text or "image" in target_text)):
                        score += 10
                        reasons.append(f"요청하신 {category} 관련 작업에 최적화된 모델입니다.")

            # 최신 모델에 대한 기본 점수 (숫자가 높을수록 최신일 확률이 높으므로 텍스트 기반 추출)
            # 이름에 포함된 숫자가 높으면 약간의 가산점 (예: 5.4 > 3.5)
            import re
            numbers = re.findall(r'\d+\.?\d*', row.family_name)
            if numbers:
                score += float(numbers[0]) 

            if not reasons:
                reasons.append(f"{row.vendor_name}의 신뢰할 수 있는 모델 시리즈입니다.")

            scored_families.append({
                "id": row.id,
                "title": row.family_name,
                "owner": row.vendor_name,
                "description": row.common_description,
                "score": score,
                "reason": reasons[0],
                "type": "ai_model_family"
            })

        # 점수 정렬 (높은 순)
        scored_families.sort(key=lambda x: x["score"], reverse=True)

        return scored_families[:3]

    finally:
        db.close()