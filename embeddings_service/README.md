# Local Embeddings Service (FastAPI)

A lightweight HTTP service to generate text embeddings locally using `sentence-transformers` with the `BAAI/bge-small-en-v1.5` model (384-d). It returns TEI-compatible responses so the Java backend can call it directly.

## Endpoints

- `GET /health` → service and model info.
- `POST /embeddings` → Request:

```json
{
  "input": ["text 1", "text 2"],
  "normalize": true
}
```

Response:

```json
{
  "model": "BAAI/bge-small-en-v1.5",
  "dimension": 384,
  "data": [
    {"embedding": [0.01, -0.02, ...]},
    {"embedding": [0.03, 0.04, ...]}
  ]
}
```

## Quick Start (Windows)

1. Create and activate a virtual environment (optional but recommended):

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

2. Install dependencies:

```powershell
pip install -r requirements.txt
```

3. Run the server (default port 8001):

```powershell
uvicorn app:app --host 0.0.0.0 --port 8001
```

Set `EMBEDDINGS_MODEL_NAME` env var if you want to change the model.

## Backend Wiring

Set this in `backend/.env`:

```
HUGGINGFACE_EMBEDDINGS_URL=http://localhost:8001
```

The backend will call `http://localhost:8001/embeddings` with `{"input": ["..."]}`.

## Notes

- First run downloads the model; allow ~100–200MB.
- Uses L2 normalization by default (`normalize=true`) for stable cosine similarity.
- Keep Qdrant collection dimension at 384 to match `bge-small`.
