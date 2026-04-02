from sentence_transformers import SentenceTransformer
from typing import List, Union
import torch

# =========================
# 1. 디바이스 자동 설정
# =========================
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

# =========================
# 2. 모델 로드 (앱 시작 시 1회)
# =========================
model = SentenceTransformer("BAAI/bge-m3", device=DEVICE)


# =========================
# 3. 단일 임베딩
# =========================
def create_embedding(text: str) -> List[float]:
    if isinstance(text, list):
        text = text[0] if text else ""

    text = str(text).strip()

    if not text:
        raise ValueError("임베딩할 텍스트가 비어 있음")

    try:
        embedding = model.encode(
            f"query: {text}",
            normalize_embeddings=True
        )

        return embedding.tolist()

    except Exception as e:
        print(f"[Embedding Error] text={text}, error={e}")
        raise RuntimeError("임베딩 생성 실패") from e


# =========================
# 4. batch 임베딩
# =========================
def create_embeddings(texts: List[str]) -> List[List[float]]:
    if not texts:
        return []

    try:
        normalized = [
            f"query: {str(t).strip()}" for t in texts if str(t).strip()
        ]

        embeddings = model.encode(
            normalized,
            normalize_embeddings=True,
            batch_size=32,
        )

        return [e.tolist() for e in embeddings]

    except Exception as e:
        print(f"[Batch Embedding Error] error={e}")
        raise RuntimeError("배치 임베딩 실패") from e