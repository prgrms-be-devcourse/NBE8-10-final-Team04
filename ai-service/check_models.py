# 사용가능한 모델 확인
import os
import google.generativeai as genai
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()

# 환경 변수에서 API 키 가져오기 (변수명이 다르면 수정하세요)
api_key = os.getenv("GEMINI_API_KEY")

if not api_key:
    print("❌ .env 파일에서 GEMINI_API_KEY를 찾을 수 없습니다. 변수명을 확인해 주세요!")
else:
    # API 설정
    genai.configure(api_key=api_key)

    print("--- 사용 가능한 모델 리스트 ---")
    try:
        for m in genai.list_models():
            if 'generateContent' in m.supported_generation_methods:
                # m.name이 'models/gemini-1.5-flash' 형태입니다.
                print(f"ID: {m.name}  |  이름: {m.display_name}")
    except Exception as e:
        print(f"❌ 에러 발생: {e}")