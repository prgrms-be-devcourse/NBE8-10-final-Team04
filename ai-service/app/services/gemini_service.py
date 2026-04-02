from app.core.prompt import (
    SYSTEM_PROMPT,
    build_chat_prompt,
    build_information_prompt,
    build_followup_prompt,
)

from dotenv import load_dotenv
import google.generativeai as genai
import os

# =========================
# 1. 초기 설정
# =========================
load_dotenv()

api_key = os.getenv("GEMINI_API_KEY")
if not api_key:
    raise ValueError("GEMINI_API_KEY가 설정되지 않았습니다.")

genai.configure(api_key=api_key)


# =========================
# 2. 모델 설정 (단일화)
# =========================
generation_config = {
    "temperature": 0.7,
    "top_p": 0.95,
    "top_k": 40,
    "max_output_tokens": 1024,
    "response_mime_type": "text/plain",
}

model = genai.GenerativeModel(
    model_name="gemini-3.1-flash-lite-preview",
    generation_config=generation_config
)


# =========================
# 3. 공통 유틸
# =========================
def _safe_text(value) -> str:
    if isinstance(value, list):
        value = value[0] if value else ""
    if value is None:
        return ""
    return str(value).strip()


# =========================
# 4. Gemini 호출 (핵심)
# =========================
def call_gemini(prompt: str) -> str:
    try:
        response = model.generate_content(prompt)

        text = _safe_text(getattr(response, "text", ""))

        print("\n" + "="*50)
        print("[Gemini Response]")
        print(text)
        print("="*50 + "\n")

        return text

    except Exception as e:
        print("Gemini Error:", e)
        raise


# =========================
# 5. Chat 응답 생성
# =========================
def generate_summary(
    question: str,
    top_tool: dict,
    cards: list[dict],
    ai_models: list[dict],
) -> str:
    try:
        prompt = SYSTEM_PROMPT + "\n\n" + build_chat_prompt(
            question, top_tool, cards, ai_models
        )
        result = call_gemini(prompt)

        return result if result else "설명을 생성하지 못했습니다."

    except Exception as e:
        print("Summary Error:", e)
        return "설명 생성 중 오류가 발생했습니다."


# =========================
# 6. 정보형
# =========================
def generate_information_answer(question: str) -> str:
    try:
        prompt = SYSTEM_PROMPT + "\n\n" + build_information_prompt(question)
        return call_gemini(prompt)
    except:
        return "설명 생성 중 오류가 발생했습니다."


# =========================
# 7. 후속 질문
# =========================
def generate_followup_answer(question: str) -> str:
    try:
        prompt = SYSTEM_PROMPT + "\n\n" + build_followup_prompt(question)
        return call_gemini(prompt)
    except:
        return "답변 생성 중 문제가 발생했습니다."