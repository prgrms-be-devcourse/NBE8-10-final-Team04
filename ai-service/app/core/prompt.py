# =========================
# 1. 시스템 프롬프트
# =========================
SYSTEM_PROMPT = """너는 비전공자 사용자를 돕는 친절한 AI 전문 도우미야.

[공통 규칙]
1. 초등학생도 이해할 수 있는 쉬운 단어를 쓰되, 성인에게 적합한 비유를 활용할 것.
2. 질문 내용을 그대로 반복하며 답변을 시작하지 말 것.
3. 제공된 데이터(Tool, Card, Model)에만 기반하여 답변하고, 모르는 것을 지어내지 말 것.
4. 지정된 출력 형식을 반드시 준수할 것.
"""


# =========================
# 2. 데이터 포맷팅
# =========================
def format_cards(cards: list[dict]) -> str:
    return "\n".join([
        f"""{i}. {card["title"]}
- 설명: {card["description"]}
- 이유: {card.get("reason", "")}
- README 일부: {card.get("readme", "")}
"""
        for i, card in enumerate(cards, 1)
    ])


def format_models(models: list[dict]) -> str:
    return "\n".join([
        f"""{i}. {m["model_name"]}
- 카테고리: {m.get("category", "")}
- 추천 이유: {m.get("reason", "")}
- context window: {m.get("context_window", "")}
- 입력 비용: {m.get("input_price", "")}
- 출력 비용: {m.get("output_price", "")}
"""
        for i, m in enumerate(models, 1)
    ])


# =========================
# 3. 메인 응답 프롬프트
# =========================
def build_chat_prompt(question: str, top_tool: dict, cards: list[dict], models: list[dict]) -> str:
    return f"""
참고 데이터:
[Top Tool] {top_tool["title"]} ({top_tool["description"]})
[AI Models]
{format_models(models)}

[Cards]
{format_cards(cards)}

사용자 질문: "{question}"

위 데이터를 바탕으로 아래 양식에 맞춰 답변해라.

### 1. 핵심 요약
사용자가 하려는 작업을 한 줄로 정의해라.

### 2. 추천 AI 툴 및 모델
- 가장 적합한 툴: {top_tool["title"]}
- 추천 모델: 반드시 위 AI Models 목록 중 하나만 선택
- 이유: 비전공자 기준으로 설명

### 3. 시작 가이드
처음 써보는 사람이 따라할 수 있게 단계별로 설명

### 4. 한눈에 비교
카드 3개를 한 줄씩 비교
"""


def build_full_prompt(question, top_tool, cards, models):
    return SYSTEM_PROMPT + "\n\n" + build_chat_prompt(question, top_tool, cards, models)


# =========================
# 4. 질문 분류 (category)
# =========================
QUESTION_CATEGORY_PROMPT = """
아래 사용자 질문의 유형을 반드시 하나만 골라라.

[유형]
recommendation
information
follow_up
out_of_scope
ambiguous

[판단 기준]
- recommendation: 추천 요청
- information: 설명 요청
- follow_up: 이어지는 질문
- out_of_scope: 범위 밖
- ambiguous: 애매함

[질문]
{question}

반드시 하나만 출력해라.
"""


def build_question_category_prompt(question: str) -> str:
    return QUESTION_CATEGORY_PROMPT.format(question=question)


# =========================
# 5. 추천 대상 (target)
# =========================
RECOMMENDATION_TARGET_PROMPT = """
아래 질문의 추천 대상을 반드시 하나만 골라라.

[대상]
ai_model
skill
ai_information
none

[판단 기준]
- ai_model: GPT, Gemini 같은 모델 추천
- skill: 자동화, 기능 구현
- ai_information: 특정 AI 설명
- none: 해당 없음

[질문]
{question}

반드시 하나만 출력해라.
"""


def build_recommendation_target_prompt(question: str) -> str:
    return RECOMMENDATION_TARGET_PROMPT.format(question=question)


# =========================
# 6. 모델 요구사항 추출 (핵심)
# =========================
MODEL_REQUIREMENTS_PROMPT = """
사용자 질문을 분석해서 모델 선택 기준을 JSON으로만 출력해라.

{{
  "task_type": "writing | code | image | document | chat | unknown",
  "needs_long_context": true,
  "needs_image": false,
  "needs_fast_response": false,
  "needs_low_cost": false,
  "needs_code": false,
  "needs_writing": false
}}

[질문]
{question}
"""


def build_model_requirements_prompt(question: str) -> str:
    return MODEL_REQUIREMENTS_PROMPT.format(question=question)


# =========================
# 7. 정보형
# =========================
def build_information_prompt(question: str) -> str:
    return f"""
질문: "{question}"

조건:
- 3줄 요약
- 쉬운 비유
- 마지막에 질문 유도
"""


# =========================
# 8. 후속 질문
# =========================
def build_followup_prompt(question: str) -> str:
    return f"""
추가 질문:
{question}

이전 대화 이어서 자연스럽게 설명
"""

# =========================
# 9. skill 추천 의도 분류
# =========================
SKILL_INTENT_PROMPT = """
아래 사용자 질문이 어떤 종류의 skill 추천을 원하는지 반드시 하나만 골라라.

[카테고리]
website
automation
backend
frontend
data
ai
general

[판단 기준]
- website: 웹사이트, 홈페이지, 웹서비스 만들기
- automation: 엑셀 자동화, 반복작업 자동화, OCR, 크롤링, 데이터 수집
- backend: 서버, API, DB, 인증, 배포, 백엔드 기능 구현
- frontend: UI, 화면, React, Vue, 사용자 화면 개발
- data: 데이터 분석, 전처리, CSV 처리
- ai: 챗봇, LLM, AI 기능 자체 구현
- general: 위에 명확히 속하지 않음

[질문]
{question}

반드시 하나만 출력해라.
설명하지 마라.
"""


def build_skill_intent_prompt(question: str) -> str:
    return SKILL_INTENT_PROMPT.format(question=question)