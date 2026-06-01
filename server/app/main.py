import hashlib
import json
import os
import re
import sqlite3
import time
from pathlib import Path
from typing import Any, TypedDict
from urllib.parse import quote_plus, urljoin

import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

try:
    from bs4 import BeautifulSoup
except ImportError:  # pragma: no cover - optional in local lightweight installs
    BeautifulSoup = None

try:
    from langgraph.graph import END, StateGraph
except ImportError:  # pragma: no cover - the endpoint falls back to inline flow
    END = None
    StateGraph = None

BASE_DIR = Path(__file__).resolve().parents[1]
load_dotenv(BASE_DIR / ".env")

app = FastAPI(title="Headline News AI Service", version="0.1.0")
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


class SourcePayload(BaseModel):
    title: str = Field(default="", max_length=200)
    url: str = Field(default="", max_length=1000)
    snippet: str = Field(default="", max_length=1000)


class ArticleBlockPayload(BaseModel):
    type: str = Field(default="text", max_length=20)
    text: str | None = Field(default=None, max_length=3000)
    url: str | None = Field(default=None, max_length=1000)
    alt: str | None = Field(default=None, max_length=300)


class ChatMessageRequest(BaseModel):
    news: NewsPayload
    question: str = Field(min_length=1, max_length=500)
    history: list[ChatTurn] = Field(default_factory=list, max_length=12)


class ArticleExtractRequest(BaseModel):
    news: NewsPayload


class ArticleAgentState(TypedDict, total=False):
    news: dict[str, Any]
    question: str
    history: list[dict[str, Any]]
    sources: list[dict[str, str]]
    result: dict[str, Any]


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
    return setting("AI_MODEL", "deepseek-chat") or "deepseek-chat"


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


def chat_max_tokens() -> int:
    try:
        return max(240, min(int(setting("AI_CHAT_MAX_TOKENS", "520")), 900))
    except ValueError:
        return 520


def web_search_enabled() -> bool:
    return setting("AI_WEB_SEARCH_ENABLED", "false").lower() in {"1", "true", "yes", "on"}


def search_max_results() -> int:
    try:
        return max(1, min(int(setting("AI_SEARCH_MAX_RESULTS", "3")), 5))
    except ValueError:
        return 3


def article_fetch_timeout() -> float:
    try:
        return max(2.0, min(float(setting("AI_ARTICLE_FETCH_TIMEOUT_SECONDS", "5")), 20.0))
    except ValueError:
        return 5.0


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


def compact_history_for_cache(history: list[dict[str, Any]]) -> list[dict[str, str]]:
    compact: list[dict[str, str]] = []
    for turn in history[-6:]:
        role = str(turn.get("role") or "").strip()
        content = re.sub(r"\s+", " ", str(turn.get("content") or "").strip())
        if role in {"user", "assistant"} and content:
            compact.append({"role": role, "content": content[:500]})
    return compact


def content_fingerprint(news: dict[str, Any]) -> str:
    content = re.sub(r"\s+", " ", str(news.get("content") or "").strip())
    return hashlib.sha256(content[:6000].encode("utf-8")).hexdigest()


def article_cache_key(url: str) -> str:
    return cache_key("article-text", {"url": url})


def article_detail_cache_key(url: str) -> str:
    return cache_key("article-detail-v3", {"url": url})


async def get_article_text(url: str | None) -> str:
    clean_url = (url or "").strip()
    if not clean_url:
        return ""
    cached = get_cache(article_cache_key(clean_url))
    if cached:
        return str(cached.get("text") or "")
    fetched = await fetch_article_text(clean_url)
    if fetched:
        set_cache(article_cache_key(clean_url), {"text": fetched})
    return fetched


def chat_message_cache_key(news: dict[str, Any], question: str, history: list[dict[str, Any]]) -> str:
    payload = {
        "provider": provider(),
        "model": model_name(),
        "newsId": news.get("id") or "",
        "url": news.get("url") or "",
        "title": news.get("title") or "",
        "source": news.get("source") or "",
        "publishTime": news.get("publishTime") or "",
        "contentHash": content_fingerprint(news),
        "question": re.sub(r"\s+", " ", question.strip()),
        "history": compact_history_for_cache(history),
    }
    return cache_key("chat-message", payload)


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


def should_search_web(news: dict[str, Any], question: str) -> bool:
    question_text = question.strip().lower()
    if is_identity_question(question_text):
        return False
    search_keywords = [
        "\u6700\u65b0",
        "\u73b0\u5728",
        "\u540e\u7eed",
        "\u80cc\u666f",
        "\u6765\u6e90",
        "\u8054\u7f51",
        "\u641c\u7d22",
        "\u67e5\u4e00\u4e0b",
        "latest",
        "update",
        "background",
        "source",
        "search",
    ]
    return any(keyword in question_text for keyword in search_keywords)


def is_identity_question(question: str) -> bool:
    text = question.strip().lower()
    identity_phrases = [
        "\u4f60\u662f\u4ec0\u4e48\u6a21\u578b",
        "\u4f60\u662f\u54ea\u4e2a\u6a21\u578b",
        "\u4f60\u662f\u8c01",
        "\u4f60\u80fd\u505a\u4ec0\u4e48",
        "\u4ec0\u4e48\u6a21\u578b",
        "what model",
        "who are you",
    ]
    return any(phrase in text for phrase in identity_phrases)


def is_lightweight_chat_question(question: str) -> bool:
    text = re.sub(r"\s+", "", question.strip().lower())
    phrases = {
        "\u4f60\u597d",
        "hi",
        "hello",
        "\u5728\u5417",
        "\u5e2e\u6211\u4e00\u4e0b",
        "\u8c22\u8c22",
        "thanks",
    }
    return text in phrases


def build_lightweight_chat_response(news: dict[str, Any]) -> dict[str, Any]:
    return {
        "answer": (
            "\u6211\u5728\u3002\u4f60\u53ef\u4ee5\u76f4\u63a5\u95ee\u6211\u8fd9\u7bc7\u65b0\u95fb\u7684\u6838\u5fc3\u5185\u5bb9\u3001"
            "\u5f71\u54cd\u3001\u80cc\u666f\u6216\u540e\u7eed\u503c\u5f97\u5173\u6ce8\u7684\u70b9\u3002"
        ),
        "suggestedQuestions": ["\u8bb2\u89e3\u4e00\u4e0b\u8fd9\u4e2a\u65b0\u95fb", "\u5f71\u54cd\u662f\u4ec0\u4e48", "\u540e\u7eed\u770b\u4ec0\u4e48"],
        "sources": normalize_sources(
            [
                {
                    "title": news.get("title") or news.get("source") or "\u5f53\u524d\u65b0\u95fb",
                    "url": news.get("url"),
                    "snippet": (news.get("content") or "")[:180],
                }
            ],
            max_items=1,
        ),
    }


def build_identity_response(news: dict[str, Any]) -> dict[str, Any]:
    provider_label = "DeepSeek" if provider().lower() == "deepseek" else provider()
    source = normalize_sources(
        [
            {
                "title": news.get("title") or news.get("source") or "\u5f53\u524d\u65b0\u95fb",
                "url": news.get("url"),
                "snippet": (news.get("content") or "")[:180],
            }
        ],
        max_items=1,
    )
    return {
        "answer": (
            f"\u6211\u662f\u65b0\u95fb App \u91cc\u7684 AI \u9605\u8bfb\u52a9\u624b\uff0c"
            f"\u5f53\u524d\u670d\u52a1\u7aef\u8c03\u7528 {provider_label} \u7684 {model_name()} \u6a21\u578b\u3002"
            "\u6211\u4f1a\u4f18\u5148\u57fa\u4e8e\u5f53\u524d\u65b0\u95fb\u539f\u6587\u548c\u5bf9\u8bdd\u4e0a\u4e0b\u6587\u56de\u7b54\uff0c"
            "\u53ea\u6709\u5728\u9700\u8981\u80cc\u666f\u6216\u6700\u65b0\u8fdb\u5c55\u65f6\u624d\u4f1a\u8054\u7f51\u68c0\u7d22\u3002"
        ),
        "suggestedQuestions": ["\u8bb2\u89e3\u4e00\u4e0b\u8fd9\u4e2a\u65b0\u95fb", "\u5f71\u54cd\u662f\u4ec0\u4e48", "\u6211\u8be5\u5173\u6ce8\u4ec0\u4e48"],
        "sources": source,
    }


def normalize_sources(raw_sources: list[dict[str, Any]], max_items: int = 3) -> list[dict[str, str]]:
    sources: list[dict[str, str]] = []
    seen: set[str] = set()
    for source in raw_sources:
        url = str(source.get("url") or source.get("link") or "").strip()
        if not url or url in seen:
            continue
        title = str(source.get("title") or source.get("source") or url).strip()
        snippet = str(source.get("snippet") or source.get("content") or source.get("raw_content") or "").strip()
        sources.append({"title": title[:200], "url": url[:1000], "snippet": snippet[:1000]})
        seen.add(url)
        if len(sources) >= max_items:
            break
    return sources


def strip_html(html: str) -> str:
    if BeautifulSoup is not None:
        soup = BeautifulSoup(html, "html.parser")
        for tag in soup(["script", "style", "noscript"]):
            tag.decompose()
        return re.sub(r"\s+", " ", soup.get_text(" ", strip=True)).strip()
    text = re.sub(r"(?is)<(script|style).*?>.*?</\1>", " ", html)
    text = re.sub(r"(?s)<[^>]+>", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def clean_article_text(text: str) -> str:
    text = re.sub(r"\s+", " ", text or "").strip()
    blocked_phrases = [
        "\u6253\u5f00app",
        "\u6253\u5f00 app",
        "\u4e0b\u8f7dapp",
        "\u4e0b\u8f7d app",
        "\u5e7f\u544a",
        "\u70b9\u51fb\u67e5\u770b",
        "\u5206\u4eab\u5230",
    ]
    if any(phrase in text.lower() for phrase in blocked_phrases):
        return ""
    return text


def is_meaningful_article_text(text: str) -> bool:
    text = clean_article_text(text)
    if len(text) < 8:
        return False
    if re.fullmatch(r"[\d\s.,，。:：/\-]+", text):
        return False
    if re.fullmatch(r"\d{1,8}", text):
        return False
    if any(marker in text for marker in ("window.", "function(", "var ", "document.")):
        return False
    noisy_words = ("打开APP", "下载APP", "相关阅读", "相关推荐", "点击加载", "版权声明")
    if len(text) < 40 and any(word in text for word in noisy_words):
        return False
    return True


def extract_meta_content(soup: Any, selectors: list[str]) -> str:
    for selector in selectors:
        value = soup.select_one(selector)
        if value:
            content = value.get("content") or value.get_text(" ", strip=True)
            if content and content.strip():
                return content.strip()
    return ""


def candidate_article_nodes(soup: Any, url: str) -> list[Any]:
    url_lower = url.lower()
    platform_selectors: list[str] = []
    if "thepaper.cn" in url_lower:
        platform_selectors = [".news_txt", ".index_cententWrap", ".news_part_father", ".newsdetail_content"]
    elif "toutiao.com" in url_lower or "toutiao.cn" in url_lower:
        platform_selectors = [".article-content", ".article__content", "#article-detail .content", ".syl-article-base"]
    elif "zhihu.com" in url_lower:
        platform_selectors = [".Post-RichTextContainer", ".RichContent-inner", ".Post-RichText", ".QuestionAnswer-content"]
    elif "v2ex.com" in url_lower:
        platform_selectors = [".topic_content", ".markdown_body", "#Main .box", ".cell"]
    elif "geekpark.net" in url_lower or "geekpark.cn" in url_lower:
        platform_selectors = ["article", ".article-content", ".post-content", ".content"]
    elif "tieba.baidu.com" in url_lower:
        platform_selectors = [".p_postlist", ".d_post_content", ".mainContent"]
    elif "baidu.com" in url_lower:
        platform_selectors = [".mainContent", "#article", ".article-content"]

    selectors = platform_selectors + [
        "article",
        "[itemprop=articleBody]",
        ".article-body",
        ".article-content",
        ".post-content",
        ".entry-content",
        ".content-body",
        ".story-body",
        "main .content",
        "#content",
        "main",
    ]
    nodes = []
    for selector in selectors:
        nodes.extend(soup.select(selector))
    return nodes


def score_article_node(node: Any) -> int:
    text_len = len(clean_article_text(node.get_text(" ", strip=True)))
    paragraph_count = len([p for p in node.select("p") if len(clean_article_text(p.get_text(" ", strip=True))) >= 20])
    image_count = len(node.select("img"))
    return text_len + paragraph_count * 80 + min(image_count, 8) * 40


def normalize_image_url(raw_url: str, base_url: str) -> str:
    raw_url = (raw_url or "").strip()
    if not raw_url or raw_url.startswith("data:"):
        return ""
    return urljoin(base_url, raw_url)


def parse_article_blocks(container: Any, base_url: str) -> list[dict[str, str]]:
    blocks: list[dict[str, str]] = []
    seen_text: set[str] = set()
    seen_images: set[str] = set()
    elements = container.select("p, h2, h3, h4, blockquote, li, img, figure")
    if not elements:
        elements = list(container.children)

    for element in elements:
        if not hasattr(element, "get_text"):
            continue
        tag = getattr(element, "name", "") or ""
        if tag == "figure":
            image = element.select_one("img")
            if image is None:
                continue
            image_url = normalize_image_url(
                image.get("src") or image.get("data-src") or image.get("data-original") or image.get("data-actualsrc") or "",
                base_url,
            )
            if not image_url or image_url in seen_images:
                continue
            seen_images.add(image_url)
            caption = element.select_one("figcaption")
            blocks.append(
                {
                    "type": "image",
                    "url": image_url,
                    "alt": image.get("alt") or (caption.get_text(" ", strip=True) if caption else ""),
                }
            )
            continue

        if tag == "img":
            image_url = normalize_image_url(
                element.get("src") or element.get("data-src") or element.get("data-original") or element.get("data-actualsrc") or "",
                base_url,
            )
            if image_url and image_url not in seen_images:
                seen_images.add(image_url)
                blocks.append({"type": "image", "url": image_url, "alt": element.get("alt") or ""})
            continue

        text = clean_article_text(element.get_text(" ", strip=True))
        if not is_meaningful_article_text(text) or text in seen_text:
            continue
        seen_text.add(text)
        blocks.append({"type": "text", "text": text})
        if len(blocks) >= 120:
            break

    return blocks


def fallback_article_payload(news: dict[str, Any], url: str | None, cached: bool = False) -> dict[str, Any]:
    blocks: list[dict[str, str]] = []
    image_url = str(news.get("imageUrl") or "").strip()
    content = clean_article_text(str(news.get("content") or ""))
    if image_url:
        blocks.append({"type": "image", "url": image_url, "alt": str(news.get("title") or "")})
    if content:
        for paragraph in re.split(r"\n+|(?<=[。！？])\s+", content):
            paragraph = clean_article_text(paragraph)
            if is_meaningful_article_text(paragraph):
                blocks.append({"type": "text", "text": paragraph})
    text_total = sum(len(block.get("text", "")) for block in blocks if block.get("type") == "text")
    return {
        "success": text_total >= 20,
        "title": str(news.get("title") or "").strip(),
        "source": news.get("source"),
        "publishTime": news.get("publishTime"),
        "url": url or news.get("url"),
        "imageUrl": image_url or None,
        "blocks": blocks[:80],
        "cached": cached,
    }


def parse_article_detail(html: str, base_url: str, news: dict[str, Any]) -> dict[str, Any]:
    if BeautifulSoup is None:
        return fallback_article_payload(news, base_url)
    soup = BeautifulSoup(html, "html.parser")
    for tag in soup(["script", "style", "noscript", "iframe", "nav", "header", "footer", "form"]):
        tag.decompose()
    for node in soup.select(
        ".comment, .comments, .comment-list, .comment-area, .recommend, .related, .sidebar, "
        ".ad, .advertisement, .app-download, .download-bar, .open-app, .launch-app, "
        ".share-bar, .toolbar, .action-bar, .bottom-bar, [class*=download], [class*=openApp], "
        "[class*=open-app], [class*=ad-wrapper], [class*=banner], [class*=recommend], "
        ".article-tag, .article-footer, .feed-card, .related-news"
    ):
        node.decompose()

    title = (
        extract_meta_content(soup, ["meta[property='og:title']", "meta[name='twitter:title']"])
        or (soup.select_one("h1").get_text(" ", strip=True) if soup.select_one("h1") else "")
        or str(news.get("title") or "")
    ).strip()
    source = (
        extract_meta_content(soup, ["meta[property='og:site_name']", "meta[name='source']", "meta[name='author']"])
        or str(news.get("source") or "")
    ).strip()
    publish_time = (
        extract_meta_content(
            soup,
            [
                "meta[property='article:published_time']",
                "meta[name='publishdate']",
                "meta[name='pubdate']",
                "meta[name='date']",
            ],
        )
        or str(news.get("publishTime") or "")
    ).strip()
    image_url = normalize_image_url(
        extract_meta_content(soup, ["meta[property='og:image']", "meta[name='twitter:image']"])
        or str(news.get("imageUrl") or ""),
        base_url,
    )

    candidates = candidate_article_nodes(soup, base_url)
    container = max(candidates, key=score_article_node) if candidates else soup.body
    blocks = parse_article_blocks(container, base_url) if container else []
    if image_url and all(block.get("url") != image_url for block in blocks if block.get("type") == "image"):
        blocks.insert(0, {"type": "image", "url": image_url, "alt": title})

    text_total = sum(len(block.get("text", "")) for block in blocks if block.get("type") == "text")
    if not blocks or text_total < 80:
        fallback = fallback_article_payload(news, base_url)
        if fallback["success"]:
            return fallback

    return {
        "success": text_total >= 80,
        "title": title,
        "source": source or None,
        "publishTime": publish_time or None,
        "url": base_url,
        "imageUrl": image_url or None,
        "blocks": blocks[:120],
        "cached": False,
    }


async def extract_article_detail(news: dict[str, Any]) -> dict[str, Any]:
    url = str(news.get("url") or "").strip()
    if not url or not url.startswith(("http://", "https://")):
        return fallback_article_payload(news, url)

    key = article_detail_cache_key(url)
    cached = get_cache(key)
    if cached:
        cached["cached"] = True
        return cached

    headers = {
        "User-Agent": "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125 Mobile Safari/537.36",
        "Accept": "text/html,application/xhtml+xml",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
    }
    try:
        async with httpx.AsyncClient(timeout=article_fetch_timeout(), follow_redirects=True) as client:
            response = await client.get(url, headers=headers)
        if response.status_code >= 400:
            return fallback_article_payload(news, url)
        result = parse_article_detail(response.text, str(response.url), news)
        if result.get("success"):
            set_cache(key, result | {"cached": False})
        return result
    except Exception:
        return fallback_article_payload(news, url)


async def fetch_article_text(url: str | None) -> str:
    if not url or not url.startswith(("http://", "https://")):
        return ""
    headers = {
        "User-Agent": "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125 Mobile Safari/537.36",
        "Accept": "text/html,application/xhtml+xml",
    }
    try:
        async with httpx.AsyncClient(timeout=article_fetch_timeout(), follow_redirects=True) as client:
            response = await client.get(url, headers=headers)
        if response.status_code >= 400:
            return ""
        return strip_html(response.text)[:8000]
    except Exception:
        return ""


async def search_web_sources(news: dict[str, Any], question: str) -> list[dict[str, str]]:
    tavily_key = setting("TAVILY_API_KEY")
    if not web_search_enabled():
        return []
    query = " ".join(part for part in [news.get("title"), news.get("source"), question] if part)
    if not tavily_key:
        return await search_duckduckgo_sources(query)
    payload = {
        "api_key": tavily_key,
        "query": query[:400],
        "max_results": search_max_results(),
        "include_answer": False,
        "include_raw_content": False,
    }
    try:
        async with httpx.AsyncClient(timeout=article_fetch_timeout()) as client:
            response = await client.post("https://api.tavily.com/search", json=payload)
        if response.status_code >= 400:
            return []
        data = response.json()
        return normalize_sources(data.get("results", []), max_items=search_max_results())
    except Exception:
        return []


async def search_duckduckgo_sources(query: str) -> list[dict[str, str]]:
    if not query.strip():
        return []
    url = f"https://duckduckgo.com/html/?q={quote_plus(query[:400])}"
    headers = {"User-Agent": "Mozilla/5.0 AppleWebKit/537.36 Chrome/125 Safari/537.36"}
    try:
        async with httpx.AsyncClient(timeout=article_fetch_timeout(), follow_redirects=True) as client:
            response = await client.get(url, headers=headers)
        if response.status_code >= 400:
            return []
        html = response.text
    except Exception:
        return []

    if BeautifulSoup is None:
        return []

    raw_sources: list[dict[str, str]] = []
    soup = BeautifulSoup(html, "html.parser")
    for result in soup.select(".result")[: search_max_results()]:
        link = result.select_one(".result__a")
        snippet = result.select_one(".result__snippet")
        if not link:
            continue
        href = link.get("href") or ""
        title = link.get_text(" ", strip=True)
        raw_sources.append(
            {
                "title": title,
                "url": href,
                "snippet": snippet.get_text(" ", strip=True) if snippet else "",
            }
        )
    return normalize_sources(raw_sources, max_items=search_max_results())


def build_article_agent_messages(
    news: dict[str, Any],
    question: str,
    history: list[dict[str, Any]] | None,
    sources: list[dict[str, str]],
) -> list[dict[str, str]]:
    safe_history = []
    for turn in (history or [])[-8:]:
        role = str(turn.get("role") or "").strip()
        if role in {"user", "assistant"} and str(turn.get("content") or "").strip():
            safe_history.append({"role": role, "content": str(turn.get("content"))[:700]})
    return [
        {
            "role": "system",
            "content": (
                "\u4f60\u662f\u65b0\u95fb App \u91cc\u7684 AI \u9605\u8bfb\u52a9\u624b\u3002"
                "\u4f60\u5fc5\u987b\u57fa\u4e8e\u5f53\u524d\u65b0\u95fb\u3001\u5bf9\u8bdd\u5386\u53f2\u548c\u5df2\u63d0\u4f9b\u7684\u6765\u6e90\u56de\u7b54\u3002"
                "\u4e0d\u8981\u7f16\u9020\u6765\u6e90\u6216\u65b0\u95fb\u5916\u4e8b\u5b9e\u3002\u53ea\u8f93\u51fa\u4e25\u683c JSON\u3002"
            ),
        },
        {
            "role": "user",
            "content": (
                "\u5f53\u524d\u65b0\u95fb\uff1a\n"
                f"{compact_news(news, max_content=5500)}\n\n"
                f"\u5bf9\u8bdd\u5386\u53f2\uff1a{json.dumps(safe_history, ensure_ascii=False)}\n\n"
                f"\u53ef\u7528\u6765\u6e90\uff1a{json.dumps(sources, ensure_ascii=False)}\n\n"
                f"\u7528\u6237\u95ee\u9898\uff1a{question}\n\n"
                "\u8fd4\u56de JSON\uff1a"
                "{\"answer\":\"\u7b80\u6d01\u56de\u7b54\","
                "\"suggestedQuestions\":[\"\u540e\u7eed\u770b\u4ec0\u4e48\uff1f\"],"
                "\"sources\":[{\"title\":\"\u6765\u6e90\u6807\u9898\",\"url\":\"https://...\",\"snippet\":\"\u77ed\u6458\u8981\"}]}"
            ),
        },
    ]


async def enrich_article_context(state: ArticleAgentState) -> ArticleAgentState:
    news = dict(state["news"])
    content = (news.get("content") or "").strip()
    if len(content) < 900:
        fetched = await get_article_text(news.get("url"))
        if fetched:
            news["content"] = (content + "\n\n" + fetched).strip() if content else fetched
    return {"news": news}


async def collect_article_sources(state: ArticleAgentState) -> ArticleAgentState:
    news = state["news"]
    sources = normalize_sources(
        [
            {
                "title": news.get("title") or news.get("source") or "\u539f\u6587",
                "url": news.get("url"),
                "snippet": (news.get("content") or "")[:260],
            }
        ],
        max_items=1,
    )
    if should_search_web(news, state["question"]):
        sources.extend(await search_web_sources(news, state["question"]))
    return {"sources": normalize_sources(sources, max_items=search_max_results() + 1)}


async def answer_article_question(state: ArticleAgentState) -> ArticleAgentState:
    result = parse_json_response(
        await call_ai(
            build_article_agent_messages(
                news=state["news"],
                question=state["question"],
                history=state.get("history", []),
                sources=state.get("sources", []),
            ),
            temperature=0.3,
            json_mode=True,
            max_tokens_override=chat_max_tokens(),
        )
    )
    return {"result": result}


def build_article_agent_graph():
    if StateGraph is None:
        return None
    graph = StateGraph(ArticleAgentState)
    graph.add_node("enrich_article", enrich_article_context)
    graph.add_node("collect_sources", collect_article_sources)
    graph.add_node("answer", answer_article_question)
    graph.set_entry_point("enrich_article")
    graph.add_edge("enrich_article", "collect_sources")
    graph.add_edge("collect_sources", "answer")
    graph.add_edge("answer", END)
    return graph.compile()


async def run_article_agent(news: dict[str, Any], question: str, history: list[dict[str, Any]]) -> dict[str, Any]:
    if is_identity_question(question):
        return build_identity_response(news)
    if is_lightweight_chat_question(question):
        return build_lightweight_chat_response(news)
    initial_state: ArticleAgentState = {"news": news, "question": question, "history": history, "sources": []}
    graph = build_article_agent_graph()
    if graph is not None:
        state = await graph.ainvoke(initial_state)
    else:
        state = initial_state | await enrich_article_context(initial_state)
        state = state | await collect_article_sources(state)
        state = state | await answer_article_question(state)

    result = state.get("result", {})
    state_sources = state.get("sources", [])
    result_sources = result.get("sources", []) if isinstance(result.get("sources"), list) else []
    return {
        "answer": str(result.get("answer") or "").strip(),
        "suggestedQuestions": [str(item).strip() for item in result.get("suggestedQuestions", []) if str(item).strip()][:3],
        "sources": normalize_sources(result_sources + state_sources, max_items=search_max_results() + 1),
    }


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


async def call_ai(
    messages: list[dict[str, str]],
    temperature: float = 0.2,
    json_mode: bool = False,
    max_tokens_override: int | None = None,
) -> str:
    key = require_ai_key()
    payload = {
        "model": model_name(),
        "messages": messages,
        "temperature": temperature,
        "max_tokens": max_tokens_override or max_tokens(),
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


@app.post("/api/article/extract")
async def article_extract(request: ArticleExtractRequest, x_app_token: str | None = Header(default=None)) -> dict[str, Any]:
    require_client_token(x_app_token)
    return await extract_article_detail(request.news.model_dump())


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
    news = request.news.model_dump()
    history = [turn.model_dump() for turn in request.history]
    key = chat_message_cache_key(news, request.question, history)
    cached = get_cache(key)
    if cached:
        cached["cached"] = True
        return cached
    result = await run_article_agent(news, request.question, history)
    response = {
        "answer": result["answer"],
        "suggestedQuestions": result["suggestedQuestions"],
        "sources": result["sources"],
        "provider": provider(),
        "model": model_name(),
        "cached": False,
    }
    set_cache(key, response | {"cached": False})
    return response
