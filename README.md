# KotNest

<div align="center">
  <img src="./public/KotNest_Landscape_Black.svg" alt="KotNest Landscape Black Logo" width="920" />
</div>

<p align="center">
  <a href="#"><img alt="Android" src="https://img.shields.io/badge/Platform-Android-111111?style=for-the-badge&logo=android&logoColor=3DDC84"></a>
  <a href="#"><img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0+-111111?style=for-the-badge&logo=kotlin&logoColor=7F52FF"></a>
  <a href="#"><img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack-Compose-111111?style=for-the-badge&logo=jetpackcompose&logoColor=4285F4"></a>
  <a href="#"><img alt="FastAPI" src="https://img.shields.io/badge/FastAPI-Backend-111111?style=for-the-badge&logo=fastapi&logoColor=00C7B7"></a>
  <a href="#"><img alt="Python" src="https://img.shields.io/badge/Python-3.10+-111111?style=for-the-badge&logo=python&logoColor=FFD43B"></a>
  <a href="#"><img alt="Cloudflare Tunnel" src="https://img.shields.io/badge/Cloudflare-Tunnel-111111?style=for-the-badge&logo=cloudflare&logoColor=F38020"></a>
</p>

<p align="center">
  AI-powered subscription manager with cloud sync, smart insights, market snapshots, and production-ready mobile UX.
</p>

## Highlights

- Modern Android app built with Kotlin + Jetpack Compose.
- Python backend with FastAPI, caching, and SQLite persistence.
- AI chat/insights with provider routing (`nesty`, `groq`, `nvidia_nims`).
- Live market snapshot (gold, oil, crypto, indices) for concrete price answers.
- Vietnam fuel price snapshot endpoint for direct numeric responses.
- Cloudflare tunnel helper for secure public backend exposure.

## Architecture

```text
Android App (Compose)
  |- Local DB (Room)
  |- Datastore Settings
  |- AI Chat UI
  `- Sync Worker
        |
        v
KotNest Backend (FastAPI)
  |- /api/subscriptions + /sync
  |- /api/exchange-rate
  |- /api/market/snapshot
  |- /api/market/vn-fuel-prices
  |- /api/time/now
  `- /api/ai/chat + /insights + /models
        |
        |- Groq / NVIDIA NIMs (OpenAI-compatible)
        |- Frankfurter + ExchangeRate-API
        |- Yahoo quote snapshot
        `- Trusted economic web sources
```

## Repository Layout

```text
app/                    # Android app
backend/                # FastAPI backend
public/                 # Logos and brand assets
```

## Quick Start (Android)

Prerequisites:
- Android Studio (latest stable)
- JDK 21

Steps:
1. Open this project in Android Studio.
2. Create root `.env` from `.env.example`.
3. Fill:
   - `GEMINI_API_KEY`
   - `KOTNEST_BACKEND_BASE_URL` (without `/api`)
   - `KOTNEST_BACKEND_API_TOKEN` (optional if anonymous allowed)
   - `KOTNEST_DEVICE_ID`
4. Build and run on emulator/device.

## Quick Start (Backend)

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

Docs:
- Swagger UI: `http://localhost:3000/docs`

## Core API Endpoints

- Health:
  - `GET /healthz`
  - `GET /health`
- Subscriptions:
  - `GET /api/subscriptions`
  - `POST /api/subscriptions/sync`
- FX:
  - `GET /api/exchange-rate`
- Market + Time:
  - `GET /api/time/now`
  - `GET /api/market/snapshot`
  - `GET /api/market/vn-fuel-prices`
- AI:
  - `GET /api/ai/models`
  - `POST /api/ai/chat`
  - `GET /api/ai/insights`

## AI Routing (Virtual Models)

- `nesty-atlas-combined-1.0`:
  - Uses preferred provider with its default model.
- `nesty-atlas-pro-1.0`:
  - Routes to stronger preset:
    - Groq -> `llama-3.3-70b-versatile`
    - NIMS -> `mistralai/mistral-large-3-675b-instruct-2512`

## Production Notes

- Keep `API_TOKEN` set in production.
- Configure CORS with explicit domains.
- Tune cache TTLs in `backend/.env`.
- Use Cloudflare tunnel vars for secure public access.
- Rebuild APK after changing backend env values used by the app.

## Brand Assets

- Main landscape logo (black): `public/KotNest_Landscape_Black.svg`
- Additional logos available under `public/`.

---

Built with focus on reliability, clean UX, and real-world finance workflows.
