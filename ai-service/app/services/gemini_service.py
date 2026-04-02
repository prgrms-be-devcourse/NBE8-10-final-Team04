from app.core.prompt import (
    SYSTEM_PROMPT,
    build_user_prompt,
    INFORMATION_PROMPT,
    build_information_prompt,
    FOLLOWUP_PROMPT,
    build_followup_prompt,
)
from dotenv import load_dotenv
import google.generativeai as genai
import os

load_dotenv()

api_key = os.getenv("GEMINI_API_KEY")

if not api_key:
    raise ValueError("GEMINI_API_KEY가 설정되지 않았습니다.")

genai.configure(api_key=api_key)

# 한도 초과시 버전 변경
# model = genai.GenerativeModel("gemini-3.1-flash-lite-preview")

# 수정 전
model = genai.GenerativeModel("gemini-3.1-flash-lite-preview")

# 수정 후
generation_config = {
    "temperature": 0.7,        # 창의성 조절 (너무 높으면 헛소리를 함)
    "top_p": 0.95,
    "top_k": 40,
    "max_output_tokens": 1024, # 답변 길이를 강제로 제한 (무한 루프 방지)
    "response_mime_type": "text/plain",
}

model = genai.GenerativeModel(
    model_name="gemini-3.1-flash-lite-preview",
    generation_config=generation_config
)


def _safe_text(value) -> str:
    if isinstance(value, list):
        if not value:
            return ""
        value = value[0]

    if value is None:
        return ""

    return str(value).strip()


def call_gemini(prompt: str) -> str:
    try:
        response = model.generate_content(prompt)

        raw_text = getattr(response, "text", "")
        text = _safe_text(raw_text)

        # --------------------------------------------------
        # 테스트 확인용
        print("\n" + "="*50)
        print(f"[Gemini Response]\n{text}")
        print("="*50 + "\n")
        # --------------------------------------------------

        return text

    except Exception as e:
        print("Gemini Error:", e)
        raise


def generate_summary(
    question: str,
    top_tool: dict,
    cards: list[dict],
    ai_models: list[dict],
) -> str:
    try:
        user_prompt = build_user_prompt(question, top_tool, cards, ai_models)
        full_prompt = f"{SYSTEM_PROMPT}\n\n{user_prompt}"

        result = call_gemini(full_prompt)
        return result if result else "설명을 생성하지 못했습니다."

    except Exception as e:
        print("Gemini Summary Error:", e)
        return "설명 생성 중 오류가 발생했습니다."


def generate_information_answer(question: str) -> str:
    full_prompt = f"{INFORMATION_PROMPT}\n\n{build_information_prompt(question)}"
    return call_gemini(full_prompt)


def generate_followup_answer(question: str) -> str:
    full_prompt = f"{FOLLOWUP_PROMPT}\n\n{build_followup_prompt(question)}"
    return call_gemini(full_prompt)