from app.core.prompt import (
    build_question_type_prompt,
    build_target_type_prompt,
)
from app.services.gemini_service import call_gemini


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


def make_result(question_type: str, target_type: str) -> dict:
    return {
        "questionType": question_type,
        "targetType": target_type,
    }


def safe_to_text(value) -> str:
    # 1. 리스트인 경우 첫 번째 요소를 꺼냄
    if isinstance(value, list):
        value = value[0] if value else ""
    
    # 2. None이거나 문자열이 아닌 경우를 대비해 강제 형변환 및 정리
    if value is None:
        return ""
    return str(value).strip()


def normalize_question_type(text) -> str:
    # [수정 핵심] safe_to_text를 먼저 거쳐서 무조건 깨끗한 문자열로 만듦
    text = safe_to_text(text).lower()

    for t in VALID_QUESTION_TYPES:
        if t in text: # 이제 'list' object has no attribute 'lower' 에러가 절대 안 남
            return t

    return "ambiguous"

def normalize_target_type(text) -> str:
    text = safe_to_text(text).lower()

    for t in VALID_TARGET_TYPES:
        if t in text:
            return t

    return "none"


def classify_by_rule(question: str) -> dict:
    if isinstance(question, list):
        # 리스트라면 첫 번째 요소 추출, 빈 리스트면 빈 문자열
        question = question[0] if question else ""
    
    # 확실하게 str 타입으로 강제 변환 후 전처리
    q = str(question).strip().lower()

    out_of_scope_keywords = [
        "날씨", "주식", "연애", "병원", "약", "통증", "정치",
        "대통령", "점심", "저녁 메뉴", "운세", "사주", "타로",
        "감기", "두통", "복통", "상담", "고민 들어줘",
        # English Keywords
        "weather", "stock", "invest", "dating", "hospital", "doctor", "medicine",
        "politics", "lunch", "dinner", "menu", "fortune", "tarot",
        "cold", "headache", "stomachache", "consult", "worries"
    ]

    follow_up_keywords = [
        "그럼", "그러면", "이거", "그거", "저거", "이건", "그건",
        "무료야", "유료야", "설치는", "어떻게 해", "어떻게 써",
        "더 자세히", "왜 그래", "그 다음", "그 다음엔", "이후엔",
        "해보려면", "쓸려면",
        # English Keywords
        "then", "so", "this", "that", "it", 
        "is it free", "is it paid", "how to install", "how to use",
        "more detail", "why", "next", "after that", "to try"
    ]

    ai_model_keywords = [
        "자소서", "자기소개서", "글쓰기", "문장", "번역", "요약",
        "발표자료", "ppt", "이미지 생성", "사진 생성", "그림 생성",
        "챗gpt", "chatgpt", "제미나이", "gemini", "claude",
        "어떤 ai", "어떤 툴", "ai 추천", "툴 추천",
        # English Keywords
        "self-introduction", "resume", "writing", "sentence", "translate", "summarize",
        "presentation", "slides", "image generation", "generate photo", "draw",
        "gpt", "gemini", "claude", "which ai", "which tool", "ai recommendation"
    ]

    skill_keywords = [
        "웹사이트", "사이트", "서비스 만들기", "앱 만들기",
        "엑셀 자동화", "업무 자동화", "회계 자동화", "자동화",
        "agent", "에이전트", "skill", "스킬", "workflow",
        "공유일기", "만들고 싶어", "구현하고 싶어",
        # English Keywords
        "website", "site", "build service", "make app",
        "excel automation", "work automation", "accounting automation", "automation",
        "agent", "skill", "workflow", "shared diary", "want to build", "want to make"
    ]

    information_keywords = [
        "뭐야", "무엇", "뜻", "설명", "설명해", "설명해줘", "what is",
        "원리", "차이", "사용법", "정의", "개념", "의미", "어떻게 동작",
        "어떤 구조", "왜 쓰", "왜 사용", "무슨 역할", "업데이트", "최근 변경",
        # English Keywords
        "what is", "meaning", "explain", "how to", "definition", "concept",
        "principle", "difference", "how it works", "structure", "why use",
        "role", "update", "recent change"
    ]

    recommendation_keywords = [
        "추천", "추천해", "추천해줘", "알려줘", "뭐 써", "뭐쓰면",
        "골라줘", "찾아줘", "적합한", "좋은", "쓸만한",
        "추천 받을", "추천받고", "추천받기",
        # English Keywords
        "recommend", "suggest", "tell me", "what to use", "which one",
        "pick", "find", "suitable", "good", "useful", "get recommendation"
    ]

    # [2] 판단 로직
    if any(word in q for word in out_of_scope_keywords):
        return make_result("out_of_scope", "none")

    if any(word in q for word in recommendation_keywords):
        if any(word in q for word in ai_model_keywords):
            return make_result("recommendation", "ai_model")
        if any(word in q for word in skill_keywords):
            return make_result("recommendation", "skill")
        # 키워드 조합이 없어도 '추천' 단어가 있으면 기본적으로 ai_model 추천으로 간주
        return make_result("recommendation", "ai_model")

    if any(word in q for word in skill_keywords):
        return make_result("recommendation", "skill")

    if any(word in q for word in follow_up_keywords):
        return make_result("follow_up", "none")

    if any(word in q for word in information_keywords):
        return make_result("information", "ai_information")

    # 특정 모델명만 언급했을 때
    if any(word in q for word in ["챗gpt", "chatgpt", "제미나이", "gemini", "claude"]):
        return make_result("information", "ai_information")

    # 위 조건에 모두 해당하지 않으면 모호함(LLM에게 넘김)
    return make_result("ambiguous", "none")


def classify_by_llm(question: str) -> dict:
    try:
        question_type_prompt = build_question_type_prompt(question)
        target_type_prompt = build_target_type_prompt(question)

        question_type_result = call_gemini(question_type_prompt)
        target_type_result = call_gemini(target_type_prompt)

        print("DEBUG question_type_result:", question_type_result, type(question_type_result))
        print("DEBUG target_type_result:", target_type_result, type(target_type_result))

        question_type = normalize_question_type(question_type_result)
        target_type = normalize_target_type(target_type_result)

        if question_type in {"follow_up", "out_of_scope", "ambiguous"}:
            target_type = "none"

        return make_result(question_type, target_type)

    except Exception as e:
        print("classify_by_llm error:", e)
        return make_result("ambiguous", "none")


def safe_question(q):
    if isinstance(q, list):
        return q[0] if q else ""
    return str(q)

def classify_question(question: str) -> dict:
    if isinstance(question, list):
        clean_question = question[0] if question else ""
    else:
        clean_question = str(question)

    rule_result = classify_by_rule(clean_question)

    if rule_result["questionType"] != "ambiguous":
        return rule_result

    return classify_by_llm(clean_question)