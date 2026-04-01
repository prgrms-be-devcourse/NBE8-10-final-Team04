from app.schemas.chat import ChatResponse, QuestionType, TargetType
from app.services.embedding_service import create_embedding
from app.services.retrieval_service import retrieve_recommendations
from app.services.model_recommend_service import retrieve_ai_models
from app.services.question_type_service import classify_question
from app.services.gemini_service import (
    generate_summary,
    generate_information_answer,
    generate_followup_answer,
)

def handle_chat(question: str) -> ChatResponse:
    # [1] 원본 질문 보존 및 변수 초기화
    original_question = str(question).strip()
    question_embedding = None 

    # [2] 빈 질문 처리
    if not original_question:
        return ChatResponse(
            question=original_question,
            questionType=QuestionType.ambiguous,
            targetType=TargetType.none,
            message="질문을 조금 더 구체적으로 입력해주세요.",
            cards=[],
            topPick=None,
            nextActions=["AI 추천받기", "사용법 물어보기"],
            outOfScope=False
        )

    # [3] 질문 유형 분류 및 데이터 세탁 (가장 중요)
    classification = classify_question(original_question)
    
    def get_clean_str(val):
        # 1. 리스트인 경우 첫 번째 요소 추출
        if isinstance(val, list):
            val = val[0] if val else ""
        # 2. Enum 객체인 경우 .value 추출 (pydantic/enum 대응)
        if hasattr(val, "value"):
            val = val.value
        # 3. 'QuestionType.recommendation' 같은 형태면 마지막 단어만 추출
        return str(val or "").split('.')[-1].strip().lower()

    # 여기서 확실하게 깨끗한 '문자열'로 만듭니다.
    q_type_str = get_clean_str(classification.get("questionType"))
    t_type_str = get_clean_str(classification.get("targetType")) # 오타 방지를 위해 get_clean_str 사용

    # [4] 범위 밖 질문 처리
    if q_type_str == "out_of_scope":
        return ChatResponse(
            question=original_question,
            questionType=QuestionType.out_of_scope,
            targetType=TargetType.none,
            message="이 서비스는 AI 도구와 스킬 추천을 위한 챗봇이에요. AI 활용과 관련된 질문을 해주세요.",
            cards=[],
            topPick=None,
            nextActions=["AI 추천받기", "사용법 보기"],
            outOfScope=True
        )

    # [5] 모호한 질문 처리
    if q_type_str == "ambiguous":
        return ChatResponse(
            question=original_question,
            questionType=QuestionType.ambiguous,
            targetType=TargetType.none,
            message="원하는 용도를 조금 더 알려주시면 더 정확하게 추천할 수 있어요.",
            cards=[],
            topPick=None,
            nextActions=["자소서 작성 AI 추천받기", "이미지 생성 AI 추천받기"],
            outOfScope=False
        )

    # [6] 정보형 질문 처리
    # if q_type_str == "information":
    #     if t_type_str == "ai_information":
    #         ai_models = retrieve_ai_models(original_question)
    #         if ai_models:
    #             top_tool = ai_models[0]
    #             try:
    #                 message = generate_summary(original_question, top_tool, ai_models[:3], ai_models)
    #             except Exception:
    #                 message = f"{top_tool.get('title')}에 대한 정보를 가져왔어요."
                
    #             return ChatResponse(
    #                 question=original_question,
    #                 questionType=QuestionType.information,
    #                 targetType=TargetType.ai_information,
    #                 message=message,
    #                 cards=[],
    #                 topPick=None,
    #                 nextActions=["비슷한 AI 추천받기", "사용법 보기"],
    #                 outOfScope=False
    #             )

    #     try:
    #         message = generate_information_answer(original_question)
    #     except Exception:
    #         message = "설명을 생성하는 중 문제가 발생했어요."

    #     return ChatResponse(
    #         question=original_question,
    #         questionType=QuestionType.information,
    #         targetType=TargetType.none,
    #         message=message,
    #         cards=[],
    #         topPick=None,
    #         nextActions=["관련 AI 추천받기"],
    #         outOfScope=False
    #     )

    if q_type_str == "information":
        # 사용자의 의도: DB를 타지 않고 제미나이가 바로 설명함
        try:
            # build_information_prompt를 사용하는 함수 호출
            message = generate_information_answer(original_question)
        except Exception as e:
            # Gemini가 429 에러 등으로 죽었을 때의 방어 로직
            if "429" in str(e):
                message = "죄송해요, 현재 질문이 너무 많아 잠시 후에 다시 답변해 드릴 수 있어요. 🥲"
            else:
                message = "설명을 생성하는 중 문제가 발생했어요."

        return ChatResponse(
            question=original_question,
            questionType=QuestionType.information,
            targetType=TargetType.none, # DB를 안 타므로 none
            message=message,
            cards=[],
            topPick=None,
            nextActions=["더 자세히 물어보기", "관련 AI 추천받기"],
            outOfScope=False
        )

    # [7] 후속 질문 처리
    if q_type_str == "follow_up":
        try:
            message = generate_followup_answer(original_question)
        except Exception:
            message = "답변 생성 중 문제가 발생했어요."

        return ChatResponse(
            question=original_question,
            questionType=QuestionType.follow_up,
            targetType=TargetType.none,
            message=message,
            cards=[],
            topPick=None,
            nextActions=["다른 AI 추천받기"],
            outOfScope=False
        )

    # [8] 추천 처리 (AI 모델 / 스킬)
    # if t_type_str == "ai_model":
    #     ai_models = retrieve_ai_models(original_question)
    #     if not ai_models:
    #         return ChatResponse(
    #             question=original_question, questionType=QuestionType.recommendation,
    #             targetType=TargetType.ai_model, message="결과가 없어요.", cards=[], nextActions=[], outOfScope=False
    #         )
        
    #     top_tool = ai_models[0]
    #     raw_cards = ai_models[:3]
        
    #     # [수정] 필드 접근 시 에러 방지 (dict인지 확인)
    #     def get_val(obj, key, default=""):
    #         if isinstance(obj, dict): return obj.get(key, default)
    #         return getattr(obj, key, default)

    #     cards = [{
    #         "id": get_val(c, "id"),
    #         "title": str(get_val(c, "title")),
    #         "description": str(get_val(c, "description")),
    #         "reason": str(get_val(c, "reason", "추천된 AI 모델입니다.")),
    #         "type": "ai_model"
    #     } for c in raw_cards]

    #     # ... (이하 ChatResponse 반환 로직)

    if t_type_str == "ai_model":
        ai_models = retrieve_ai_models(original_question)
        
        # [A] 모델 데이터가 있는 경우
        if ai_models:
            top_tool = ai_models[0]
            raw_cards = ai_models[:3]
            
            try:
                message = generate_summary(original_question, top_tool, raw_cards, ai_models)
            except Exception:
                message = f"요청하신 {top_tool.get('title', 'AI 모델')}을(를) 추천해 드립니다."

            # 안전한 카드 매핑
            cards = [{
                "id": c.get("id"),
                "title": str(c.get("title")),
                "description": str(c.get("description", "상세 정보를 확인해보세요.")), # 필수!
                "reason": str(c.get("reason", "적합한 AI 모델입니다.")),
                "type": "ai_model"
            } for c in raw_cards]

            top_pick = {
                "id": top_tool.get("id"),
                "title": str(top_tool.get("title")),
                "description": str(top_tool.get("description", "")),
                "reason": str(top_tool.get("reason", "")),
                "howToUse": str(top_tool.get("howToUse", "공식 홈페이지를 확인해보세요."))
            }

            return ChatResponse(
                question=original_question,
                questionType=QuestionType.recommendation,
                targetType=TargetType.ai_model,
                message=message,
                cards=cards,
                topPick=top_pick,
                nextActions=["다른 AI 추천받기", "상세 설명 보기"],
                outOfScope=False
            )
        
        # [B] 모델 데이터가 없는 경우 (여기서 바로 return하여 skill 로직 차단)
        else:
            return ChatResponse(
                question=original_question,
                questionType=QuestionType.recommendation,
                targetType=TargetType.ai_model,
                message="현재 데이터베이스에 관련 AI 모델 정보가 없습니다. 🥲 다른 키워드로 검색해보시겠어요?",
                cards=[],
                topPick=None,
                nextActions=["다른 AI 물어보기", "스킬 추천받기"],
                outOfScope=False
            )

    # [9] Skill 추천 처리 (로그 찍힌 바로 그 부분!)
    # elif t_type_str == "skill":
    #     question_embedding = create_embedding(original_question)
    #     retrieval_result = retrieve_recommendations(original_question, question_embedding)
    
    # top_tool = retrieval_result.get("top_tool")
    # raw_cards = retrieval_result.get("cards", [])

    # if not top_tool or not raw_cards:
    #     # 데이터가 없을 때의 처리
    #     return ChatResponse(...)

    # # [수정] DB에서 온 리스트 형태의 데이터를 안전하게 딕셔너리로 변환
    # def safe_card(c):
    #     # 만약 c 자체가 리스트라면 첫 번째 요소를 사용 (에러 방지 핵심!)
    #     if isinstance(c, list): c = c[0] if c else {}
    #     return {
    #         "id": c.get("id", 0),
    #         "title": str(c.get("title", "Unknown")),
    #         "description": str(c.get("description", "")),
    #         "reason": str(c.get("reason", "관련 스킬 추천")),
    #         "type": "skill"
    #     }

    # cards = [safe_card(c) for c in raw_cards]
    
    # # top_pick도 안전하게 생성
    # t = top_tool[0] if isinstance(top_tool, list) else top_tool
    # top_pick = {
    #     "id": t.get("id", 0),
    #     "title": str(t.get("title", "")),
    #     "description": str(t.get("description", "")),
    #     "reason": str(t.get("reason", "")),
    #     "howToUse": str(t.get("howToUse", "가이드를 확인하세요."))
    # }

    # # 최종 응답 생성 (여기서 lower() 에러가 날 수 있는 모든 변수를 str로 감쌈)
    # return ChatResponse(
    #     question=str(original_question),
    #     questionType=QuestionType.recommendation,
    #     targetType=TargetType.skill,
    #     message="고객님의 요청에 맞는 스킬을 찾아보았습니다.",
    #     cards=cards,
    #     topPick=top_pick,
    #     nextActions=["사용법 보기", "다른 추천"],
    #     outOfScope=False
    # )

    # [9] Skill 추천 처리
    elif t_type_str == "skill":
        # 1. 임베딩 생성 및 DB 조회
        question_embedding = create_embedding(original_question)
        retrieval_result = retrieve_recommendations(original_question, question_embedding)
        
        top_tool = retrieval_result.get("top_tool")
        raw_cards = retrieval_result.get("cards", [])

        # 데이터가 없을 때의 방어 로직
        if not top_tool or not raw_cards:
            return ChatResponse(
                question=original_question,
                questionType=QuestionType.recommendation,
                targetType=TargetType.skill,
                message="요청하신 업무와 관련된 에이전트 스킬을 찾지 못했어요. 🥲 다른 키워드로 질문해주시겠어요?",
                cards=[],
                topPick=None,
                nextActions=["AI 툴 추천받기", "웹사이트 만들기", "엑셀 자동화"],
                outOfScope=False
            )

        # 2. 카드 목록 생성 (기획안 반영: 별점, 깃허브ID 등)
        def safe_card(c):
            if isinstance(c, list): c = c[0] if c else {}
            return {
                "id": c.get("id", 0),
                "title": str(c.get("title", "Unknown Skill")),
                "description": str(c.get("description", "")),
                "reason": str(c.get("reason", "비전공자도 사용하기 쉬운 스킬이에요!")),
                "starCount": c.get("starCount", 0),
                "githubId": c.get("githubId", "user"),
                "type": "skill"
            }

        cards = [safe_card(c) for c in raw_cards]
        
        # 3. Top Pick 추출 및 메시지 생성
        t = top_tool[0] if isinstance(top_tool, list) else top_tool
        
        try:
            # [중요] 여기서 Gemini가 비전공자용으로 풀어서 설명하도록 함수 호출
            message = generate_summary(original_question, t, raw_cards, []) 
        except Exception:
            message = f"고객님의 업무 자동화를 위해 {t.get('title')} 스킬을 추천해 드립니다!"

        top_pick = {
            "id": t.get("id", 0),
            "title": str(t.get("title", "")),
            "description": str(t.get("description", "")),
            "reason": str(t.get("reason", "")),
            "howToUse": str(t.get("howToUse", "상세 페이지에서 가이드를 확인하세요.")),
            "starCount": t.get("starCount", 0),
            "githubId": t.get("githubId", "user")
        }

        # 4. 최종 응답 반환
        return ChatResponse(
            question=original_question,
            questionType=QuestionType.recommendation,
            targetType=TargetType.skill,
            message=message,
            cards=cards,
            topPick=top_pick,
            nextActions=["AI 툴 추천받기", "웹사이트 만들기", "엑셀 자동화"],
            outOfScope=False
        )