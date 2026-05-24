from __future__ import annotations

import json
import os
import sqlite3
import time
from contextlib import contextmanager
from dataclasses import dataclass
from typing import Any, Iterator


@dataclass
class CachedRate:
    base_currency: str
    target_currency: str
    rate: float
    source_date: str
    provider: str
    fetched_at: int


class Database:
    def __init__(self, db_path: str) -> None:
        self._db_path = db_path
        parent = os.path.dirname(os.path.abspath(db_path))
        if parent:
            os.makedirs(parent, exist_ok=True)
        self._init_db()

    @contextmanager
    def _connect(self) -> Iterator[sqlite3.Connection]:
        conn = sqlite3.connect(self._db_path)
        conn.row_factory = sqlite3.Row
        try:
            yield conn
            conn.commit()
        finally:
            conn.close()

    def _init_db(self) -> None:
        with self._connect() as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS subscriptions (
                    device_id TEXT NOT NULL,
                    subscription_id INTEGER NOT NULL,
                    payload TEXT NOT NULL,
                    synced_at INTEGER NOT NULL,
                    PRIMARY KEY (device_id, subscription_id)
                )
                """
            )
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS exchange_rates (
                    base_currency TEXT NOT NULL,
                    target_currency TEXT NOT NULL,
                    rate REAL NOT NULL,
                    source_date TEXT NOT NULL,
                    provider TEXT NOT NULL,
                    fetched_at INTEGER NOT NULL,
                    PRIMARY KEY (base_currency, target_currency)
                )
                """
            )

    def get_subscriptions(self, device_id: str) -> list[dict[str, Any]]:
        with self._connect() as conn:
            rows = conn.execute(
                """
                SELECT payload
                FROM subscriptions
                WHERE device_id = ?
                ORDER BY subscription_id ASC
                """,
                (device_id,),
            ).fetchall()
        return [json.loads(row["payload"]) for row in rows]

    def sync_subscriptions(self, device_id: str, subscriptions: list[dict[str, Any]]) -> int:
        now_ms = int(time.time() * 1000)
        incoming_ids: list[int] = []

        with self._connect() as conn:
            if not subscriptions:
                conn.execute("DELETE FROM subscriptions WHERE device_id = ?", (device_id,))
                return 0

            for item in subscriptions:
                sub_id = int(item.get("id", 0))
                if sub_id <= 0:
                    raise ValueError("Each subscription must contain id > 0.")

                payload = json.dumps(item, ensure_ascii=True, separators=(",", ":"))
                incoming_ids.append(sub_id)
                conn.execute(
                    """
                    INSERT INTO subscriptions (device_id, subscription_id, payload, synced_at)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(device_id, subscription_id) DO UPDATE SET
                        payload = excluded.payload,
                        synced_at = excluded.synced_at
                    """,
                    (device_id, sub_id, payload, now_ms),
                )

            placeholders = ",".join(["?"] * len(incoming_ids))
            conn.execute(
                f"""
                DELETE FROM subscriptions
                WHERE device_id = ?
                  AND subscription_id NOT IN ({placeholders})
                """,
                [device_id, *incoming_ids],
            )
        return len(incoming_ids)

    def get_cached_rate(self, base_currency: str, target_currency: str, max_age_seconds: int) -> CachedRate | None:
        min_fetched_at = int(time.time()) - max(max_age_seconds, 1)
        with self._connect() as conn:
            row = conn.execute(
                """
                SELECT base_currency, target_currency, rate, source_date, provider, fetched_at
                FROM exchange_rates
                WHERE base_currency = ?
                  AND target_currency = ?
                  AND fetched_at >= ?
                LIMIT 1
                """,
                (base_currency, target_currency, min_fetched_at),
            ).fetchone()

        if row is None:
            return None

        return CachedRate(
            base_currency=row["base_currency"],
            target_currency=row["target_currency"],
            rate=float(row["rate"]),
            source_date=row["source_date"],
            provider=row["provider"],
            fetched_at=int(row["fetched_at"]),
        )

    def upsert_rate(self, cached_rate: CachedRate) -> None:
        with self._connect() as conn:
            conn.execute(
                """
                INSERT INTO exchange_rates
                (base_currency, target_currency, rate, source_date, provider, fetched_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(base_currency, target_currency) DO UPDATE SET
                    rate = excluded.rate,
                    source_date = excluded.source_date,
                    provider = excluded.provider,
                    fetched_at = excluded.fetched_at
                """,
                (
                    cached_rate.base_currency,
                    cached_rate.target_currency,
                    cached_rate.rate,
                    cached_rate.source_date,
                    cached_rate.provider,
                    cached_rate.fetched_at,
                ),
            )

