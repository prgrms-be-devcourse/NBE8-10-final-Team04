from sentence_transformers import SentenceTransformer
from typing import List

# 앱 시작 시 1번만 모델 로드
model = SentenceTransformer("BAAI/bge-m3")

def create_embedding(text: str) -> List[float]:
    # [방어 로직 추가] 리스트로 들어올 경우를 대비
    if isinstance(text, list):
        text = text[0] if text else ""
    
    if not text or not str(text).strip():
        raise ValueError("임베딩할 텍스트가 비어 있습니다.")

    try:
        # 소문자로 강제 변환하지 않고 사용자가 입력한 '원본'을 최대한 유지합니다.
        # (단, 앞뒤 공백만 제거)
        normalized_text = str(text).strip() 
        
        # bge-m3 공식 가이드에 따라 query: 접두어 유지
        embedding = model.encode(
            f"query: {normalized_text}",
            normalize_embeddings=True
        )
        return embedding.tolist()

    except Exception as e:
        print(f"[Embedding Error] text={text}, error={e}")
        raise RuntimeError("임베딩 생성 중 오류가 발생했습니다.") from e