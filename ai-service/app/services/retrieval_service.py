import json
from sqlalchemy import text
from app.db.database import SessionLocal
from app.services.gemini_service import call_gemini


EMPTY_RESPONSE = {
    "top_tool": None,
    "cards": []
}


# =========================
# 1. 카드 생성
# =========================
def _build_card(row, final_score=None):
    return {
        "id": row.id,
        "title": row.name,
        "owner": getattr(row, "source_repo", "Unknown"),
        "star": getattr(row, "star_count", 0),
        "description": (row.summary or row.content_md or "설명 없음")[:120],
        "uploadedAt": str(row.created_at) if row.created_at else None,
        "reason": "",
        "like": False,
        "type": "skill",
        "readme": (row.content_md or "")[:500],
        "score": round(final_score, 4) if final_score is not None else None,
    }


# =========================
# 2. Gemini reason 생성
# =========================
def _generate_reasons(question, cards):
    try:
        prompt = f"""
사용자 질문: "{question}"

아래 3개의 스킬 각각에 대해
"왜 이 질문에 적합한지" 한 문장씩만 설명해라.

출력 형식:
["이유1", "이유2", "이유3"]

스킬 목록:
{[c["title"] for c in cards]}
"""
        result = call_gemini(prompt)
        reasons = json.loads(result)

        if isinstance(reasons, list):
            return reasons

    except Exception:
        pass

    return ["관련성이 높은 스킬입니다."] * len(cards)


# =========================
# 3. language_stats 파싱
# =========================
def _parse_language_stats(language_stats):
    if not language_stats:
        return {}

    if isinstance(language_stats, dict):
        return language_stats

    if isinstance(language_stats, str):
        try:
            return json.loads(language_stats)
        except Exception:
            return {}

    return {}


# =========================
# 4. intent 기반 보정 점수
# =========================
def _calculate_intent_bonus(skill_intent: str, summary: str, language_stats) -> float:
    summary_text = (summary or "").lower()
    lang = _parse_language_stats(language_stats)

    python_ratio = lang.get("Python", 0)
    html_ratio = lang.get("HTML", 0)
    js_ratio = lang.get("JavaScript", 0)
    ts_ratio = lang.get("TypeScript", 0)
    java_ratio = lang.get("Java", 0)
    shell_ratio = lang.get("Shell", 0)

    bonus = 0.0

    if skill_intent == "website":
        if html_ratio > 0:
            bonus += 0.15
        if js_ratio > 0 or ts_ratio > 0:
            bonus += 0.25
        if "web" in summary_text or "website" in summary_text or "frontend" in summary_text:
            bonus += 0.20
        if "react" in summary_text or "next" in summary_text or "vue" in summary_text:
            bonus += 0.20
        if python_ratio >= 80 and html_ratio == 0 and js_ratio == 0 and ts_ratio == 0:
            bonus -= 0.10

    elif skill_intent == "automation":
        if python_ratio >= 30:
            bonus += 0.25
        if shell_ratio > 0:
            bonus += 0.05
        if "automation" in summary_text or "crawler" in summary_text or "scraping" in summary_text:
            bonus += 0.20

    elif skill_intent == "backend":
        if java_ratio > 0 or python_ratio > 0:
            bonus += 0.20
        if "backend" in summary_text or "api" in summary_text or "server" in summary_text:
            bonus += 0.20
        if "spring" in summary_text or "fastapi" in summary_text or "django" in summary_text:
            bonus += 0.20

    elif skill_intent == "frontend":
        if html_ratio > 0:
            bonus += 0.10
        if js_ratio > 0 or ts_ratio > 0:
            bonus += 0.25
        if "frontend" in summary_text or "ui" in summary_text:
            bonus += 0.20
        if "react" in summary_text or "vue" in summary_text:
            bonus += 0.20
        if python_ratio >= 80 and html_ratio == 0 and js_ratio == 0 and ts_ratio == 0:
            bonus -= 0.15

    elif skill_intent == "data":
        if python_ratio >= 30:
            bonus += 0.20
        if "data" in summary_text or "analysis" in summary_text or "csv" in summary_text:
            bonus += 0.20

    elif skill_intent == "ai":
        if python_ratio >= 30:
            bonus += 0.10
        if "ai" in summary_text or "llm" in summary_text or "chatbot" in summary_text:
            bonus += 0.25

    return bonus


# =========================
# 5. 최종 점수 계산
# =========================
def _calculate_final_score(row, skill_intent: str) -> float:
    distance = float(row.distance) if row.distance is not None else 999.0

    # distance 낮을수록 좋음
    base_score = 1 / (1 + distance)

    intent_bonus = _calculate_intent_bonus(
        skill_intent=skill_intent,
        summary=getattr(row, "summary", ""),
        language_stats=getattr(row, "language_stats", {})
    )

    star_count = getattr(row, "star_count", 0) or 0
    star_bonus = min(star_count / 1000, 0.10)

    return base_score + intent_bonus + star_bonus


# =========================
# 6. 벡터 검색
# =========================
def _search_by_chunks(db, question_embedding, skill_intent):
    query = text("""
        SELECT 
            s.id, s.name, s.content_md,
            r.source_repo, r.source_uri, r.summary,
            r.language_stats,
            r.star_count, r.created_at, 
            sc.id AS chunk_id,
            sc.embedding <=> CAST(:embedding AS vector) AS distance
        FROM skill_chunks sc
        JOIN skills s ON s.id = sc.skill_id
        LEFT JOIN repositories r ON r.id = s.repository_id
        ORDER BY distance ASC, r.star_count DESC
        LIMIT 30;
    """)

    rows = db.execute(query, {"embedding": str(question_embedding)}).fetchall()

    if not rows:
        return []

    best_by_skill = {}

    for row in rows:
        final_score = _calculate_final_score(row, skill_intent)

        if row.id not in best_by_skill:
            best_by_skill[row.id] = {
                "row": row,
                "score": final_score
            }
        else:
            if final_score > best_by_skill[row.id]["score"]:
                best_by_skill[row.id] = {
                    "row": row,
                    "score": final_score
                }

    ranked = list(best_by_skill.values())
    ranked.sort(key=lambda x: x["score"], reverse=True)

    cards = []
    for item in ranked[:3]:
        cards.append(_build_card(item["row"], item["score"]))

    return cards


# =========================
# 7. 텍스트 fallback
# =========================
def _search_by_text(db, question):
    keywords = question.split()

    conditions = []
    params = {}

    for i, kw in enumerate(keywords):
        conditions.append(f"(LOWER(name) LIKE LOWER(:kw{i}) OR LOWER(content_md) LIKE LOWER(:kw{i}))")
        params[f"kw{i}"] = f"%{kw}%"

    if not conditions:
        return []

    query = text(f"""
        SELECT
            s.id, s.name, s.content_md,
            r.source_repo, r.summary, r.language_stats,
            r.star_count, r.created_at
        FROM skills s
        LEFT JOIN repositories r ON r.id = s.repository_id
        WHERE {" OR ".join(conditions)}
        ORDER BY r.star_count DESC
        LIMIT 10;
    """)

    rows = db.execute(query, params).fetchall()

    cards = []
    seen = set()

    for row in rows:
        if row.id in seen:
            continue

        seen.add(row.id)
        cards.append(_build_card(row))

        if len(cards) == 3:
            break

    return cards


# =========================
# 8. 메인
# =========================
def retrieve_recommendations(question: str, question_embedding: list[float], skill_intent: str = "general"):
    db = SessionLocal()

    try:
        cards = _search_by_chunks(db, question_embedding, skill_intent)

        if not cards:
            cards = _search_by_text(db, question)

        if not cards:
            return EMPTY_RESPONSE

        reasons = _generate_reasons(question, cards)

        for i, card in enumerate(cards):
            card["reason"] = reasons[i] if i < len(reasons) else "관련성이 높은 스킬입니다."

        return {
            "top_tool": cards[0],
            "cards": cards
        }

    except Exception as e:
        print(f"[Retrieval Error] {e}")
        return EMPTY_RESPONSE

    finally:
        db.close()