from __future__ import annotations

import httpx


class HTTPClient:
    def __init__(self, timeout_seconds: int = 30):
        self.timeout_seconds = timeout_seconds

    def build_client(self, headers: dict[str, str] | None = None) -> httpx.Client:
        return httpx.Client(timeout=self.timeout_seconds, headers=headers or {})
