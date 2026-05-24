from __future__ import annotations

import threading
import time
from dataclasses import dataclass
from typing import Any


@dataclass
class CacheItem:
    value: Any
    expires_at: float


class TTLCache:
    def __init__(self, default_ttl_seconds: int) -> None:
        self._default_ttl_seconds = default_ttl_seconds
        self._data: dict[str, CacheItem] = {}
        self._lock = threading.Lock()

    def get(self, key: str) -> Any | None:
        now = time.time()
        with self._lock:
            item = self._data.get(key)
            if item is None:
                return None
            if item.expires_at <= now:
                self._data.pop(key, None)
                return None
            return item.value

    def set(self, key: str, value: Any, ttl_seconds: int | None = None) -> None:
        ttl = ttl_seconds if ttl_seconds is not None else self._default_ttl_seconds
        expires_at = time.time() + max(ttl, 1)
        with self._lock:
            self._data[key] = CacheItem(value=value, expires_at=expires_at)

    def invalidate(self, key: str) -> None:
        with self._lock:
            self._data.pop(key, None)

    def invalidate_prefix(self, prefix: str) -> None:
        with self._lock:
            for key in list(self._data.keys()):
                if key.startswith(prefix):
                    self._data.pop(key, None)

