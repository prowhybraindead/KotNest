from __future__ import annotations

import os
import re
import shutil
import subprocess
import time
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import Annotated
from urllib.parse import urlparse
from zoneinfo import ZoneInfo

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException, Query, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

try:
    from .cache import TTLCache
    from .config import load_settings
    from .database import CachedRate, Database
    from .models import (
        AiChatRequest,
        AiChatResponse,
        AiCitation,
        AiInsightResponse,
        AiModelConfigResponse,
        ExchangeRateResponse,
        FuelPriceItem,
        MarketQuote,
        MarketSnapshotResponse,
        Subscription,
        TimeNowResponse,
        VietnamFuelPriceResponse,
    )
except ImportError:
    # Allow running this file directly (e.g., python app/main.py in some panels)
    from cache import TTLCache
    from config import load_settings
    from database import CachedRate, Database
    from models import (  # type: ignore[no-redef]
        AiChatRequest,
        AiChatResponse,
        AiCitation,
        AiInsightResponse,
        AiModelConfigResponse,
        ExchangeRateResponse,
        FuelPriceItem,
        MarketQuote,
        MarketSnapshotResponse,
        Subscription,
        TimeNowResponse,
        VietnamFuelPriceResponse,
    )

try:
    from ddgs import DDGS
except Exception:  # noqa: BLE001
    DDGS = None


settings = load_settings()
database = Database(settings.database_path)
cache = TTLCache(default_ttl_seconds=settings.subscription_cache_ttl_seconds)


@asynccontextmanager
async def app_lifespan(_app: FastAPI):
    _start_cloudflare_tunnel_if_configured()
    yield


app = FastAPI(title="KotNest Backend", version="1.2.0", lifespan=app_lifespan)
bearer_scheme = HTTPBearer(auto_error=False)

GROQ_PROVIDER = "groq"
NIMS_PROVIDER = "nvidia_nims"
NESTY_PROVIDER = "nesty"

NESTY_MODEL_COMBINED = "nesty-atlas-combined-1.0"
NESTY_MODEL_PRO = "nesty-atlas-pro-1.0"
NESTY_VIRTUAL_MODELS = [NESTY_MODEL_COMBINED, NESTY_MODEL_PRO]

GROQ_MODELS = [
    "meta-llama/llama-4-scout-17b-16e-instruct",
    "llama-3.3-70b-versatile",
    "groq/compound",
    "qwen/qwen3-32b",
]

NIMS_MODEL_ALIASES = {
    "mistral-large-3-675b-instruct-2512": "mistralai/mistral-large-3-675b-instruct-2512",
    "minimax-m2.7": "minimaxai/minimax-m2.7",
    "gemma-3n-e4b-it": "google/gemma-3n-e4b-it",
}

TRUSTED_ECONOMIC_DOMAINS = [
    "imf.org",
    "worldbank.org",
    "oecd.org",
    "fred.stlouisfed.org",
    "federalreserve.gov",
    "bls.gov",
    "bea.gov",
    "ecb.europa.eu",
    "bis.org",
    "sbv.gov.vn",
    "gso.gov.vn",
    "mof.gov.vn",
    "chinhphu.vn",
    "tapchitaichinh.vn",
    "thoibaotaichinhvietnam.vn",
    "vneconomy.vn",
    "cafef.vn",
    "vietnambiz.vn",
    "baodautu.vn",
    "vnexpress.net",
    "nhandan.vn",
]

BLOCKED_CONTENT_PATTERNS = [
    r"\bnsfw\b",
    r"\bporn\b",
    r"\bsex\b",
    r"child\s*sexual",
    r"\bcp\b",
    r"\bdrug\s*recipe\b",
    r"\bmake\s*bomb\b",
    r"\bweapon\s*build\b",
    r"hate\s*crime",
    r"kill\s+someone",
    r"terrorist\s+plan",
    r"money\s*launder",
]

SMALL_TALK_PATTERNS = [
    r"^\s*(hi|hello|hey|yo|sup|xin chao|xin chào|chao|chào|helo|alo)\s*!?\s*$",
    r"^\s*(good morning|good afternoon|good evening|buoi sang|buổi sáng|buoi toi|buổi tối)\s*!?\s*$",
    r"^\s*(ban la ai|bạn là ai|who are you|how are you)\s*\??\s*$",
]

FINANCE_HINT_WORDS = {
    "budget",
    "spend",
    "spending",
    "expense",
    "expenses",
    "subscription",
    "renewal",
    "due",
    "bill",
    "bills",
    "finance",
    "financial",
    "inflation",
    "interest",
    "gdp",
    "chi",
    "tieu",
    "chi tieu",
    "chi tiêu",
    "ngan sach",
    "ngân sách",
    "tai chinh",
    "tài chính",
    "hoa don",
    "hóa đơn",
    "gia han",
    "gia hạn",
    "ty gia",
    "tỷ giá",
}

LANGUAGE_HINTS_VI = {
    "xin",
    "chao",
    "chào",
    "ban",
    "bạn",
    "toi",
    "tôi",
    "minh",
    "mình",
    "duoc",
    "được",
    "khong",
    "không",
    "giup",
    "giúp",
    "chi",
    "tiêu",
    "tieu",
    "tai",
    "chinh",
    "tài",
    "chính",
    "ngan",
    "sach",
    "ngân",
    "sách",
}

MARKET_SYMBOLS = {
    "gold_spot_usd": "GC=F",
    "xau_usd_spot": "XAUUSD=X",
    "brent_oil_usd": "BZ=F",
    "wti_oil_usd": "CL=F",
    "btc_usd": "BTC-USD",
    "eth_usd": "ETH-USD",
    "bnb_usd": "BNB-USD",
    "sol_usd": "SOL-USD",
    "xrp_usd": "XRP-USD",
    "ada_usd": "ADA-USD",
    "doge_usd": "DOGE-USD",
    "usdt_usd": "USDT-USD",
    "pepe_usd": "PEPE24478-USD",
    "sp500_index": "^GSPC",
    "nasdaq_index": "^IXIC",
    "vnindex": "^VNINDEX.VN",
    "dji_index": "^DJI",
}

VN_FUEL_KEYWORDS = {
    "xang",
    "xăng",
    "dau",
    "dầu",
    "diesel",
    "petrol",
    "gasoline",
    "ron95",
    "ron 95",
    "e5 ron 92",
    "do 0,05s",
    "do 0.05s",
}

PRICE_LOOKUP_KEYWORDS = {
    "gia",
    "giá",
    "price",
    "bao nhieu",
    "bao nhiêu",
    "bao nhieu tien",
    "quote",
    "market",
    "thị trường",
}

MARKET_QUERY_KEYWORDS: dict[str, set[str]] = {
    "gold_spot_usd": {"vang", "vàng", "gold", "xau"},
    "xau_usd_spot": {"xauusd", "xau/usd"},
    "brent_oil_usd": {"brent", "dau brent", "dầu brent"},
    "wti_oil_usd": {"wti", "dau wti", "dầu wti"},
    "btc_usd": {"btc", "bitcoin"},
    "eth_usd": {"eth", "ethereum"},
    "bnb_usd": {"bnb"},
    "sol_usd": {"sol", "solana"},
    "xrp_usd": {"xrp", "ripple"},
    "ada_usd": {"ada", "cardano"},
    "doge_usd": {"doge", "dogecoin"},
    "usdt_usd": {"usdt", "tether"},
    "pepe_usd": {"pepe"},
    "vnindex": {"vnindex", "vn-index"},
    "sp500_index": {"sp500", "s&p500", "s&p 500"},
    "nasdaq_index": {"nasdaq"},
    "dji_index": {"dow", "dowjones", "djia"},
}


def _settings_ai_time_zone() -> str:
    value = getattr(settings, "ai_time_zone", "Asia/Ho_Chi_Minh")
    text = str(value).strip() if value is not None else ""
    return text or "Asia/Ho_Chi_Minh"


def _settings_market_snapshot_enabled() -> bool:
    value = getattr(settings, "ai_enable_market_snapshot", True)
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}


def _settings_market_snapshot_ttl_seconds() -> int:
    value = getattr(settings, "ai_market_snapshot_ttl_seconds", 120)
    try:
        ttl = int(value)
    except Exception:
        ttl = 120
    return max(20, ttl)


def _settings_yahoo_finance_base_url() -> str:
    value = getattr(settings, "yahoo_finance_base_url", "https://query1.finance.yahoo.com")
    text = str(value).strip() if value is not None else ""
    return (text or "https://query1.finance.yahoo.com").rstrip("/")


def _settings_vn_fuel_enabled() -> bool:
    value = getattr(settings, "ai_enable_vn_fuel_snapshot", True)
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}


def _settings_vn_fuel_ttl_seconds() -> int:
    value = getattr(settings, "ai_vn_fuel_snapshot_ttl_seconds", 900)
    try:
        ttl = int(value)
    except Exception:
        ttl = 900
    return max(60, ttl)


def _settings_vn_fuel_source_url() -> str:
    value = getattr(settings, "vn_fuel_source_url", "https://giaxanghomnay.com/")
    text = str(value).strip() if value is not None else ""
    return text or "https://giaxanghomnay.com/"


if settings.cors_origins:
    allowed_origins = settings.cors_origins
else:
    allowed_origins = ["*"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def _is_authorized(authorization: str | None) -> bool:
    if settings.api_token:
        expected_bearer = f"Bearer {settings.api_token}"
        expected_raw = settings.api_token
        return authorization in {expected_bearer, expected_raw}
    return settings.allow_anonymous_sync


def require_sync_auth(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer_scheme)] = None,
    authorization: Annotated[str | None, Header()] = None,
) -> None:
    resolved_authorization = authorization
    if credentials is not None and credentials.credentials:
        resolved_authorization = f"Bearer {credentials.credentials}"
    if not _is_authorized(resolved_authorization):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized. Provide Authorization header or enable ALLOW_ANONYMOUS_SYNC.",
        )


def _resolve_device_id(query_device_id: str, header_device_id: str | None) -> str:
    if header_device_id and header_device_id.strip():
        return header_device_id.strip()
    return query_device_id


def _has_blocked_content(text: str | None) -> bool:
    if not text:
        return False
    lowered = text.lower()
    return any(re.search(pattern, lowered) is not None for pattern in BLOCKED_CONTENT_PATTERNS)


def _normalize_provider(provider: str | None) -> str:
    normalized = (provider or settings.ai_default_provider or GROQ_PROVIDER).strip().lower()
    if normalized not in {GROQ_PROVIDER, NIMS_PROVIDER}:
        return GROQ_PROVIDER
    return normalized


def _normalize_model(provider: str, model: str | None) -> str:
    if provider == GROQ_PROVIDER:
        target = (model or settings.ai_default_groq_model or "llama-3.3-70b-versatile").strip()
        if target in GROQ_MODELS:
            return target
        return "llama-3.3-70b-versatile"

    target = (model or settings.ai_default_nims_model or "google/gemma-3n-e4b-it").strip()
    if target in NIMS_MODEL_ALIASES:
        return NIMS_MODEL_ALIASES[target]
    if target in NIMS_MODEL_ALIASES.values():
        return target
    return "google/gemma-3n-e4b-it"


def _normalize_virtual_model(model: str | None) -> str:
    target = (model or settings.ai_default_virtual_model or NESTY_MODEL_COMBINED).strip().lower()
    if target in {NESTY_MODEL_COMBINED, "combined", "default", "balanced"}:
        return NESTY_MODEL_COMBINED
    if target in {NESTY_MODEL_PRO, "pro", "professional"}:
        return NESTY_MODEL_PRO
    return NESTY_MODEL_COMBINED


def _provider_has_api_key(provider: str) -> bool:
    _, api_key = _provider_base_url_and_key(provider)
    return bool(api_key)


def _preferred_provider_for_virtual() -> str:
    preferred = _normalize_provider(settings.ai_default_provider)
    if _provider_has_api_key(preferred):
        return preferred
    alternate = NIMS_PROVIDER if preferred == GROQ_PROVIDER else GROQ_PROVIDER
    if _provider_has_api_key(alternate):
        return alternate
    return preferred


def _resolve_runtime_route_from_virtual(model: str | None) -> tuple[str, str, str]:
    virtual_model = _normalize_virtual_model(model)
    if virtual_model == NESTY_MODEL_PRO:
        preferred_provider = _preferred_provider_for_virtual()
        if preferred_provider == GROQ_PROVIDER:
            runtime_model = _normalize_model(GROQ_PROVIDER, "llama-3.3-70b-versatile")
        else:
            runtime_model = _normalize_model(NIMS_PROVIDER, "mistral-large-3-675b-instruct-2512")
        return preferred_provider, runtime_model, virtual_model

    preferred_provider = _preferred_provider_for_virtual()
    runtime_model = _normalize_model(preferred_provider, None)
    return preferred_provider, runtime_model, virtual_model


def _resolve_route(provider: str | None, model: str | None) -> tuple[str, str, str]:
    normalized_provider = (provider or "").strip().lower()
    normalized_model = (model or "").strip().lower()
    if normalized_provider == NESTY_PROVIDER or normalized_model in {
        NESTY_MODEL_COMBINED,
        NESTY_MODEL_PRO,
        "combined",
        "pro",
        "professional",
        "default",
        "balanced",
    }:
        return _resolve_runtime_route_from_virtual(model)

    runtime_provider = _normalize_provider(provider)
    runtime_model = _normalize_model(runtime_provider, model)
    return runtime_provider, runtime_model, runtime_model


def _resolve_fallback_route(runtime_provider: str, requested_model: str) -> tuple[str, str]:
    fallback_provider = NIMS_PROVIDER if runtime_provider == GROQ_PROVIDER else GROQ_PROVIDER
    if requested_model == NESTY_MODEL_PRO:
        if fallback_provider == GROQ_PROVIDER:
            return fallback_provider, _normalize_model(GROQ_PROVIDER, "llama-3.3-70b-versatile")
        return fallback_provider, _normalize_model(NIMS_PROVIDER, "mistral-large-3-675b-instruct-2512")
    return fallback_provider, _normalize_model(fallback_provider, None)


def _provider_base_url_and_key(provider: str) -> tuple[str, str]:
    if provider == GROQ_PROVIDER:
        return settings.groq_api_base_url, settings.groq_api_key
    return settings.nvidia_nims_api_base_url, settings.nvidia_nims_api_key


def _extract_assistant_text(response_json: dict[str, object]) -> str:
    content = (
        response_json.get("choices", [{}])[0]  # type: ignore[index]
        .get("message", {})  # type: ignore[union-attr]
        .get("content", "")  # type: ignore[union-attr]
    )
    if isinstance(content, str):
        return content.strip()
    if isinstance(content, list):
        parts: list[str] = []
        for item in content:
            if isinstance(item, dict):
                text = item.get("text")
                if isinstance(text, str):
                    parts.append(text)
        return "\n".join(parts).strip()
    return ""


def _is_trusted_domain(url: str) -> bool:
    try:
        host = (urlparse(url).hostname or "").lower()
    except Exception:
        return False
    if not host:
        return False
    return any(host == suffix or host.endswith(f".{suffix}") for suffix in TRUSTED_ECONOMIC_DOMAINS)


def _search_economic_sources(query: str, max_results: int) -> list[AiCitation]:
    if not settings.ai_enable_web_search:
        return []
    if DDGS is None:
        return []

    expanded_query = (
        f"{query} economy inflation interest rates GDP Vietnam economy policy market "
        "site:imf.org OR site:worldbank.org OR site:oecd.org OR site:federalreserve.gov "
        "OR site:bls.gov OR site:bea.gov OR site:fred.stlouisfed.org OR site:ecb.europa.eu OR site:bis.org "
        "OR site:sbv.gov.vn OR site:gso.gov.vn OR site:mof.gov.vn OR site:tapchitaichinh.vn "
        "OR site:vneconomy.vn OR site:vietnambiz.vn OR site:baodautu.vn OR site:vnexpress.net"
    )

    citations: list[AiCitation] = []
    seen_urls: set[str] = set()

    try:
        search_results = DDGS().text(
            expanded_query,
            region="wt-wt",
            safesearch="moderate",
            max_results=max(max_results * 4, 12),
            backend="auto",
        )
    except Exception:
        return []

    for item in search_results:
        if not isinstance(item, dict):
            continue
        url = str(item.get("href", "")).strip()
        if not url or url in seen_urls:
            continue
        if not _is_trusted_domain(url):
            continue
        seen_urls.add(url)

        title = str(item.get("title", "")).strip() or "Untitled source"
        snippet = str(item.get("body", "")).strip()
        source = (urlparse(url).hostname or "source").lower()
        citations.append(
            AiCitation(
                title=title[:200],
                url=url,
                snippet=snippet[:500],
                source=source,
            )
        )
        if len(citations) >= max_results:
            break

    return citations


def _build_sanitized_finance_signal(subscriptions: list[dict[str, object]]) -> dict[str, object]:
    total_count = len(subscriptions)
    overdue = 0
    due_today = 0
    upcoming = 0
    paused = 0
    monthly_vnd_estimate = 0.0
    currency_totals: dict[str, float] = {}

    for item in subscriptions:
        status = str(item.get("status", "Upcoming"))
        if status == "Overdue":
            overdue += 1
        elif status == "Due Today":
            due_today += 1
        elif status == "Paused":
            paused += 1
        else:
            upcoming += 1

        amount = float(item.get("amount", 0.0) or 0.0)
        currency = str(item.get("currency", "VND")).upper()
        currency_totals[currency] = currency_totals.get(currency, 0.0) + amount

        billing_cycle = str(item.get("billingCycle", ""))
        if currency == "VND":
            if billing_cycle == "Monthly":
                monthly_vnd_estimate += amount
            elif billing_cycle == "Yearly":
                monthly_vnd_estimate += amount / 12.0
            elif billing_cycle == "Weekly":
                monthly_vnd_estimate += amount * 4.0

    return {
        "totalCount": total_count,
        "statusSummary": {
            "overdue": overdue,
            "dueToday": due_today,
            "upcoming": upcoming,
            "paused": paused,
        },
        "currencyTotals": {k: round(v, 2) for k, v in currency_totals.items()},
        "monthlyVndEstimate": round(monthly_vnd_estimate, 2),
    }


def _detect_response_language(user_message: str, history_messages: list[str] | None = None) -> str:
    text = user_message.strip()
    lowered = text.lower()
    if not lowered and history_messages:
        lowered = " ".join(history_messages[-3:]).lower()

    if re.search(r"[àáạảãăắằẳẵặâấầẩẫậđèéẹẻẽêếềểễệìíịỉĩòóọỏõôốồổỗộơớờởỡợùúụủũưứừửữựỳýỵỷỹ]", lowered):
        return "vi"
    if any(word in lowered.split() for word in LANGUAGE_HINTS_VI):
        return "vi"
    if re.search(r"[\u3040-\u30ff]", lowered):
        return "ja"
    if re.search(r"[\uac00-\ud7af]", lowered):
        return "ko"
    if re.search(r"[\u4e00-\u9fff]", lowered):
        return "zh"
    if re.search(r"[\u0400-\u04FF]", lowered):
        return "ru"
    if re.search(r"[\u0600-\u06FF]", lowered):
        return "ar"
    return "en"


def _language_system_instruction(language_code: str) -> str:
    if language_code == "vi":
        return (
            "Always answer in Vietnamese to match the user's language. "
            "Keep wording natural, friendly, and concise."
        )
    if language_code == "ja":
        return "Always answer in Japanese to match the user's language."
    if language_code == "ko":
        return "Always answer in Korean to match the user's language."
    if language_code == "zh":
        return "Always answer in Chinese to match the user's language."
    if language_code == "ru":
        return "Always answer in Russian to match the user's language."
    if language_code == "ar":
        return "Always answer in Arabic to match the user's language."
    return "Always answer in English to match the user's language."


def _build_local_response_fallback(
    signal: dict[str, object],
    language_code: str,
    *,
    small_talk: bool,
) -> str:
    if small_talk:
        if language_code == "vi":
            return "Chao ban, minh la Nesty. Minh san sang ho tro phan tich chi tieu va toi uu cac khoan gia han cua ban."
        return "Hi, I'm Nesty. I can help you analyze spending and optimize your subscriptions."

    total_count = int(signal.get("totalCount", 0))
    status_summary = signal.get("statusSummary", {})
    overdue = int(status_summary.get("overdue", 0)) if isinstance(status_summary, dict) else 0
    due_today = int(status_summary.get("dueToday", 0)) if isinstance(status_summary, dict) else 0
    monthly_vnd = float(signal.get("monthlyVndEstimate", 0.0))

    if language_code == "vi":
        return (
            f"Ban dang co {total_count} khoan can theo doi. Qua han: {overdue}, den han hom nay: {due_today}. "
            f"Uoc tinh chi phi dinh ky hang thang: {monthly_vnd:,.0f} VND. "
            "Goi y: uu tien xu ly khoan qua han, gom ngay thanh toan, va cat giam cac dich vu it dung."
        )

    return (
        f"You have {total_count} active dues. Overdue: {overdue}, due today: {due_today}. "
        f"Estimated recurring monthly VND load: {monthly_vnd:,.0f}. "
        "Suggestion: clear overdue items first, consolidate billing dates, and trim low-usage services."
    )


def _time_context_payload() -> dict[str, object]:
    tz_name = _settings_ai_time_zone()
    try:
        tz = ZoneInfo(tz_name)
    except Exception:
        tz = timezone.utc
    now_local = datetime.now(tz)
    now_utc = datetime.now(timezone.utc)
    epoch_ms = int(now_utc.timestamp() * 1000)
    return {
        "timezone": tz_name,
        "localDateTime": now_local.isoformat(),
        "utcDateTime": now_utc.isoformat(),
        "epochSeconds": int(now_utc.timestamp()),
        "epochMilliseconds": epoch_ms,
        "dayOfWeek": now_local.strftime("%A"),
        "date": now_local.strftime("%Y-%m-%d"),
        "generatedAt": epoch_ms,
    }


def _fetch_market_snapshot_from_yahoo() -> list[MarketQuote]:
    symbols = ",".join(MARKET_SYMBOLS.values())
    response = httpx.get(
        f"{_settings_yahoo_finance_base_url()}/v7/finance/quote",
        params={"symbols": symbols},
        timeout=10.0,
    )
    response.raise_for_status()
    payload = response.json()
    results = payload.get("quoteResponse", {}).get("result", [])
    if not isinstance(results, list):
        return []

    by_symbol: dict[str, dict[str, object]] = {}
    for item in results:
        if isinstance(item, dict):
            symbol = str(item.get("symbol", "")).strip()
            if symbol:
                by_symbol[symbol] = item

    quotes: list[MarketQuote] = []
    for key, symbol in MARKET_SYMBOLS.items():
        src = by_symbol.get(symbol)
        if not src:
            continue
        price = src.get("regularMarketPrice")
        if not isinstance(price, (int, float)):
            continue
        market_time_value = src.get("regularMarketTime")
        if isinstance(market_time_value, (int, float)):
            market_time = datetime.fromtimestamp(float(market_time_value), timezone.utc).isoformat()
        else:
            market_time = datetime.now(timezone.utc).isoformat()

        quotes.append(
            MarketQuote(
                key=key,
                symbol=symbol,
                name=str(src.get("shortName", "") or src.get("longName", "") or symbol)[:140],
                price=float(price),
                currency=str(src.get("currency", "") or "USD"),
                change=float(src.get("regularMarketChange", 0.0) or 0.0),
                changePercent=float(src.get("regularMarketChangePercent", 0.0) or 0.0),
                marketTime=market_time,
                marketState=str(src.get("marketState", "") or "UNKNOWN"),
                source="Yahoo Finance",
            )
        )

    return quotes


def _get_market_snapshot_context() -> dict[str, object]:
    if not _settings_market_snapshot_enabled():
        return {"enabled": False, "quotes": []}

    cache_key = "market:snapshot"
    mem_cached = cache.get(cache_key)
    if isinstance(mem_cached, dict):
        return mem_cached

    time_payload = _time_context_payload()
    try:
        quotes = _fetch_market_snapshot_from_yahoo()
    except Exception:
        quotes = []

    payload: dict[str, object] = {
        "enabled": True,
        "provider": "Yahoo Finance",
        "timezone": time_payload["timezone"],
        "localDateTime": time_payload["localDateTime"],
        "utcDateTime": time_payload["utcDateTime"],
        "generatedAt": time_payload["generatedAt"],
        "quotes": [item.model_dump() for item in quotes],
    }
    cache.set(cache_key, payload, ttl_seconds=_settings_market_snapshot_ttl_seconds())
    return payload


def _extract_prices_from_line(line: str) -> list[float]:
    candidates = re.findall(r"(?<!\d)(\d{1,3}(?:[.,]\d{3})+|\d{4,6})(?!\d)", line)
    prices: list[float] = []
    for token in candidates:
        normalized = token.replace(".", "").replace(",", "").strip()
        if not normalized.isdigit():
            continue
        value = float(normalized)
        if value < 5000:
            continue
        prices.append(value)
    return prices


def _parse_vn_fuel_prices_from_html(html: str) -> list[FuelPriceItem]:
    cleaned = re.sub(r"<[^>]+>", " ", html)
    cleaned = cleaned.replace("&nbsp;", " ")
    lines = [re.sub(r"\s+", " ", line).strip() for line in cleaned.splitlines()]

    product_aliases: list[tuple[str, tuple[str, ...], str]] = [
        ("Xăng RON 95-III", ("xăng ron 95-iii", "xang ron 95-iii", "ron 95-iii"), "VND/liter"),
        ("Xăng E5 RON 92-II", ("xăng e5 ron 92-ii", "xang e5 ron 92-ii", "e5 ron 92-ii"), "VND/liter"),
        ("DO 0,05S-II", ("do 0,05s-ii", "do 0.05s-ii", "dầu do 0,05s-ii"), "VND/liter"),
        ("Dầu hỏa 2-K", ("dầu hỏa 2-k", "dau hoa 2-k", "dầu ko", "dau ko"), "VND/liter"),
        ("Mazut FO", ("mazut", "fo no2b"), "VND/kg"),
    ]

    extracted: list[FuelPriceItem] = []
    seen_products: set[str] = set()

    for canonical_name, aliases, unit in product_aliases:
        if canonical_name in seen_products:
            continue
        for raw_line in lines:
            line = raw_line.lower()
            if not any(alias in line for alias in aliases):
                continue
            prices = _extract_prices_from_line(raw_line)
            if not prices:
                continue
            region1 = prices[0]
            region2 = prices[1] if len(prices) > 1 else None
            extracted.append(
                FuelPriceItem(
                    product=canonical_name,
                    region1Price=region1,
                    region2Price=region2,
                    unit=unit,
                )
            )
            seen_products.add(canonical_name)
            break

    return extracted


def _fetch_vn_fuel_prices() -> list[FuelPriceItem]:
    response = httpx.get(
        _settings_vn_fuel_source_url(),
        timeout=10.0,
        headers={
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
            )
        },
    )
    response.raise_for_status()
    return _parse_vn_fuel_prices_from_html(response.text)


def _get_vn_fuel_context() -> dict[str, object]:
    if not _settings_vn_fuel_enabled():
        return {"enabled": False, "prices": []}

    cache_key = "market:vn_fuel_prices"
    cached_value = cache.get(cache_key)
    if isinstance(cached_value, dict):
        return cached_value

    time_payload = _time_context_payload()
    try:
        prices = _fetch_vn_fuel_prices()
    except Exception:
        prices = []

    payload: dict[str, object] = {
        "enabled": bool(prices),
        "provider": "giaxanghomnay.com",
        "sourceUrl": _settings_vn_fuel_source_url(),
        "timezone": time_payload["timezone"],
        "localDateTime": time_payload["localDateTime"],
        "generatedAt": time_payload["generatedAt"],
        "prices": [item.model_dump() for item in prices],
    }
    cache.set(cache_key, payload, ttl_seconds=_settings_vn_fuel_ttl_seconds())
    return payload


def _needs_vn_fuel_context(message: str, focus: str | None) -> bool:
    combined = f"{message} {focus or ''}".lower()
    return any(keyword in combined for keyword in VN_FUEL_KEYWORDS)


def _is_price_lookup_message(message: str, focus: str | None) -> bool:
    combined = f"{message} {focus or ''}".lower().strip()
    if not combined:
        return False
    if any(keyword in combined for keyword in PRICE_LOOKUP_KEYWORDS):
        return True
    if _needs_vn_fuel_context(message, focus):
        return True
    return any(any(token in combined for token in tokens) for tokens in MARKET_QUERY_KEYWORDS.values())


def _filter_market_snapshot_for_query(
    market_snapshot: dict[str, object],
    message: str,
    focus: str | None,
) -> dict[str, object]:
    quotes_raw = market_snapshot.get("quotes")
    if not isinstance(quotes_raw, list):
        return market_snapshot

    query = f"{message} {focus or ''}".lower()
    selected_keys: set[str] = set()
    for key, tokens in MARKET_QUERY_KEYWORDS.items():
        if any(token in query for token in tokens):
            selected_keys.add(key)

    # If user asked for price but not a specific asset, keep a compact diversified snapshot.
    if _is_price_lookup_message(message, focus) and not selected_keys:
        selected_keys.update(
            {
                "gold_spot_usd",
                "xau_usd_spot",
                "brent_oil_usd",
                "wti_oil_usd",
                "btc_usd",
                "eth_usd",
                "usdt_usd",
                "vnindex",
                "sp500_index",
            }
        )

    if not selected_keys:
        return market_snapshot

    filtered_quotes: list[dict[str, object]] = []
    for item in quotes_raw:
        if not isinstance(item, dict):
            continue
        key = str(item.get("key", ""))
        if key in selected_keys:
            filtered_quotes.append(item)

    if not filtered_quotes:
        return market_snapshot

    trimmed = dict(market_snapshot)
    trimmed["quotes"] = filtered_quotes
    return trimmed


def _build_system_rules() -> str:
    return (
        f"You are {settings.ai_brand_name}, a legal-safe finance assistant for subscription budgeting. "
        "Only discuss lawful personal finance, budgeting, and macroeconomic context. "
        "Never output NSFW sexual content, hate content, violent wrongdoing, terrorism, fraud, or illegal instructions. "
        "If user sends greeting or casual small-talk, answer naturally and briefly first (without forcing financial analysis). "
        "Avoid robotic openings like 'Given...' unless user asked formal analysis. "
        "If user asks unsafe or illegal topics, refuse briefly and redirect to safe alternatives. "
        "Be concise, factual, and provide practical steps with uncertainty where needed."
    )


def _is_small_talk_message(message: str) -> bool:
    text = message.strip().lower()
    if not text:
        return False
    for pattern in SMALL_TALK_PATTERNS:
        if re.search(pattern, text) is not None:
            return True
    if len(text.split()) <= 4 and not any(word in text for word in FINANCE_HINT_WORDS):
        return True
    return False


def _call_openai_compatible_chat(provider: str, model: str, messages: list[dict[str, str]]) -> str:
    base_url, api_key = _provider_base_url_and_key(provider)
    if not api_key:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"Missing API key for provider: {provider}",
        )

    endpoint = f"{base_url}/chat/completions"
    payload = {
        "model": model,
        "messages": messages,
        "temperature": 0.25,
        "max_tokens": 900,
        "stream": False,
    }
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
    }

    response = httpx.post(
        endpoint,
        json=payload,
        headers=headers,
        timeout=float(settings.ai_timeout_seconds),
    )
    response.raise_for_status()

    text = _extract_assistant_text(response.json())
    if not text:
        raise ValueError("AI provider returned empty text.")
    return text


def _build_prompt_context(
    signal: dict[str, object],
    citations: list[AiCitation],
    focus: str | None,
    time_context: dict[str, object] | None = None,
    market_snapshot: dict[str, object] | None = None,
    vn_fuel_context: dict[str, object] | None = None,
) -> str:
    context_lines = [
        f"Sanitized finance summary: {signal}",
        f"Focus: {(focus or 'general optimization').strip()[:220]}",
    ]

    if time_context:
        context_lines.append(
            "Current time context: "
            f"timezone={time_context.get('timezone')}, "
            f"local={time_context.get('localDateTime')}, "
            f"utc={time_context.get('utcDateTime')}, "
            f"dayOfWeek={time_context.get('dayOfWeek')}"
        )

    if market_snapshot and market_snapshot.get("quotes"):
        context_lines.append("Current market snapshot:")
        raw_quotes = market_snapshot.get("quotes", [])
        if isinstance(raw_quotes, list):
            for item in raw_quotes[:14]:
                if not isinstance(item, dict):
                    continue
                context_lines.append(
                    f"- {item.get('key')}: {item.get('price')} {item.get('currency')} "
                    f"(change {item.get('change')} / {item.get('changePercent')}%) "
                    f"symbol={item.get('symbol')} source={item.get('source')}"
                )

    if vn_fuel_context and vn_fuel_context.get("prices"):
        context_lines.append(
            "Vietnam fuel price snapshot "
            f"(source={vn_fuel_context.get('provider')}, url={vn_fuel_context.get('sourceUrl')}, "
            f"local={vn_fuel_context.get('localDateTime')}):"
        )
        raw_prices = vn_fuel_context.get("prices", [])
        if isinstance(raw_prices, list):
            for item in raw_prices[:10]:
                if not isinstance(item, dict):
                    continue
                context_lines.append(
                    f"- {item.get('product')}: region1={item.get('region1Price')} "
                    f"region2={item.get('region2Price')} unit={item.get('unit')}"
                )

    if citations:
        context_lines.append("Trusted economic web references:")
        for idx, c in enumerate(citations, 1):
            context_lines.append(f"[{idx}] {c.title} | {c.source} | {c.url}")
            if c.snippet:
                context_lines.append(f"snippet: {c.snippet}")

    context_lines.append(
        "Return practical budget advice. If user asks for prices, prioritize concrete numbers from provided market/fuel snapshots. "
        "Mention source numbers [1], [2] when using web references."
    )
    return "\n".join(context_lines)


@app.get("/healthz")
def healthz() -> dict[str, str]:
    return {"status": "ok", "service": "kotnest-backend", "time": str(int(time.time()))}


@app.get("/")
def root_health() -> dict[str, str]:
    return healthz()


@app.get("/health")
def health() -> dict[str, str]:
    return healthz()


@app.get("/api/time/now", response_model=TimeNowResponse)
def get_time_now() -> TimeNowResponse:
    payload = _time_context_payload()
    return TimeNowResponse(
        timezone=str(payload["timezone"]),
        localDateTime=str(payload["localDateTime"]),
        utcDateTime=str(payload["utcDateTime"]),
        epochSeconds=int(payload["epochSeconds"]),
        epochMilliseconds=int(payload["epochMilliseconds"]),
        dayOfWeek=str(payload["dayOfWeek"]),
        date=str(payload["date"]),
    )


@app.get("/api/market/snapshot", response_model=MarketSnapshotResponse)
def get_market_snapshot(_authorized: None = Depends(require_sync_auth)) -> MarketSnapshotResponse:
    context = _get_market_snapshot_context()
    quotes_raw = context.get("quotes", [])
    quotes: list[MarketQuote] = []
    if isinstance(quotes_raw, list):
        for item in quotes_raw:
            if isinstance(item, dict):
                try:
                    quotes.append(MarketQuote(**item))
                except Exception:
                    continue
    return MarketSnapshotResponse(
        provider=str(context.get("provider", "Yahoo Finance")),
        timezone=str(context.get("timezone", _settings_ai_time_zone())),
        localDateTime=str(context.get("localDateTime", "")),
        utcDateTime=str(context.get("utcDateTime", "")),
        generatedAt=int(context.get("generatedAt", int(time.time() * 1000))),
        cached=bool(cache.get("market:snapshot") is not None),
        quotes=quotes,
    )


@app.get("/api/market/vn-fuel-prices", response_model=VietnamFuelPriceResponse)
def get_vn_fuel_prices(_authorized: None = Depends(require_sync_auth)) -> VietnamFuelPriceResponse:
    context = _get_vn_fuel_context()
    prices_raw = context.get("prices", [])
    prices: list[FuelPriceItem] = []
    if isinstance(prices_raw, list):
        for item in prices_raw:
            if isinstance(item, dict):
                try:
                    prices.append(FuelPriceItem(**item))
                except Exception:
                    continue

    time_payload = _time_context_payload()
    return VietnamFuelPriceResponse(
        provider=str(context.get("provider", "giaxanghomnay.com")),
        sourceUrl=str(context.get("sourceUrl", _settings_vn_fuel_source_url())),
        timezone=str(context.get("timezone", time_payload["timezone"])),
        localDateTime=str(context.get("localDateTime", time_payload["localDateTime"])),
        generatedAt=int(context.get("generatedAt", time_payload["generatedAt"])),
        cached=bool(cache.get("market:vn_fuel_prices") is not None),
        prices=prices,
    )


@app.get("/api/subscriptions", response_model=list[Subscription])
def get_subscriptions(
    device_id: Annotated[str, Query(min_length=1)] = "default-device",
    x_device_id: Annotated[str | None, Header(alias="X-Device-Id")] = None,
) -> list[Subscription]:
    resolved_device_id = _resolve_device_id(device_id, x_device_id)
    cache_key = f"subs:{resolved_device_id}"
    cached_value = cache.get(cache_key)
    if cached_value is not None:
        return cached_value

    subscriptions = database.get_subscriptions(resolved_device_id)
    cache.set(cache_key, subscriptions, ttl_seconds=settings.subscription_cache_ttl_seconds)
    return subscriptions


@app.post("/api/subscriptions/sync", status_code=status.HTTP_204_NO_CONTENT)
def sync_subscriptions(
    subscriptions: list[Subscription],
    device_id: Annotated[str, Query(min_length=1)] = "default-device",
    x_device_id: Annotated[str | None, Header(alias="X-Device-Id")] = None,
    _authorized: None = Depends(require_sync_auth),
) -> Response:
    resolved_device_id = _resolve_device_id(device_id, x_device_id)
    try:
        database.sync_subscriptions(
            device_id=resolved_device_id,
            subscriptions=[item.model_dump() for item in subscriptions],
        )
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc

    cache.invalidate(f"subs:{resolved_device_id}")
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.get("/api/exchange-rate", response_model=ExchangeRateResponse)
def get_exchange_rate(
    base: Annotated[str, Query(min_length=3, max_length=3)] = "USD",
    target: Annotated[str, Query(min_length=3, max_length=3)] = "VND",
) -> ExchangeRateResponse:
    base_currency = base.upper()
    target_currency = target.upper()
    cache_key = f"fx:{base_currency}:{target_currency}"

    mem_cached = cache.get(cache_key)
    if mem_cached is not None:
        return ExchangeRateResponse(**mem_cached, cached=True)

    db_cached = database.get_cached_rate(
        base_currency=base_currency,
        target_currency=target_currency,
        max_age_seconds=settings.exchange_rate_cache_ttl_seconds,
    )
    if db_cached is not None:
        payload = {
            "baseCurrency": db_cached.base_currency,
            "targetCurrency": db_cached.target_currency,
            "rate": db_cached.rate,
            "sourceDate": db_cached.source_date,
            "provider": db_cached.provider,
        }
        cache.set(cache_key, payload, ttl_seconds=settings.exchange_rate_cache_ttl_seconds)
        return ExchangeRateResponse(**payload, cached=True)

    upstream = _fetch_rate_from_upstream(base_currency=base_currency, target_currency=target_currency)
    cached_rate = CachedRate(
        base_currency=base_currency,
        target_currency=target_currency,
        rate=upstream["rate"],
        source_date=upstream["sourceDate"],
        provider=upstream["provider"],
        fetched_at=int(time.time()),
    )
    database.upsert_rate(cached_rate)
    cache.set(cache_key, upstream, ttl_seconds=settings.exchange_rate_cache_ttl_seconds)
    return ExchangeRateResponse(**upstream, cached=False)


@app.get("/api/ai/models", response_model=AiModelConfigResponse)
def get_ai_models(_authorized: None = Depends(require_sync_auth)) -> AiModelConfigResponse:
    default_provider = NESTY_PROVIDER
    default_model = _normalize_virtual_model(settings.ai_default_virtual_model)
    provider_models = {
        NESTY_PROVIDER: NESTY_VIRTUAL_MODELS,
    }
    return AiModelConfigResponse(
        brandName=settings.ai_brand_name,
        rulesVersion=settings.ai_rules_version,
        defaultProvider=default_provider,
        defaultModel=default_model,
        providers=provider_models,
    )


@app.post("/api/ai/chat", response_model=AiChatResponse)
def chat_with_ai(
    request: AiChatRequest,
    device_id: Annotated[str, Query(min_length=1)] = "default-device",
    x_device_id: Annotated[str | None, Header(alias="X-Device-Id")] = None,
    _authorized: None = Depends(require_sync_auth),
) -> AiChatResponse:
    now_ms = int(time.time() * 1000)
    resolved_device_id = _resolve_device_id(device_id, x_device_id)
    user_message = request.message.strip()[:1800]
    history_messages = [item.content for item in request.history if item.role == "user" and item.content]
    detected_language = _detect_response_language(user_message, history_messages)
    is_price_lookup = _is_price_lookup_message(user_message, request.focus)
    is_small_talk = _is_small_talk_message(user_message) and not is_price_lookup

    if _has_blocked_content(request.message) or _has_blocked_content(request.focus):
        blocked_answer = "Request blocked by safety policy. Ask about legal personal finance or economic planning."
        if detected_language == "vi":
            blocked_answer = "Yeu cau bi chan boi chinh sach an toan. Ban hay hoi ve tai chinh ca nhan hop phap."
        return AiChatResponse(
            brandName=settings.ai_brand_name,
            provider="filtered",
            model="filtered",
            filtered=True,
            rulesVersion=settings.ai_rules_version,
            answer=blocked_answer,
            citations=[],
            generatedAt=now_ms,
        )

    runtime_provider, runtime_model, requested_model = _resolve_route(
        provider=request.provider,
        model=request.model,
    )

    subscriptions = database.get_subscriptions(resolved_device_id)
    signal = _build_sanitized_finance_signal(subscriptions=subscriptions)
    time_context = _time_context_payload()
    market_snapshot = _get_market_snapshot_context() if not is_small_talk else {"enabled": False, "quotes": []}
    if market_snapshot.get("quotes"):
        market_snapshot = _filter_market_snapshot_for_query(
            market_snapshot=market_snapshot,
            message=user_message,
            focus=request.focus,
        )
    vn_fuel_context = (
        _get_vn_fuel_context() if (not is_small_talk and _needs_vn_fuel_context(user_message, request.focus)) else None
    )

    web_enabled = bool(request.enableWebSearch and settings.ai_enable_web_search and not is_small_talk)
    citations: list[AiCitation] = []
    if web_enabled:
        citations = _search_economic_sources(
            query=request.message,
            max_results=max(1, settings.ai_web_search_max_results),
        )
    if is_price_lookup and market_snapshot.get("quotes"):
        citations.append(
            AiCitation(
                title="Yahoo Finance Market Snapshot",
                url=f"{_settings_yahoo_finance_base_url()}/v7/finance/quote",
                snippet="Internal snapshot used for gold/oil/crypto/index reference prices.",
                source="query1.finance.yahoo.com",
            )
        )
    if vn_fuel_context and vn_fuel_context.get("prices"):
        citations.append(
            AiCitation(
                title="Vietnam Fuel Price Snapshot",
                url=str(vn_fuel_context.get("sourceUrl", _settings_vn_fuel_source_url())),
                snippet="Internal snapshot for Vietnam retail fuel prices (region 1/2).",
                source="giaxanghomnay.com",
            )
        )

    sanitized_history: list[dict[str, str]] = []
    for item in request.history[-8:]:
        if item.role not in {"user", "assistant"}:
            continue
        if _has_blocked_content(item.content):
            continue
        sanitized_history.append(
            {
                "role": item.role,
                "content": item.content.strip()[:1500],
            }
        )

    if is_small_talk:
        messages = [
            {"role": "system", "content": _build_system_rules()},
            {"role": "system", "content": _language_system_instruction(detected_language)},
            {
                "role": "system",
                "content": (
                    "For greetings/small-talk: respond warmly in 1-3 short sentences, then offer finance help. "
                    "Do not force spending analysis if user did not ask."
                ),
            },
            {"role": "user", "content": user_message},
        ]
    else:
        prompt_context = _build_prompt_context(
            signal=signal,
            citations=citations,
            focus=request.focus,
            time_context=time_context,
            market_snapshot=market_snapshot,
            vn_fuel_context=vn_fuel_context,
        )
        messages = [
            {"role": "system", "content": _build_system_rules()},
            {"role": "system", "content": _language_system_instruction(detected_language)},
            {
                "role": "system",
                "content": (
                    "Do not expose private raw records. Use only aggregated/sanitized finance context from backend. "
                    "If economic data is uncertain, say uncertainty explicitly. "
                    "Use provided time and market snapshot context when relevant. "
                    "If Vietnam fuel snapshot numbers are present, use those concrete values instead of vague statements."
                ),
            },
            *(
                [
                    {
                        "role": "system",
                        "content": (
                            "When user asks for current prices, answer with concrete numeric values from provided context, "
                            "include unit/currency, and mention the snapshot time."
                        ),
                    }
                ]
                if is_price_lookup
                else []
            ),
            *sanitized_history,
            {
                "role": "user",
                "content": (
                    f"User message: {user_message}\n"
                    f"Backend context:\n{prompt_context}"
                ),
            },
        ]

    try:
        answer = _call_openai_compatible_chat(
            provider=runtime_provider,
            model=runtime_model,
            messages=messages,
        )
    except Exception:
        fallback_answer_text: str | None = None
        fallback_provider, fallback_model = _resolve_fallback_route(runtime_provider, requested_model)
        try:
            answer = _call_openai_compatible_chat(
                provider=fallback_provider,
                model=fallback_model,
                messages=messages,
            )
            runtime_provider = fallback_provider
            runtime_model = fallback_model
        except Exception:
            fallback_answer_text = _build_local_response_fallback(
                signal=signal,
                language_code=detected_language,
                small_talk=is_small_talk,
            )

        if fallback_answer_text is not None:
            return AiChatResponse(
                brandName=settings.ai_brand_name,
                provider=(
                    NESTY_PROVIDER
                    if requested_model in NESTY_VIRTUAL_MODELS
                    else "local_fallback"
                ),
                model=(
                    requested_model
                    if requested_model in NESTY_VIRTUAL_MODELS
                    else "local_rules"
                ),
                filtered=False,
                rulesVersion=settings.ai_rules_version,
                answer=fallback_answer_text,
                citations=citations,
                generatedAt=now_ms,
            )

    if _has_blocked_content(answer):
        answer = _build_local_response_fallback(
            signal=signal,
            language_code=detected_language,
            small_talk=is_small_talk,
        )
        filtered = True
    else:
        filtered = False

    return AiChatResponse(
        brandName=settings.ai_brand_name,
        provider=NESTY_PROVIDER if requested_model in NESTY_VIRTUAL_MODELS else runtime_provider,
        model=requested_model if requested_model in NESTY_VIRTUAL_MODELS else runtime_model,
        filtered=filtered,
        rulesVersion=settings.ai_rules_version,
        answer=answer,
        citations=citations,
        generatedAt=now_ms,
    )


@app.get("/api/ai/insights", response_model=AiInsightResponse)
def get_ai_insights(
    device_id: Annotated[str, Query(min_length=1)] = "default-device",
    x_device_id: Annotated[str | None, Header(alias="X-Device-Id")] = None,
    focus: Annotated[str | None, Query()] = None,
    provider: Annotated[str | None, Query()] = None,
    model: Annotated[str | None, Query()] = None,
    _authorized: None = Depends(require_sync_auth),
) -> AiInsightResponse:
    if _has_blocked_content(focus):
        return AiInsightResponse(
            brandName=settings.ai_brand_name,
            provider="filtered",
            model="filtered",
            filtered=True,
            insight="Request blocked by policy filters due to unsafe content intent.",
            actions=[
                "Use neutral finance goals only.",
                "Avoid NSFW or illegal topics.",
                "Ask about legal budgeting improvements.",
            ],
            generatedAt=int(time.time() * 1000),
        )

    chat_result = chat_with_ai(
        request=AiChatRequest(
            provider=provider,
            model=model,
            message="Give concise spending optimization insights for this profile.",
            history=[],
            focus=focus,
            enableWebSearch=False,
        ),
        device_id=device_id,
        x_device_id=x_device_id,
        _authorized=None,
    )

    actions = [
        "Prioritize overdue dues first.",
        "Consolidate renewals to fewer billing dates.",
        "Review low-usage subscriptions every month.",
    ]

    return AiInsightResponse(
        brandName=chat_result.brandName,
        provider=chat_result.provider,
        model=chat_result.model,
        filtered=chat_result.filtered,
        insight=chat_result.answer,
        actions=actions,
        generatedAt=chat_result.generatedAt,
    )


def _fetch_rate_from_upstream(base_currency: str, target_currency: str) -> dict[str, str | float]:
    primary_error: Exception | None = None
    try:
        return _fetch_rate_from_frankfurter(base_currency=base_currency, target_currency=target_currency)
    except Exception as exc:  # noqa: BLE001
        primary_error = exc

    try:
        return _fetch_rate_from_open_er_api(base_currency=base_currency, target_currency=target_currency)
    except Exception as fallback_exc:  # noqa: BLE001
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Upstream providers failed. primary={primary_error}; fallback={fallback_exc}",
        ) from fallback_exc


def _fetch_rate_from_frankfurter(base_currency: str, target_currency: str) -> dict[str, str | float]:
    response = httpx.get(
        f"{settings.frankfurter_base_url}/latest",
        params={"base": base_currency, "symbols": target_currency},
        timeout=8.0,
    )
    response.raise_for_status()
    payload = response.json()

    rates = payload.get("rates", {})
    rate = rates.get(target_currency)
    if rate is None:
        raise ValueError(f"Missing exchange rate for {target_currency} from Frankfurter.")

    source_date = str(payload.get("date", "")) or time.strftime("%Y-%m-%d")
    return {
        "baseCurrency": base_currency,
        "targetCurrency": target_currency,
        "rate": float(rate),
        "sourceDate": source_date,
        "provider": "Frankfurter API",
    }


def _fetch_rate_from_open_er_api(base_currency: str, target_currency: str) -> dict[str, str | float]:
    response = httpx.get(
        f"{settings.open_er_api_base_url}/latest/USD",
        timeout=8.0,
    )
    response.raise_for_status()
    payload = response.json()

    if payload.get("result") != "success":
        raise ValueError(f"Open ER API returned non-success result: {payload.get('result')}")

    rates: dict[str, float] = payload.get("rates", {})
    target_to_usd = rates.get(target_currency)
    base_to_usd = rates.get(base_currency)
    if target_to_usd is None or base_to_usd is None or base_to_usd == 0:
        raise ValueError(f"Open ER API missing rate(s) for {base_currency} or {target_currency}.")

    source_date = str(payload.get("time_last_update_utc", "")).strip()
    if not source_date:
        source_date = time.strftime("%Y-%m-%d")

    return {
        "baseCurrency": base_currency,
        "targetCurrency": target_currency,
        "rate": float(target_to_usd) / float(base_to_usd),
        "sourceDate": source_date,
        "provider": "ExchangeRate-API Open Access",
    }


def _start_cloudflare_tunnel_if_configured() -> None:
    if not settings.tunnel_enabled:
        print("[cloudflare] Skip: TUNNEL_ENABLED is disabled.")
        return
    if not settings.cloudflare_tunnel_token:
        print("[cloudflare] Skip: CLOUDFLARE_TUNNEL_TOKEN is empty.")
        return

    backend_dir = Path(__file__).resolve().parents[1]
    script_path = backend_dir / "cloudflare" / "start.sh"
    if not script_path.exists():
        print(f"[cloudflare] Skip: start script missing at {script_path}")
        return

    env = os.environ.copy()
    env["CLOUDFLARE_TUNNEL_TOKEN"] = settings.cloudflare_tunnel_token
    env["TUNNEL_METRICS"] = settings.tunnel_metrics
    env["TUNNEL_AUTO_INSTALL_CLOUDFLARED"] = "1" if settings.tunnel_auto_install_cloudflared else "0"
    env["CLOUDFLARED_BIN_PATH"] = settings.cloudflared_bin_path

    shell_runner = shutil.which("bash") or shutil.which("sh")
    if shell_runner is None:
        print("[cloudflare] Skip: no shell runner found (bash/sh).")
        return

    print(
        "[cloudflare] Starting tunnel helper "
        f"(metrics={settings.tunnel_metrics}, bin={settings.cloudflared_bin_path})"
    )
    subprocess.Popen(  # noqa: S603
        [shell_runner, str(script_path)],  # noqa: S607
        cwd=str(backend_dir),
        env=env,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        start_new_session=True,
    )
    print("[cloudflare] Tunnel helper launched.")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host=settings.host, port=settings.port)
