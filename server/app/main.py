import hashlib
import json
import os
import re
import sqlite3
import time
from pathlib import Path
from typing import Any

import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

BASE_DIR = Path(__file__).resolve().parents[1]
load_dotenv(BASE_DIR / ".env")

app = FastAPI(title="Toutiao AI Service", version="0.1.0")
DB_PATH = BASE_DIR / "data" / "cache.sqlite3"
UNKNOWN_TEXT = "\u672a\u77e5"
NONE_TEXT = "\u65e0"
DAILY_BRIEF_TITLE = "\u4eca\u65e5AI\u7b80\u62a5"


class NewsPayload(BaseModel):
    id: str | None = Field(default=None, max_length=160)
    category: str | None = Field(default=None, max_length=80)
    title: str = Field(min_length=1, max_length=300)
    source: str | None = Field(default=None, max_length=80)
    url: str | None = Field(default=None, max_length=1000)
    imageUrl: str | None = Field(default=None, max_length=1000)
    publishTime: str | None = Field(default=None, max_length=80)
    content: str | None = Field(default=None, max_length=12000)


class SummaryRequest(BaseModel):
    news: NewsPayload


class DailyBriefRequest(BaseModel):
    items: list[NewsPayload] = Field(min_length=1, max_length=60)


class NewsRankRequest(BaseModel):
    items: list[NewsPayload] = Field(min_length=1, max_length=60)
    maxItems: int = Field(default=5, ge=1, le=10)


class ChatRequest(BaseModel):
    news: NewsPayload
    question: str = Field(min_length=1, max_length=500)


class ChatTurn(BaseModel):
    role: str = Field(min_length=1, max_length=20)
    content: str = Field(min_length=1, max_length=2000)


class ChatMessageRequest(BaseModel):
    news: NewsPayload
    question: str = Field(min_length=1, max_length=500)
    history: list[ChatTurn] = Field(default_factory=list, max_length=12)


def setting(name: str, default: str = "") -> str:
    return os.getenv(name, default).strip()


def require_client_token(x_app_token: str | None) -> None:
    expected = setting("AI_APP_TOKEN")
    if expected and x_app_token != expected:
        raise HTTPException(status_code=401, detail="invalid AI_APP_TOKEN")


def require_ai_key() -> str:
    key = setting("AI_API_KEY")
    if not key:
        raise HTTPException(status_code=503, detail="AI_API_KEY is not configured on the server")
    return key


def provider() -> str:
    return setting("AI_PROVIDER", "deepseek") or "deepseek"


def model_name() -> str:
    return setting("AI_MODEL", "deepseek-v4-flash") or "deepseek-v4-flash"


def base_url() -> str:
    return setting("AI_BASE_URL", "https://api.deepseek.com").rstrip("/")


def cache_ttl() -> int:
    try:
        return int(setting("AI_CACHE_TTL_SECONDS", "86400"))
    except ValueError:
        return 86400


def timeout_seconds() -> float:
    try:
        return float(setting("AI_TIMEOUT_SECONDS", "45"))
    except ValueError:
        return 45.0


def thinking_mode() -> str:
    mode = setting("AI_THINKING", "disabled").lower()
    return mode if mode in {"enabled", "disabled"} else "disabled"


def max_tokens() -> int:
    try:
        return int(setting("AI_MAX_TOKENS", "900"))
    except ValueError:
        return 900


def init_db() -> None:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    with sqlite3.connect(DB_PATH) as conn:
        conn.execute(
            """CREATE TABLE IF NOT EXISTS ai_cache (
                cache_key TEXT PRIMARY KEY,
                payload TEXT NOT NULL,
                expires_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        conn.commit()


def cache_key(kind: str, payload: Any) -> str:
    raw = json.dumps({"kind": kind, "payload": payload}, ensure_ascii=False, sort_keys=True)
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def get_cache(key: str) -> dict[str, Any] | None:
    init_db()
    now = int(time.time())
    with sqlite3.connect(DB_PATH) as conn:
        row = conn.execute("SELECT payload FROM ai_cache WHERE cache_key = ? AND expires_at > ?", (key, now)).fetchone()
    if not row:
        return None
    return json.loads(row[0])


def set_cache(key: str, payload: dict[str, Any]) -> None:
    init_db()
    now = int(time.time())
    with sqlite3.connect(DB_PATH) as conn:
        conn.execute(
            "REPLACE INTO ai_cache(cache_key, payload, expires_at, created_at) VALUES (?, ?, ?, ?)",
            (key, json.dumps(payload, ensure_ascii=False), now + cache_ttl(), now),
        )
        conn.commit()


def compact_news(news: dict[str, Any], max_content: int = 5000) -> str:
    content = (news.get("content") or "").strip()
    if len(content) > max_content:
        content = content[:max_content] + "..."
    lines = [
        f"\u6807\u9898\uff1a{news.get('title', '')}",
        f"\u6765\u6e90\uff1a{news.get('source') or UNKNOWN_TEXT}",
        f"\u53d1\u5e03\u65f6\u95f4\uff1a{news.get('publishTime') or UNKNOWN_TEXT}",
        f"\u94fe\u63a5\uff1a{news.get('url') or NONE_TEXT}",
    ]
    if content:
        lines.append(f"\u6b63\u6587\uff1a{content}")
    return "\n".join(lines)


def build_summary_messages(news: dict[str, Any]) -> list[dict[str, str]]:
    return [
        {
            "role": "system",
            "content": "\u4f60\u662f\u4e00\u4e2a\u4e2d\u6587\u65b0\u95fb\u9605\u8bfb\u52a9\u624b\u3002\u53ea\u8f93\u51fa\u4e25\u683c JSON\uff0c\u4e0d\u8981 Markdown\u3002",
        },
        {
            "role": "user",
            "content": (
                "\u8bf7\u57fa\u4e8e\u4e0b\u9762\u65b0\u95fb\u751f\u6210\uff1a\u4e00\u53e5\u8bdd\u6458\u8981\u30013\u4e2a\u8981\u70b9\u30011\u52303\u4e2a\u6807\u7b7e\u3002\n"
                "\u8fd4\u56de JSON \u683c\u5f0f\uff1a{\"summary\":\"...\",\"bullets\":[\"...\",\"...\",\"...\"],\"tags\":[\"...\"]}\n\n"
                + compact_news(news)
            ),
        },
    ]


def build_daily_messages(items: list[dict[str, Any]]) -> list[dict[str, str]]:
    joined = "\n\n".join(f"{idx + 1}. {compact_news(item, max_content=800)}" for idx, item in enumerate(items[:60]))
    return [
        {"role": "system", "content": "\u4f60\u662f\u4e00\u4e2a\u4e2d\u6587\u65b0\u95fb\u7f16\u8f91\u3002\u53ea\u8f93\u51fa\u4e25\u683c JSON\uff0c\u4e0d\u8981 Markdown\u3002"},
        {
            "role": "user",
            "content": (
                "\u8bf7\u628a\u8fd9\u4e9b\u65b0\u95fb\u6574\u7406\u6210\u4eca\u65e5AI\u7b80\u62a5\uff0c\u6700\u591a10\u6761\u70ed\u70b9\uff0c\u6bcf\u6761\u5305\u542b\u6807\u9898\u548c\u4e00\u53e5\u89e3\u91ca\u3002\n"
                "\u8fd4\u56de JSON\uff1a{\"title\":\"\u4eca\u65e5AI\u7b80\u62a5\",\"items\":[{\"title\":\"...\",\"reason\":\"...\"}]}\n\n"
                + joined
            ),
        },
    ]


def build_news_rank_messages(items: list[dict[str, Any]], max_items: int = 5) -> list[dict[str, str]]:
    compact_items = []
    for idx, item in enumerate(items[:60]):
        compact_items.append(
            {
                "index": idx + 1,
                "id": item.get("id") or "",
                "category": item.get("category") or "",
                "title": item.get("title") or "",
                "source": item.get("source") or "",
                "url": item.get("url") or "",
                "publishTime": item.get("publishTime") or "",
                "content": (item.get("content") or "")[:800],
            }
        )
    return [
        {
            "role": "system",
            "content": (
                "\u4f60\u662f\u4e00\u4e2a\u4e25\u8c28\u7684\u4e2d\u6587\u65b0\u95fb\u7f16\u8f91\u3002"
                "\u53ea\u80fd\u4ece\u7528\u6237\u63d0\u4f9b\u7684\u65b0\u95fb\u5217\u8868\u91cc\u9009\u62e9\uff0c\u4e0d\u8981\u7f16\u9020\u65b0\u95fb\u3002"
                "\u53ea\u8f93\u51fa\u4e25\u683c JSON\uff0c\u4e0d\u8981 Markdown\u3002"
            ),
        },
        {
            "role": "user",
            "content": (
                f"\u8bf7\u4ece\u4ee5\u4e0b\u65b0\u95fb\u4e2d\u9009\u51fa\u6700\u503c\u5f97\u770b\u7684 {max_items} \u6761\uff0c"
                "\u4f18\u5148\u8003\u8651\u516c\u5171\u5f71\u54cd\u3001\u65b0\u9c9c\u5ea6\u3001\u4fe1\u606f\u5bc6\u5ea6\u548c\u8ba8\u8bba\u4ef7\u503c\u3002"
                "\u6bcf\u6761\u5fc5\u987b\u4f7f\u7528\u539f\u59cb id \u586b\u5165 newsId\uff0c\u5e76\u4fdd\u7559 url\u3002\n"
                "\u8fd4\u56de JSON\uff1a"
                "{\"title\":\"\u4eca\u5929\u6700\u503c\u5f97\u770b\u7684\u65b0\u95fb\","
                "\"items\":[{\"newsId\":\"\u539f\u59cbid\",\"title\":\"...\",\"reason\":\"...\",\"url\":\"...\"}]}\n\n"
                f"\u65b0\u95fb\u5217\u8868\uff1a{json.dumps(compact_items, ensure_ascii=False)}"
            ),
        },
    ]


def normalize_rank_items(
    items: list[dict[str, Any]],
    source_items: list[dict[str, Any]],
    max_items: int = 5,
) -> list[dict[str, Any]]:
    by_id = {str(item.get("id")): item for item in source_items if item.get("id")}
    by_url = {str(item.get("url")): item for item in source_items if item.get("url")}
    by_title = {str(item.get("title")): item for item in source_items if item.get("title")}
    normalized: list[dict[str, Any]] = []
    used: set[str] = set()

    def append_item(ai_item: dict[str, Any], source: dict[str, Any] | None) -> None:
        source = source or {}
        news_id = str(source.get("id") or ai_item.get("newsId") or ai_item.get("id") or source.get("url") or "")
        dedupe_key = news_id or str(source.get("url") or ai_item.get("url") or ai_item.get("title") or len(normalized))
        if dedupe_key in used or len(normalized) >= max_items:
            return
        used.add(dedupe_key)
        normalized.append(
            {
                "newsId": news_id,
                "title": str(source.get("title") or ai_item.get("title") or "").strip(),
                "reason": str(ai_item.get("reason") or "\u70ed\u5ea6\u9ad8\uff0c\u503c\u5f97\u5173\u6ce8").strip(),
                "url": source.get("url") or ai_item.get("url"),
                "source": source.get("source") or ai_item.get("source"),
                "publishTime": source.get("publishTime") or ai_item.get("publishTime"),
                "imageUrl": source.get("imageUrl") or ai_item.get("imageUrl"),
            }
        )

    for ai_item in items:
        if not isinstance(ai_item, dict):
            continue
        source = (
            by_id.get(str(ai_item.get("newsId")))
            or by_id.get(str(ai_item.get("id")))
            or by_url.get(str(ai_item.get("url")))
            or by_title.get(str(ai_item.get("title")))
        )
        append_item(ai_item, source)

    for source in source_items:
        if len(normalized) >= max_items:
            break
        append_item({"reason": "\u5f53\u524d\u70ed\u70b9\u65b0\u95fb\uff0c\u53ef\u4ee5\u5feb\u901f\u4e86\u89e3\u4e8b\u4ef6\u8fdb\u5c55"}, source)

    return normalized[:max_items]


def build_chat_messages(news: dict[str, Any], question: str) -> list[dict[str, str]]:
    return [
        {"role": "system", "content": "\u4f60\u662f\u4e00\u4e2a\u4e2d\u6587\u65b0\u95fb\u89e3\u91ca\u52a9\u624b\u3002\u56de\u7b54\u8981\u7b80\u6d01\u3001\u5ba2\u89c2\uff0c\u4e0d\u7f16\u9020\u65b0\u95fb\u5916\u4e8b\u5b9e\u3002"},
        {"role": "user", "content": f"\u65b0\u95fb\u4fe1\u606f\u5982\u4e0b\uff1a\n{compact_news(news)}\n\n\u7528\u6237\u95ee\u9898\uff1a{question}"},
    ]


def build_chat_message_messages(
    news: dict[str, Any],
    question: str,
    history: list[dict[str, Any]] | None = None,
) -> list[dict[str, str]]:
    safe_history = []
    for turn in (history or [])[-8:]:
        role = str(turn.get("role") or "").strip()
        if role not in {"user", "assistant"}:
            continue
        content = str(turn.get("content") or "").strip()
        if content:
            safe_history.append(f"{role}: {content[:1000]}")
    history_text = "\n".join(safe_history) if safe_history else NONE_TEXT
    return [
        {
            "role": "system",
            "content": (
                "\u4f60\u662f\u5d4c\u5165\u65b0\u95fb App \u7684\u4e2d\u6587 AI \u5bf9\u8bdd\u52a9\u624b\u3002"
                "\u53ea\u6839\u636e\u5f53\u524d\u65b0\u95fb\u548c\u5bf9\u8bdd\u5386\u53f2\u56de\u7b54\uff0c\u4e0d\u7f16\u9020\u672a\u63d0\u4f9b\u7684\u4e8b\u5b9e\u3002"
                "\u56de\u7b54\u8981\u77ed\u3001\u6e05\u695a\u3001\u53ef\u64cd\u4f5c\u3002\u53ea\u8f93\u51fa\u4e25\u683c JSON\uff0c\u4e0d\u8981 Markdown\u3002"
            ),
        },
        {
            "role": "user",
            "content": (
                "\u5f53\u524d\u65b0\u95fb\uff1a\n"
                f"{compact_news(news, max_content=6000)}\n\n"
                f"\u5bf9\u8bdd\u5386\u53f2\uff1a\n{history_text}\n\n"
                f"\u7528\u6237\u5f53\u524d\u95ee\u9898\uff1a{question}\n\n"
                "\u8fd4\u56de JSON\uff1a"
                "{\"answer\":\"\u56de\u7b54\u5185\u5bb9\","
                "\"suggestedQuestions\":[\"\u8fd9\u4ef6\u4e8b\u540e\u7eed\u770b\u4ec0\u4e48\uff1f\",\"\u5bf9\u6211\u6709\u4ec0\u4e48\u5f71\u54cd\uff1f\"]}"
            ),
        },
    ]


def parse_json_response(text: str) -> dict[str, Any]:
    cleaned = text.strip()
    cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
    cleaned = re.sub(r"\s*```$", "", cleaned)
    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", cleaned, flags=re.S)
        if match:
            return json.loads(match.group(0))
        raise HTTPException(status_code=502, detail="AI provider returned non-JSON content")


async def call_ai(messages: list[dict[str, str]], temperature: float = 0.2, json_mode: bool = False) -> str:
    key = require_ai_key()
    payload = {
        "model": model_name(),
        "messages": messages,
        "temperature": temperature,
        "max_tokens": max_tokens(),
        "thinking": {"type": thinking_mode()},
    }
    if json_mode:
        payload["response_format"] = {"type": "json_object"}
    headers = {"Authorization": f"Bearer {key}", "Content-Type": "application/json"}
    async with httpx.AsyncClient(timeout=timeout_seconds()) as client:
        response = await client.post(f"{base_url()}/chat/completions", headers=headers, json=payload)
    if response.status_code >= 400:
        raise HTTPException(status_code=502, detail=f"AI provider error: {response.status_code} {response.text[:300]}")
    data = response.json()
    return data["choices"][0]["message"]["content"]


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "provider": provider(), "model": model_name()}


@app.post("/api/ai/summary")
async def summarize(request: SummaryRequest, x_app_token: str | None = Header(default=None)) -> dict[str, Any]:
    require_client_token(x_app_token)
    payload = request.news.model_dump()
    key = cache_key("summary", payload)
    cached = get_cache(key)
    if cached:
        cached["cached"] = True
        return cached
    result = parse_json_response(await call_ai(build_summary_messages(payload), json_mode=True))
    response = {
        "summary": str(result.get("summary") or "").strip(),
        "bullets": [str(item).strip() for item in result.get("bullets", [])][:3],
        "tags": [str(item).strip() for item in result.get("tags", [])][:3],
        "provider": provider(),
        "model": model_name(),
        "cached": False,
    }
    set_cache(key, response | {"cached": False})
    return response


@app.post("/api/ai/daily-brief")
async def daily_brief(request: DailyBriefRequest, x_app_token: str | None = Header(default=None)) -> dict[str, Any]:
    require_client_token(x_app_token)
    payload = [item.model_dump() for item in request.items]
    key = cache_key("daily-brief", payload)
    cached = get_cache(key)
    if cached:
        cached["cached"] = True
        return cached
    result = parse_json_response(await call_ai(build_daily_messages(payload), json_mode=True))
    response = {
        "title": str(result.get("title") or DAILY_BRIEF_TITLE).strip(),
        "items": result.get("items", [])[:10],
        "provider": provider(),
        "model": model_name(),
        "cached": False,
    }
    set_cache(key, response | {"cached": False})
    return response


@app.post("/api/ai/news-rank")
async def news_rank(request: NewsRankRequest, x_app_token: str | None = Header(default=None)) -> dict[str, Any]:
    require_client_token(x_app_token)
    payload = [item.model_dump() for item in request.items]
    max_items = request.maxItems
    key = cache_key("news-rank", {"items": payload, "maxItems": max_items})
    cached = get_cache(key)
    if cached:
        cached["cached"] = True
        return cached
    result = parse_json_response(await call_ai(build_news_rank_messages(payload, max_items=max_items), json_mode=True))
    items = normalize_rank_items(result.get("items", []), payload, max_items=max_items)
    response = {
        "title": str(result.get("title") or f"\u4eca\u5929\u6700\u503c\u5f97\u770b\u7684 {len(items)} \u6761").strip(),
        "items": items,
        "provider": provider(),
        "model": model_name(),
        "cached": False,
    }
    set_cache(key, response | {"cached": False})
    return response


@app.post("/api/ai/chat")
async def chat(request: ChatRequest, x_app_token: str | None = Header(default=None)) -> dict[str, Any]:
    require_client_token(x_app_token)
    answer = await call_ai(build_chat_messages(request.news.model_dump(), request.question), temperature=0.3)
    return {"answer": answer.strip(), "provider": provider(), "model": model_name()}


@app.post("/api/ai/chat/message")
async def chat_message(request: ChatMessageRequest, x_app_token: str | None = Header(default=None)) -> dict[str, Any]:
    require_client_token(x_app_token)
    history = [turn.model_dump() for turn in request.history]
    result = parse_json_response(
        await call_ai(
            build_chat_message_messages(request.news.model_dump(), request.question, history),
            temperature=0.3,
            json_mode=True,
        )
    )
    suggestions = [str(item).strip() for item in result.get("suggestedQuestions", []) if str(item).strip()]
    return {
        "answer": str(result.get("answer") or "").strip(),
        "suggestedQuestions": suggestions[:3],
        "provider": provider(),
        "model": model_name(),
    }
