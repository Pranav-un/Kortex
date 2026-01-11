import os
from typing import List, Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

try:
    from sentence_transformers import SentenceTransformer
except Exception as e:
    raise RuntimeError("Failed to import sentence_transformers. Please install requirements.") from e

# Configuration
MODEL_NAME = os.getenv("EMBEDDINGS_MODEL_NAME", "BAAI/bge-small-en-v1.5")
NORMALIZE_DEFAULT = True

app = FastAPI(title="Local Embeddings Service", version="0.1.0")

# Load model at startup
type_hint_model: Optional[SentenceTransformer] = None

try:
    model = SentenceTransformer(MODEL_NAME)
except Exception as e:
    raise RuntimeError(f"Failed to load model '{MODEL_NAME}'.") from e

# Determine dimension by encoding a dummy string once
try:
    _probe = model.encode(["test"], normalize_embeddings=NORMALIZE_DEFAULT)
    EMBEDDING_DIM = int(len(_probe[0]))
except Exception:
    EMBEDDING_DIM = 384  # sensible default for bge-small


class EmbeddingsRequest(BaseModel):
    # TEI-compatible schema: "input" is a list of texts
    input: List[str]
    normalize: Optional[bool] = NORMALIZE_DEFAULT


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "model": MODEL_NAME,
        "dimension": EMBEDDING_DIM,
    }


@app.post("/embeddings")
async def embeddings(req: EmbeddingsRequest):
    texts = [t for t in req.input if isinstance(t, str) and t.strip()]
    if not texts:
        raise HTTPException(status_code=400, detail="Input must contain at least one non-empty string.")

    try:
        vecs = model.encode(texts, normalize_embeddings=req.normalize)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Encoding failed: {e}")

    # TEI-like response shape: { "data": [{ "embedding": [...] }, ...] }
    data = [{"embedding": v.tolist()} for v in vecs]
    return {
        "model": MODEL_NAME,
        "dimension": EMBEDDING_DIM,
        "data": data,
    }


# Optional root message
@app.get("/")
async def root():
    return {"message": "Embeddings service ready", "model": MODEL_NAME, "dimension": EMBEDDING_DIM}
