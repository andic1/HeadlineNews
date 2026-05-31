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
