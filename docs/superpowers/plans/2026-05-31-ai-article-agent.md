# AI Article Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a LangGraph-backed article assistant that can read article context, optionally search the web, and return source-backed answers.

**Architecture:** FastAPI keeps the public `/api/ai/chat/message` endpoint while routing its implementation through a small server-side article graph. Android keeps the same chat UX and adds rendering for source links returned by the server.

**Tech Stack:** FastAPI, httpx, LangGraph, DeepSeek-compatible chat completions, Kotlin serialization, Jetpack Compose.

---

### Task 1: Server Agent Tests

**Files:**
- Modify: `server/tests/test_app.py`

- [ ] Add tests for `should_search_web`, `normalize_sources`, `build_article_agent_messages`, and `/api/ai/chat/message` response source shape.
- [ ] Run remote server pytest against old code and verify the new tests fail before implementation.

### Task 2: Server LangGraph Workflow

**Files:**
- Modify: `server/app/main.py`
- Modify: `server/requirements.txt`
- Modify: `server/.env.example`

- [ ] Add source DTOs and response source fields.
- [ ] Add bounded article fetch and search helpers.
- [ ] Add `build_article_agent_graph()` using LangGraph `StateGraph`.
- [ ] Route `/api/ai/chat/message` through the graph and preserve compatibility.
- [ ] Keep graceful fallback when search or LangGraph is unavailable.

### Task 3: Android Source Rendering

**Files:**
- Modify: `android/app/src/main/java/com/demo/toutiao/data/api/AiApi.kt`
- Modify: `android/app/src/main/java/com/demo/toutiao/ui/ai/AiChatMessage.kt`
- Modify: `android/app/src/main/java/com/demo/toutiao/ui/detail/DetailViewModel.kt`
- Modify: `android/app/src/main/java/com/demo/toutiao/ui/detail/DetailScreen.kt`
- Modify: `android/app/src/test/java/com/demo/toutiao/ui/ai/AiChatMessageTest.kt`

- [ ] Add `AiSource` DTO.
- [ ] Store assistant sources in `AiChatMessage`.
- [ ] Render source chips below assistant answers.
- [ ] Preserve chat behavior when sources are empty.

### Task 4: Verification and Deploy

**Files:**
- Remote: `/opt/toutiao-ai`

- [ ] Run server tests on the cloud virtualenv.
- [ ] Install new server dependencies from a China-friendly pip mirror.
- [ ] Upload changed server files and restart `toutiao-ai.service`.
- [ ] Smoke test `/health` and `/api/ai/chat/message`.
- [ ] Run targeted Android unit tests or compile check unless the user asks to skip.
