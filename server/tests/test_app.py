from fastapi.testclient import TestClient

from app.main import app


def test_health_returns_ok():
    client = TestClient(app)
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_summary_requires_ai_key_when_missing(monkeypatch):
    monkeypatch.delenv("AI_API_KEY", raising=False)
    monkeypatch.delenv("AI_APP_TOKEN", raising=False)
    client = TestClient(app)
    response = client.post(
        "/api/ai/summary",
        json={"news": {"title": "\u6d4b\u8bd5\u65b0\u95fb\u6807\u9898", "source": "\u6d4b\u8bd5\u6e90", "content": "\u8fd9\u662f\u4e00\u6bb5\u7528\u4e8e\u6d4b\u8bd5\u7684\u65b0\u95fb\u6b63\u6587\u3002"}},
    )
    assert response.status_code == 503
    assert "AI_API_KEY" in response.json()["detail"]


def test_build_summary_prompt_contains_news_title():
    from app.main import build_summary_messages

    messages = build_summary_messages({"title": "\u56fd\u4ea7\u5927\u6a21\u578b\u65b0\u95fb", "source": "\u4eca\u65e5\u5934\u6761", "content": "\u6b63\u6587\u5185\u5bb9"})
    joined = "\n".join(message["content"] for message in messages)
    assert "\u56fd\u4ea7\u5927\u6a21\u578b\u65b0\u95fb" in joined
    assert "\u4e00\u53e5\u8bdd\u6458\u8981" in joined
    assert "3\u4e2a\u8981\u70b9" in joined


def test_parse_json_response_strips_markdown_fence():
    from app.main import parse_json_response

    parsed = parse_json_response("```json\n{\"summary\":\"\u77ed\u6458\u8981\",\"bullets\":[\"\u8981\u70b91\"],\"tags\":[\"AI\"]}\n```")
    assert parsed["summary"] == "\u77ed\u6458\u8981"
    assert parsed["bullets"] == ["\u8981\u70b91"]
    assert parsed["tags"] == ["AI"]


def test_default_model_is_flash(monkeypatch):
    monkeypatch.delenv("AI_MODEL", raising=False)
    from app.main import model_name

    assert model_name() == "deepseek-v4-flash"


def test_call_ai_payload_disables_thinking(monkeypatch):
    from app.main import thinking_mode, max_tokens

    monkeypatch.delenv("AI_THINKING", raising=False)
    monkeypatch.delenv("AI_MAX_TOKENS", raising=False)
    assert thinking_mode() == "disabled"
    assert max_tokens() == 900


def test_build_news_rank_prompt_requires_clickable_fields():
    from app.main import build_news_rank_messages

    messages = build_news_rank_messages(
        [
            {
                "id": "news-1",
                "title": "\u9ad8\u6e29\u9884\u8b66",
                "source": "\u4eca\u65e5\u5934\u6761",
                "url": "https://example.com/news-1",
                "publishTime": "2026-05-31 15:44",
                "content": "\u591a\u5730\u9ad8\u6e29\u6301\u7eed\u3002",
            }
        ],
        max_items=5,
    )
    joined = "\n".join(message["content"] for message in messages)
    assert "newsId" in joined
    assert "url" in joined
    assert "\u6700\u503c\u5f97\u770b" in joined
    assert "news-1" in joined


def test_normalize_rank_items_keeps_news_url_and_source():
    from app.main import normalize_rank_items

    normalized = normalize_rank_items(
        [{"newsId": "news-1", "title": "\u9ad8\u6e29\u9884\u8b66", "reason": "\u5f71\u54cd\u8303\u56f4\u5927"}],
        [
            {
                "id": "news-1",
                "title": "\u9ad8\u6e29\u9884\u8b66",
                "source": "\u4eca\u65e5\u5934\u6761",
                "url": "https://example.com/news-1",
                "publishTime": "2026-05-31 15:44",
                "imageUrl": "https://example.com/cover.jpg",
            }
        ],
        max_items=5,
    )

    assert normalized[0]["newsId"] == "news-1"
    assert normalized[0]["url"] == "https://example.com/news-1"
    assert normalized[0]["source"] == "\u4eca\u65e5\u5934\u6761"
    assert normalized[0]["imageUrl"] == "https://example.com/cover.jpg"


def test_build_chat_message_prompt_contains_history():
    from app.main import build_chat_message_messages

    messages = build_chat_message_messages(
        {"title": "\u9ad8\u6e29\u9884\u8b66", "content": "\u591a\u5730\u9ad8\u6e29\u6301\u7eed\u3002"},
        "\u5bf9\u6211\u6709\u4ec0\u4e48\u5f71\u54cd\uff1f",
        [{"role": "user", "content": "\u8fd9\u662f\u4ec0\u4e48\uff1f"}, {"role": "assistant", "content": "\u8fd9\u662f\u5929\u6c14\u65b0\u95fb\u3002"}],
    )
    joined = "\n".join(message["content"] for message in messages)
    assert "\u9ad8\u6e29\u9884\u8b66" in joined
    assert "\u8fd9\u662f\u4ec0\u4e48" in joined
    assert "\u5bf9\u6211\u6709\u4ec0\u4e48\u5f71\u54cd" in joined


def test_should_search_web_for_latest_or_short_context():
    from app.main import should_search_web

    assert should_search_web({"title": "\u6d4b\u8bd5", "content": "\u77ed\u5185\u5bb9"}, "\u6700\u65b0\u8fdb\u5c55\u662f\u4ec0\u4e48\uff1f") is True
    assert should_search_web({"title": "\u6d4b\u8bd5", "content": "\u77ed\u5185\u5bb9"}, "\u8bb2\u89e3\u4e00\u4e0b\u8fd9\u4e2a\u65b0\u95fb") is False
    assert should_search_web({"title": "\u6d4b\u8bd5", "content": "\u8fd9\u662f\u4e00\u6bb5\u8db3\u591f\u957f\u7684\u6587\u7ae0\u5185\u5bb9" * 80}, "\u5e2e\u6211\u603b\u7ed3") is False
    assert should_search_web({"title": "\u6d4b\u8bd5", "content": "\u77ed\u5185\u5bb9"}, "\u4f60\u662f\u4ec0\u4e48\u6a21\u578b\uff1f") is False


def test_normalize_sources_keeps_titles_urls_and_limits_count():
    from app.main import normalize_sources

    sources = normalize_sources(
        [
            {"title": "\u6765\u6e901", "url": "https://example.com/a", "content": "\u5185\u5bb9A"},
            {"title": "\u91cd\u590d", "url": "https://example.com/a", "content": "\u5185\u5bb9B"},
            {"title": "\u6765\u6e902", "url": "https://example.com/b", "snippet": "\u5185\u5bb9C"},
        ],
        max_items=2,
    )

    assert sources == [
        {"title": "\u6765\u6e901", "url": "https://example.com/a", "snippet": "\u5185\u5bb9A"},
        {"title": "\u6765\u6e902", "url": "https://example.com/b", "snippet": "\u5185\u5bb9C"},
    ]


def test_build_article_agent_prompt_contains_sources():
    from app.main import build_article_agent_messages

    messages = build_article_agent_messages(
        news={"title": "\u65b0\u95fb\u6807\u9898", "content": "\u65b0\u95fb\u6b63\u6587"},
        question="\u540e\u7eed\u770b\u4ec0\u4e48\uff1f",
        history=[],
        sources=[{"title": "\u80cc\u666f\u6750\u6599", "url": "https://example.com/source", "snippet": "\u8865\u5145\u4fe1\u606f"}],
    )
    joined = "\n".join(message["content"] for message in messages)
    assert "\u65b0\u95fb\u6807\u9898" in joined
    assert "\u540e\u7eed\u770b\u4ec0\u4e48" in joined
    assert "https://example.com/source" in joined
    assert "sources" in joined


def test_chat_message_endpoint_returns_sources(monkeypatch):
    import app.main as main

    async def fake_call_ai(messages, temperature=0.2, json_mode=False, max_tokens_override=None):
        return (
            '{"answer":"\u8fd9\u662f\u5e26\u6765\u6e90\u7684\u56de\u7b54",'
            '"suggestedQuestions":["\u7ee7\u7eed\u8ffd\u95ee"],'
            '"sources":[{"title":"\u539f\u6587","url":"https://example.com/news","snippet":"\u539f\u6587\u6458\u8981"}]}'
        )

    monkeypatch.setenv("AI_API_KEY", "test-key")
    monkeypatch.delenv("AI_APP_TOKEN", raising=False)
    monkeypatch.setenv("AI_WEB_SEARCH_ENABLED", "false")
    monkeypatch.setattr(main, "call_ai", fake_call_ai)

    client = TestClient(app)
    response = client.post(
        "/api/ai/chat/message",
        json={
            "news": {"title": "\u65b0\u95fb\u6807\u9898", "url": "https://example.com/news", "content": "\u539f\u6587\u5185\u5bb9"},
            "question": "\u8fd9\u6761\u65b0\u95fb\u8bb2\u4e86\u4ec0\u4e48\uff1f",
            "history": [],
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["sources"][0]["url"] == "https://example.com/news"
    assert body["suggestedQuestions"] == ["\u7ee7\u7eed\u8ffd\u95ee"]


def test_chat_message_identity_question_skips_ai_call(monkeypatch):
    import app.main as main

    async def fail_call_ai(*args, **kwargs):
        raise AssertionError("identity question should not call the model")

    monkeypatch.setenv("AI_API_KEY", "test-key")
    monkeypatch.delenv("AI_APP_TOKEN", raising=False)
    monkeypatch.setenv("AI_WEB_SEARCH_ENABLED", "true")
    monkeypatch.setattr(main, "call_ai", fail_call_ai)
    monkeypatch.setattr(main, "fetch_article_text", lambda url: (_ for _ in ()).throw(AssertionError("should not fetch article")))
    monkeypatch.setattr(main, "search_web_sources", lambda news, question: (_ for _ in ()).throw(AssertionError("should not search web")))

    client = TestClient(app)
    response = client.post(
        "/api/ai/chat/message",
        json={
            "news": {"title": "\u65b0\u95fb\u6807\u9898", "url": "https://example.com/news", "content": "\u77ed\u5185\u5bb9"},
            "question": "\u4f60\u662f\u4ec0\u4e48\u6a21\u578b\uff1f",
            "history": [],
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert "DeepSeek" in body["answer"]
    assert body["sources"][0]["url"] == "https://example.com/news"


def test_chat_message_reuses_cached_answer(monkeypatch, tmp_path):
    import app.main as main

    calls = {"ai": 0}

    async def fake_call_ai(messages, temperature=0.2, json_mode=False, max_tokens_override=None):
        calls["ai"] += 1
        return (
            '{"answer":"\u7b2c\u4e00\u6b21\u751f\u6210\u7684\u56de\u7b54",'
            '"suggestedQuestions":["\u7ee7\u7eed\u8ffd\u95ee"],'
            '"sources":[{"title":"\u539f\u6587","url":"https://example.com/news","snippet":"\u539f\u6587\u6458\u8981"}]}'
        )

    monkeypatch.setenv("AI_API_KEY", "test-key")
    monkeypatch.delenv("AI_APP_TOKEN", raising=False)
    monkeypatch.setenv("AI_WEB_SEARCH_ENABLED", "false")
    monkeypatch.setattr(main, "DB_PATH", tmp_path / "cache.sqlite3")
    monkeypatch.setattr(main, "call_ai", fake_call_ai)

    client = TestClient(app)
    payload = {
        "news": {
            "id": "news-1",
            "title": "\u65b0\u95fb\u6807\u9898",
            "url": "https://example.com/news",
            "content": "\u8fd9\u662f\u4e00\u6bb5\u8db3\u591f\u957f\u7684\u65b0\u95fb\u5185\u5bb9" * 100,
        },
        "question": "\u8bb2\u89e3\u4e00\u4e0b\u8fd9\u4e2a\u65b0\u95fb",
        "history": [],
    }

    first = client.post("/api/ai/chat/message", json=payload)
    second = client.post("/api/ai/chat/message", json=payload)

    assert first.status_code == 200
    assert second.status_code == 200
    assert calls["ai"] == 1
    assert first.json()["cached"] is False
    assert second.json()["cached"] is True


def test_enrich_article_context_reuses_cached_article_text(monkeypatch, tmp_path):
    import asyncio
    import app.main as main

    calls = {"fetch": 0}

    async def fake_fetch_article_text(url):
        calls["fetch"] += 1
        return "\u539f\u6587\u6293\u53d6\u5185\u5bb9" * 80

    monkeypatch.setattr(main, "DB_PATH", tmp_path / "cache.sqlite3")
    monkeypatch.setattr(main, "fetch_article_text", fake_fetch_article_text)

    state = {
        "news": {"title": "\u77ed\u65b0\u95fb", "url": "https://example.com/news", "content": "\u77ed\u5185\u5bb9"},
        "question": "\u8bb2\u89e3\u4e00\u4e0b",
        "history": [],
    }

    first = asyncio.run(main.enrich_article_context(state))
    second = asyncio.run(main.enrich_article_context(state))

    assert calls["fetch"] == 1
    assert "\u539f\u6587\u6293\u53d6\u5185\u5bb9" in first["news"]["content"]
    assert "\u539f\u6587\u6293\u53d6\u5185\u5bb9" in second["news"]["content"]
