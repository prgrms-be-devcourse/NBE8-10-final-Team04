from sqlalchemy import text
from app.db.database import SessionLocal


def _build_card_from_chunk_row(row):
    distance = float(row.distance)
    score = max(0.0, round(1 - distance, 4))

    return {
        "id": row.id,
        "title": row.name,
        "owner": row.source_repo,  # 스키마의 owner/name 형식 사용
        "star": row.star_count,
        "description": row.summary or "설명 없음", # repositories.summary 사용
        "uploadedAt": str(row.created_at) if row.created_at else None,
        "reason": "질문과 관련성이 높은 스킬이 검색되었습니다.",
        "like": False,
        "score": score,
        "type": "skill",
        "readme": (row.content_md or "")[:500],
        "source_url": row.source_uri,
        "search_text": row.search_text,
        "distance": distance,
        "chunk_id": row.chunk_id,
        "skill_id": row.skill_id,
        "section_title": row.section_title,
    }


def _build_card_from_skill_row(row, question: str):
    return {
        "id": row.id,
        "title": row.name,
        "owner": getattr(row, 'source_repo', 'Unknown'),
        "star": getattr(row, 'star_count', 0),
        "description": (row.content_md or "설명 없음")[:120],
        "uploadedAt": str(row.created_at) if row.created_at else None,
        "reason": f"'{question}'와 관련된 스킬 설명이 검색되었습니다.",
        "like": False,
        "score": 0.5,
        "type": "skill",
        "readme": (row.content_md or "")[:500],
    }


def _search_by_chunks(db, question_embedding: list[float]):
    # query = text("""
    #     SELECT
    #         s.id,
    #         s.name,
    #         s.content_md,
    #         s.file_path,
    #         sc.id AS chunk_id,
    #         sc.skill_id,
    #         sc.section_title,
    #         sc.search_text,
    #         sc.embedding <=> CAST(:question_embedding AS vector) AS distance
    #     FROM skill_chunks sc
    #     JOIN skills s ON s.id = sc.skill_id
    #     ORDER BY sc.embedding <=> CAST(:question_embedding AS vector)
    #     LIMIT 10;
    # """)

    query = text("""
        SELECT 
            s.id, s.name, s.content_md,
            r.source_repo, r.source_uri, r.summary, 
            r.star_count, r.created_at, 
            sc.id AS chunk_id, sc.skill_id, sc.section_title, sc.search_text,
            sc.embedding <=> CAST(:question_embedding AS vector) AS distance
        FROM skill_chunks sc
        JOIN skills s ON s.id = sc.skill_id
        LEFT JOIN repositories r ON r.id = s.repository_id
        ORDER BY distance ASC, r.star_count DESC
        LIMIT 10;
    """)
    result = db.execute(query, {"question_embedding": str(question_embedding)})
    rows = result.fetchall()

    if not rows:
        return {
            "top_tool": None,
            "cards": []
        }

    cards = []
    seen_skill_ids = set()

    for row in rows:
        if row.id in seen_skill_ids:
            continue

        seen_skill_ids.add(row.id)
        cards.append(_build_card_from_chunk_row(row))

        if len(cards) == 3:
            break

    return {
        "top_tool": cards[0] if cards else None,
        "cards": cards
    }


def _search_by_skills_text(db, question: str):
    keywords = question.strip().split()

    conditions = []
    params = {}

    for i, keyword in enumerate(keywords):
        conditions.append(f"(LOWER(name) LIKE LOWER(:kw{i}) OR LOWER(content_md) LIKE LOWER(:kw{i}))")
        params[f"kw{i}"] = f"%{keyword}%"

    if not conditions:
        return {
            "top_tool": None,
            "cards": []
        }

    query = text(f"""
            SELECT
                s.id, s.name, s.content_md, s.file_path,
                r.source_repo, r.star_count, r.created_at
            FROM skills s
            LEFT JOIN repositories r ON r.id = s.repository_id
            WHERE {" OR ".join(conditions)}
            ORDER BY r.star_count DESC
            LIMIT 10;
        """)

    result = db.execute(query, params)
    rows = result.fetchall()

    if not rows:
        return {
            "top_tool": None,
            "cards": []
        }

    cards = []
    seen_skill_ids = set()

    for row in rows:
        if row.id in seen_skill_ids:
            continue

        seen_skill_ids.add(row.id)
        cards.append(_build_card_from_skill_row(row, question))

        if len(cards) == 3:
            break

    return {
        "top_tool": cards[0] if cards else None,
        "cards": cards
    }


def retrieve_recommendations(question: str, question_embedding: list[float]):
    db = SessionLocal()
    try:
        # 1차: 벡터 검색
        chunk_result = _search_by_chunks(db, question_embedding)
        if chunk_result["cards"]:
            return chunk_result

        # 2차: 텍스트 fallback
        text_result = _search_by_skills_text(db, question)
        return text_result
    except Exception as e:
        print(f"[Retrieval Error] {e}")
        return {"top_tool": None, "cards": []}
    finally:
        db.close()