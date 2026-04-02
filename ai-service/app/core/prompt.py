SYSTEM_PROMPT = """너는 비전공자 사용자를 돕는 친절한 AI 전문 도우미야.
[공통 규칙]
1. 초등학생도 이해할 수 있는 쉬운 단어를 쓰되, 성인에게 적합한 비유를 활용할 것.
2. 질문 내용을 그대로 반복하며 답변을 시작하지 말 것.
3. 제공된 데이터(Tool, Card, Model)에만 기반하여 답변하고, 모르는 것을 지어내지 말 것.
4. 지정된 출력 형식을 반드시 준수할 것."""

def format_card(cards: list[dict]) -> str:
    result = ""
    for i, card in enumerate(cards, 1):
        result += f"""
        {i}. {card["title"]}
        - 설명: {card["description"]}
        - 이유: {card.get("reason", "")}
        - README 일부: {card.get("readme", "")}
        """
    return result

def format_ai_models(ai_models: list[dict]) -> str:
    result = ""
    for i, model in enumerate(ai_models, 1):
        result += f"""
        {i}. {model["model_name"]}
        - 카테고리: {model.get("category", "")}
        - 추천 이유: {model.get("reason", "")}
        - context window: {model.get("context_window", "")}
        - 입력 비용: {model.get("input_price", "")}
        - 출력 비용: {model.get("output_price", "")}
        """
    return result


def build_user_prompt(question: str, top_tool: dict, cards: list[dict], ai_models: list[dict]) -> str:
    cards_str = format_card(cards)
    ai_models_str = format_ai_models(ai_models)

    return f"""
        참고 데이터:
        [Top Tool] {top_tool["title"]} ({top_tool["description"]})
        [AI Models] {ai_models_str}
        [Cards] {cards_str}

        사용자 질문: "{question}"

        위 데이터를 바탕으로 아래 양식에 맞춰 답변해줘.

        ### 1. 핵심 요약
        사용자가 하려는 작업을 한 줄로 정의해줘.

        ### 2. 추천 AI 툴 및 모델
        - 가장 적합한 툴: {top_tool["title"]}
        - 추천 모델: (제시된 모델 중 가장 가성비 좋은 것 하나 선택)
        - 이유: (비전공자 관점에서 설명)

        ### 3. 시작 가이드
        비전공자가 이 툴을 어떻게 처음 써보면 좋을지 단계별로 간단히 알려줘.

        ### 4. 한눈에 비교
        제공된 카드 3개의 특징을 한 문장씩 비교해서 표나 리스트로 보여줘.
        """


def build_full_prompt(question, top_tool, cards, ai_models):
    return SYSTEM_PROMPT + "\n\n" + build_user_prompt(question, top_tool, cards, ai_models)

# 정보형 질문 프롬프트
INFORMATION_PROMPT = """
너는 비전공자를 위한 AI 설명 도우미다.
항상 쉽고 간단하게 설명해라.
"""

# [정보형] 규칙 중복 제거
def build_information_prompt(question: str) -> str:
    return f"""질문: "{question}"

        기술 가이드 답변 규칙:
        1. 전문 용어는 반드시 일상적인 비유로 설명.
        2. 3줄 내외로 핵심만 요약.
        3. 마지막에 "더 궁금한 AI 기술이 있나요?" 문구 포함."""

# 후속 질문형 프롬프트
FOLLOWUP_PROMPT = """
    너는 사용자의 질문을 이어서 설명하는 AI 도우미다.
    맥락을 자연스럽게 이어서 답변해라.
    """



def build_followup_prompt(question: str) -> str:
    return f"""
        사용자 추가 질문:
        {question}
        이전 대화에 이어서 설명하듯 자연스럽게 답변해라.
        """



QUESTION_TYPE_CLASSIFY_PROMPT = """
아래 사용자 질문의 유형을 반드시 하나만 골라라.

[유형]
recommendation
information
follow_up
out_of_scope
ambiguous


[판단 기준]
- recommendation: AI 도구, 스킬, 서비스 추천 요청
- information: 개념, 뜻, 원리, 사용법 설명 요청
- follow_up: 이전 대화 맥락을 이어가는 후속 질문
- out_of_scope: AI 추천 서비스 범위를 벗어난 질문
- ambiguous: 의도가 모호해서 바로 판단하기 어려운 질문

[질문]
{question}
반드시 위 5개 중 하나의 단어만 출력해라.
설명하지 마라.
"""

TARGET_TYPE_CLASSIFY_PROMPT = """
아래 사용자 질문의 추천/설명 대상을 반드시 하나만 골라라.

[대상]
ai_model
skill
ai_information
none

[판단 기준]
- ai_model: ChatGPT, Gemini, Claude 같은 구체적인 AI 서비스나 도구 추천 요청
- skill: 웹사이트 만들기, 엑셀 자동화, 업무 자동화 같은 실무 AI agent skill 추천 요청
- ai_information: 특정 AI 툴(예: ChatGPT 업데이트, 제미나이 특징)에 대한 구체적인 정보/비교 설명 요청
- none: RAG, 임베딩, LLM 같은 '일반적인 AI 기술 개념'이나 서비스 범위를 벗어난 질문, 또는 판단 불가

[질문]
{question}

반드시 위 4개 중 하나의 단어만 출력해라.
설명하지 마라.
"""

def build_question_type_prompt(question: str) -> str:
    return QUESTION_TYPE_CLASSIFY_PROMPT.format(question=question)

def build_target_type_prompt(question: str) -> str:
    return TARGET_TYPE_CLASSIFY_PROMPT.format(question=question)