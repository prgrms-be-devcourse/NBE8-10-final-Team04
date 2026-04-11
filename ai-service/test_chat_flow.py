from app.services.recommend_service import handle_chat
import traceback

# 테스트 질문 + 기대 유형
test_cases = [
    # 1. AI 툴 추천
    {
        "question": "자소서 작성에 도움되는 AI 추천해줘",
        "expected_type": "recommendation",
        "expected_target": "ai_model",
    },
    {
        "question": "이미지 생성할 때 쓸만한 AI 뭐 있어?",
        "expected_type": "recommendation",
        "expected_target": "ai_model",
    },

    # 2. 업무 관련 스킬 추천
    {
        "question": "웹사이트를 만들고 싶은데 어떻게 해야하지?",
        "expected_type": "recommendation",
        "expected_target": "skill",
    },
    {
        "question": "공유일기 사이트 만들고 싶어",
        "expected_type": "recommendation",
        "expected_target": "skill",
    },
    {
        "question": "회계 업무 자동화 하고 싶어",
        "expected_type": "recommendation",
        "expected_target": "skill",
    },

    # 3. 정보형 질문
    {
        "question": "제미나이를 쓰려고 하는데 자소서 쓰기 좋아?",
        "expected_type": "information",
        "expected_target": "ai_information",
    },
    {
        "question": "챗 지피티가 최근에 업데이트됐다는데 뭐가 달라?",
        "expected_type": "information",
        "expected_target": "ai_information",
    },
    {
        "question": "RAG가 뭐야?",
        "expected_type": "information",
        "expected_target": "none",
    },

    # 4. 범위 밖 질문
    {
        "question": "오늘 날씨 어때?",
        "expected_type": "out_of_scope",
        "expected_target": "none",
    },
    {
        "question": "연애 상담 해줘",
        "expected_type": "out_of_scope",
        "expected_target": "none",
    },

    # 5. 모호한 질문
    {
        "question": "AI 추천해줘",
        "expected_type": "ambiguous",
        "expected_target": "none",
    },
    {
        "question": "",
        "expected_type": "ambiguous",
        "expected_target": "none",
    },
]


def normalize_enum(value):
    """
    Enum이든 문자열이든 비교 가능하게 통일
    예:
    QuestionType.recommendation -> recommendation
    recommendation -> recommendation
    """
    if value is None:
        return "None"

    value_str = str(value)
    if "." in value_str:
        return value_str.split(".")[-1]
    return value_str


def print_section_title(title):
    print(f"\n[{title}]")


def print_card(card, idx):
    print(f"{idx}. {getattr(card, 'title', '제목 없음')}")
    print(f"   - 타입: {getattr(card, 'type', '없음')}")
    print(f"   - 추천 이유: {getattr(card, 'reason', '없음')}")

    owner = getattr(card, "owner", None)
    star = getattr(card, "star", None)
    description = getattr(card, "description", None)
    uploaded_at = getattr(card, "uploadedAt", None)
    like = getattr(card, "like", None)

    if owner is not None:
        print(f"   - 제작자: {owner}")
    if star is not None:
        print(f"   - 스타 수: {star}")
    if description:
        print(f"   - 설명: {description}")
    if uploaded_at is not None:
        print(f"   - 업로드일: {uploaded_at}")
    if like is not None:
        print(f"   - 찜 수: {like}")


def validate_response(response, expected_type, expected_target):
    actual_type = normalize_enum(getattr(response, "questionType", None))
    actual_target = normalize_enum(getattr(response, "targetType", None))

    print_section_title("기대값 비교")
    print(f"- expected questionType : {expected_type}")
    print(f"- actual   questionType : {actual_type}")
    print(f"- expected targetType   : {expected_target}")
    print(f"- actual   targetType   : {actual_target}")

    type_ok = actual_type == expected_type
    target_ok = actual_target == expected_target

    print(f"- questionType 결과: {'✅ OK' if type_ok else '❌ 불일치'}")
    print(f"- targetType 결과  : {'✅ OK' if target_ok else '❌ 불일치'}")

    return actual_type, actual_target


print("\n🚀 [시스템 테스트 시작] 기획 의도 기준 응답 확인\n")

for i, case in enumerate(test_cases, 1):
    question = case["question"]
    expected_type = case["expected_type"]
    expected_target = case["expected_target"]

    print("=" * 100)
    print(f"TEST CASE #{i}")
    print(f"질문: {repr(question)}")

    try:
        response = handle_chat(question)

        actual_type, actual_target = validate_response(
            response,
            expected_type,
            expected_target
        )

        # 1. 기본 분류 출력
        print_section_title("분류 결과")
        print(f"questionType: {getattr(response, 'questionType', None)}")
        print(f"targetType  : {getattr(response, 'targetType', None)}")

        # 2. 메시지 출력
        print_section_title("응답 메시지")
        message = getattr(response, "message", None)
        if message:
            print(message)
        else:
            print("❌ message 없음")

        # 3. out_of_scope 여부
        out_of_scope = getattr(response, "outOfScope", False)
        if out_of_scope:
            print_section_title("범위 밖 질문 처리")
            print("⚠️ 서비스 범위 밖 질문으로 처리됨")
            next_actions = getattr(response, "nextActions", None)
            print(f"nextActions: {next_actions}")
            print("\n")
            continue

        # 4. 카드 출력
        cards = getattr(response, "cards", []) or []
        print_section_title(f"추천 카드 ({len(cards)}개)")
        if cards:
            for idx, card in enumerate(cards, 1):
                print_card(card, idx)
        else:
            print("카드 없음")

        # 5. topPick 출력
        top_pick = getattr(response, "topPick", None)
        print_section_title("Top Pick")
        if top_pick:
            print(f"- 제목: {getattr(top_pick, 'title', '없음')}")
            print(f"- 이유: {getattr(top_pick, 'reason', '없음')}")

            if hasattr(top_pick, "howToUse"):
                how_to_use = getattr(top_pick, "howToUse", None)
                print(f"- 사용법: {how_to_use if how_to_use else '없음'}")
        else:
            print("없음")

        # 6. nextActions 출력
        print_section_title("다음 행동 버튼")
        next_actions = getattr(response, "nextActions", None)
        print(next_actions if next_actions else "없음")

        # 7. 간단 판정
        print_section_title("간단 판정")
        if actual_type in ["recommendation"]:
            if len(cards) == 0:
                print("❌ 추천형인데 카드가 없음 -> DB 조회 / 추천 로직 확인 필요")
            else:
                print("✅ 추천형 응답으로 보임")

        elif actual_type == "information":
            if message:
                print("✅ 정보형 응답으로 보임")
            else:
                print("❌ 정보형인데 message가 비어 있음")

        elif actual_type == "ambiguous":
            print("✅ 모호한 질문 처리 여부 확인")

        elif actual_type == "out_of_scope":
            print("✅ 범위 밖 질문 처리 여부 확인")

    except Exception:
        print("❌ [ERROR] 테스트 중 오류 발생")
        print(f"질문: {repr(question)}")
        traceback.print_exc()

    print("\n")

print("=" * 100)
print("🏁 모든 테스트 시나리오 종료")