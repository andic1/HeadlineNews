# AI News Assistant MVP Design

## Goal

Build a practical AI news layer that uses the app's own news feed as context, recommends the most worthwhile stories, lets users open those stories, and provides a closable article-level chat assistant.

## Scope

This MVP does not introduce LangGraph. The server remains FastAPI-based with explicit prompts and typed request/response contracts. LangGraph can be added later when the assistant needs durable sessions, long-term memory, and cross-article agent workflows.

## AI Daily Rank

The app sends the currently loaded news items for the active tab to the server. The server ranks them using title, source, publish time, URL, and description, then returns 3 to 5 recommended stories. Each returned item must include `newsId`, `title`, `reason`, `url`, `source`, and `publishTime` so Android can open the exact story.

Android displays these recommendations in the existing home AI card. Tapping a recommendation first tries to match `newsId` against the current list snapshot. If no local item matches, Android creates a lightweight `NewsItem` from the AI response and opens the detail screen with the returned URL.

## Article Assistant

The detail page keeps an AI panel, but it must not permanently cover the article. The assistant has three states:

- Collapsed: a small bottom bar with the title "AI 阅读助手" and a short summary hint.
- Expanded: a chat-style panel with summary, recommended prompt chips, message history, and input.
- Closed: hidden for the current article, with a small floating reopen button.

The assistant sends the current article plus recent chat messages to the server. Responses are rendered as conversation messages rather than replacing a single answer field.

## Server API

The server exposes:

- `POST /api/ai/news-rank`: ranks feed items and returns clickable recommendations.
- `POST /api/ai/chat/message`: accepts one article, a user question, and recent history, then returns the assistant answer.

Existing endpoints remain available for backward compatibility.

## Error Handling

Android shows a compact retry state for AI rank failures and keeps the article usable if AI fails. Server failures should return normal JSON errors, not crash the app. Missing AI credentials or upstream failures should produce clear fallback messages.

## Testing

Android unit tests cover mapping of AI rank payloads and click fallback behavior where possible. Existing Gradle test and debug build must pass. Server endpoints are smoke-tested with curl after deployment.
