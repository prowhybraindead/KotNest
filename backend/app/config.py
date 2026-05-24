from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parents[1]
load_dotenv(BASE_DIR / ".env", override=False)
load_dotenv(BASE_DIR / "cloudflare" / ".env", override=False)


def _as_bool(value: str | None, default: bool) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "y", "on"}


@dataclass(frozen=True)
class Settings:
    host: str
    port: int
    database_path: str
    subscription_cache_ttl_seconds: int
    exchange_rate_cache_ttl_seconds: int
    api_token: str
    allow_anonymous_sync: bool
    cors_origins: list[str]
    frankfurter_base_url: str
    open_er_api_base_url: str
    groq_api_base_url: str
    groq_api_key: str
    nvidia_nims_api_base_url: str
    nvidia_nims_api_key: str
    ai_default_provider: str
    ai_default_virtual_model: str
    ai_default_groq_model: str
    ai_default_nims_model: str
    ai_enable_web_search: bool
    ai_web_search_max_results: int
    ai_enable_market_snapshot: bool
    ai_market_snapshot_ttl_seconds: int
    ai_time_zone: str
    ai_enable_vn_fuel_snapshot: bool
    ai_vn_fuel_snapshot_ttl_seconds: int
    vn_fuel_source_url: str
    ai_rules_version: str
    ai_timeout_seconds: int
    ai_brand_name: str
    yahoo_finance_base_url: str
    tunnel_metrics: str
    cloudflare_tunnel_token: str
    tunnel_auto_install_cloudflared: bool
    cloudflared_bin_path: str
    tunnel_enabled: bool


def load_settings() -> Settings:
    raw_origins = os.getenv("CORS_ORIGINS", "").strip()
    origins = [item.strip() for item in raw_origins.split(",") if item.strip()]
    return Settings(
        host=os.getenv("HOST", "0.0.0.0"),
        port=int(os.getenv("PORT", "3000")),
        database_path=os.getenv("DATABASE_PATH", "./data/kotnest.db"),
        subscription_cache_ttl_seconds=int(os.getenv("SUBSCRIPTION_CACHE_TTL_SECONDS", "45")),
        exchange_rate_cache_ttl_seconds=int(os.getenv("EXCHANGE_RATE_CACHE_TTL_SECONDS", "21600")),
        api_token=os.getenv("API_TOKEN", "").strip(),
        allow_anonymous_sync=_as_bool(os.getenv("ALLOW_ANONYMOUS_SYNC"), True),
        cors_origins=origins,
        frankfurter_base_url=os.getenv("FRANKFURTER_BASE_URL", "https://api.frankfurter.dev/v1").rstrip("/"),
        open_er_api_base_url=os.getenv("OPEN_ER_API_BASE_URL", "https://open.er-api.com/v6").rstrip("/"),
        groq_api_base_url=os.getenv("GROQ_API_BASE_URL", "https://api.groq.com/openai/v1").rstrip("/"),
        groq_api_key=os.getenv("GROQ_API_KEY", "").strip(),
        nvidia_nims_api_base_url=os.getenv("NVIDIA_NIMS_API_BASE_URL", "https://integrate.api.nvidia.com/v1").rstrip("/"),
        nvidia_nims_api_key=os.getenv("NVIDIA_NIMS_API_KEY", "").strip(),
        ai_default_provider=os.getenv("AI_DEFAULT_PROVIDER", "groq").strip().lower(),
        ai_default_virtual_model=os.getenv("AI_DEFAULT_VIRTUAL_MODEL", "nesty-atlas-combined-1.0").strip(),
        ai_default_groq_model=os.getenv("AI_DEFAULT_GROQ_MODEL", "llama-3.3-70b-versatile").strip(),
        ai_default_nims_model=os.getenv("AI_DEFAULT_NIMS_MODEL", "google/gemma-3n-e4b-it").strip(),
        ai_enable_web_search=_as_bool(os.getenv("AI_ENABLE_WEB_SEARCH"), True),
        ai_web_search_max_results=int(os.getenv("AI_WEB_SEARCH_MAX_RESULTS", "5")),
        ai_enable_market_snapshot=_as_bool(os.getenv("AI_ENABLE_MARKET_SNAPSHOT"), True),
        ai_market_snapshot_ttl_seconds=int(os.getenv("AI_MARKET_SNAPSHOT_TTL_SECONDS", "120")),
        ai_time_zone=os.getenv("AI_TIME_ZONE", "Asia/Ho_Chi_Minh").strip() or "Asia/Ho_Chi_Minh",
        ai_enable_vn_fuel_snapshot=_as_bool(os.getenv("AI_ENABLE_VN_FUEL_SNAPSHOT"), True),
        ai_vn_fuel_snapshot_ttl_seconds=int(os.getenv("AI_VN_FUEL_SNAPSHOT_TTL_SECONDS", "900")),
        vn_fuel_source_url=os.getenv("VN_FUEL_SOURCE_URL", "https://giaxanghomnay.com/").strip(),
        ai_rules_version=os.getenv("AI_RULES_VERSION", "2026-05-kotnest-safe-finance-v1").strip(),
        ai_timeout_seconds=int(os.getenv("AI_TIMEOUT_SECONDS", "14")),
        ai_brand_name=os.getenv("AI_BRAND_NAME", "Nesty"),
        yahoo_finance_base_url=os.getenv("YAHOO_FINANCE_BASE_URL", "https://query1.finance.yahoo.com").rstrip("/"),
        tunnel_metrics=os.getenv("TUNNEL_METRICS", "127.0.0.1:20241").strip(),
        cloudflare_tunnel_token=os.getenv("CLOUDFLARE_TUNNEL_TOKEN", "").strip(),
        tunnel_auto_install_cloudflared=_as_bool(os.getenv("TUNNEL_AUTO_INSTALL_CLOUDFLARED"), True),
        cloudflared_bin_path=os.getenv(
            "CLOUDFLARED_BIN_PATH",
            "/home/container/.cloudflared/bin/cloudflared",
        ).strip(),
        tunnel_enabled=_as_bool(os.getenv("TUNNEL_ENABLED"), True),
    )
