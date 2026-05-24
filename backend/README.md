# KotNest Backend (Cloud Sync + AI Bridge)

Backend nay chay doc lap de anh upload len VPS.

## Features

- Cloud sync subscriptions:
  - `GET /api/subscriptions`
  - `POST /api/subscriptions/sync`
- Exchange rate cache + fallback:
  - `GET /api/exchange-rate`
  - Provider order: Frankfurter -> ExchangeRate-API Open Access
- AI bridge with safety filtering:
  - `GET /api/ai/models`
  - `GET /api/ai/insights`
  - `POST /api/ai/chat`
- Web search augmentation via DDGS for trusted economic domains only.

## Run local

```bash
cd backend
python -m venv .venv
# Windows
.venv\Scripts\activate
# Linux/macOS
source .venv/bin/activate

pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 3000
```

Swagger:
- `http://localhost:3000/docs`

## Environment

See `backend/.env.example`.
Cloudflare tunnel vars can be in `backend/.env` or `backend/cloudflare/.env`.

Important vars:
- `API_TOKEN`
- `ALLOW_ANONYMOUS_SYNC`
- `GROQ_API_KEY`
- `NVIDIA_NIMS_API_KEY`
- `AI_DEFAULT_PROVIDER`
- `AI_DEFAULT_GROQ_MODEL`
- `AI_DEFAULT_NIMS_MODEL`
- `AI_ENABLE_WEB_SEARCH`
- `AI_WEB_SEARCH_MAX_RESULTS`
- `CLOUDFLARE_TUNNEL_TOKEN`
- `TUNNEL_METRICS`
- `TUNNEL_AUTO_INSTALL_CLOUDFLARED`
- `CLOUDFLARED_BIN_PATH`

## Cloudflare Tunnel auto-start

When backend starts via `python app/main.py`, it auto-runs `backend/cloudflare/start.sh`
if `TUNNEL_ENABLED=1` and `CLOUDFLARE_TUNNEL_TOKEN` is set.

## AI Provider / Model options

Provider `groq` models:
- `meta-llama/llama-4-scout-17b-16e-instruct`
- `llama-3.3-70b-versatile`
- `groq/compound`
- `qwen/qwen3-32b`

Provider `nvidia_nims` models:
- `mistral-large-3-675b-instruct-2512` (resolved to `mistralai/...`)
- `minimax-m2.7` (resolved to `minimaxai/...`)
- `gemma-3n-e4b-it` (resolved to `google/...`)

## Safety + Privacy design

- Backend strips data to sanitized finance summary before AI provider call.
- Raw local notes/urls are not forwarded to cloud AI providers.
- Input and output are filtered for NSFW/illegal content.
- If provider fails, backend returns local fallback advice.
- Web search only keeps sources from trusted economic domains.

## Android integration notes

App uses build-time env in root `.env`:
- `KOTNEST_BACKEND_BASE_URL`
- `KOTNEST_BACKEND_API_TOKEN`
- `KOTNEST_DEVICE_ID`

After changing these vars, rebuild APK.
