# AI Article Agent Design

## Goal

Upgrade the detail-page AI assistant so it can understand the current article, optionally search the web for background context, and return answers with source links.

## Scope

This iteration focuses on the detail article chat endpoint. The home `AI 今日速读` ranking flow stays fast and feed-based.

## Architecture

The server keeps FastAPI as the public API and adds a small LangGraph workflow behind `/api/ai/chat/message`. The graph reads app-provided article text, fetches the article URL when more content is needed, decides whether the user question needs web search, gathers bounded search evidence, and asks DeepSeek to produce a strict JSON answer.

The workflow is designed to degrade safely. If LangGraph, search credentials, or article fetching are unavailable, the endpoint still answers from the app-provided article context.

## Data Flow

1. Android sends `news`, `question`, and recent chat `history`.
2. Server builds article context from `content`, title, source, publish time, and URL.
3. Server fetches the original article URL only when configured and useful.
4. Server decides whether to run web search using simple keyword heuristics plus article length.
5. Server searches the web only when `TAVILY_API_KEY` is configured.
6. DeepSeek receives article context, chat history, and source snippets.
7. Server returns `answer`, `suggestedQuestions`, and `sources`.
8. Android renders sources below the latest assistant answer.

## Limits

- Search results are capped by `AI_SEARCH_MAX_RESULTS`, default `3`.
- Article fetch has its own timeout, default `8` seconds.
- Source snippets are short and link back to original URLs.
- Search is opt-in through `AI_WEB_SEARCH_ENABLED=true` and `TAVILY_API_KEY`.

## Error Handling

Article fetch/search failures never fail the whole chat response. They are represented internally as missing evidence and the model is told not to invent facts.

## Testing

Server tests cover graph routing, article source extraction, search source normalization, and the response shape. Android tests cover DTO compatibility for `sources`.
