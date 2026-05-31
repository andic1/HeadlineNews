# AI News Assistant MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add clickable AI news recommendations and a closable conversational article assistant backed by FastAPI endpoints.

**Architecture:** Android sends current feed/article context to the AI server through typed DTOs. The server ranks feed items and answers article questions using explicit prompts. Android keeps the UI resilient by allowing AI failure without blocking news reading.

**Tech Stack:** Kotlin, Jetpack Compose, Retrofit, kotlinx.serialization, FastAPI, DeepSeek-compatible chat completions.

---

### Task 1: Android AI Contracts

**Files:**
- Modify: `android/app/src/main/java/com/demo/toutiao/data/api/AiApi.kt`
- Modify: `android/app/src/main/java/com/demo/toutiao/data/repo/AiRepository.kt`
- Modify: `android/app/src/test/java/com/demo/toutiao/data/repo/AiRepositoryTest.kt`

- [ ] Add `newsRank` and `chatMessage` endpoints.
- [ ] Add `newsId`, `category`, and `imageUrl` fields to AI payloads.
- [ ] Preserve existing summary/chat endpoints for compatibility.
- [ ] Run `.\gradlew.bat :app:testDebugUnitTest --tests com.demo.toutiao.data.repo.AiRepositoryTest --no-daemon`.

### Task 2: Clickable Home AI Rank

**Files:**
- Modify: `android/app/src/main/java/com/demo/toutiao/ui/home/AiBriefCard.kt`
- Modify: `android/app/src/main/java/com/demo/toutiao/ui/home/HomeScreen.kt`
- Modify: `android/app/src/main/java/com/demo/toutiao/ui/home/HomeViewModel.kt`

- [ ] Rename behavior from brief-only to rank recommendations while keeping UI copy as `AI 今日速读`.
- [ ] Pass a click handler into the AI card.
- [ ] On click, open the matching local `NewsItem`; if missing, create a fallback item from the AI response URL.
- [ ] Keep loading/error states compact.

### Task 3: Detail Chat Assistant UI

**Files:**
- Modify: `android/app/src/main/java/com/demo/toutiao/ui/detail/DetailViewModel.kt`
- Modify: `android/app/src/main/java/com/demo/toutiao/ui/detail/DetailScreen.kt`
- Create: `android/app/src/main/java/com/demo/toutiao/ui/ai/AiChatMessage.kt`

- [ ] Add chat message history in the view model.
- [ ] Replace the single answer text with conversation bubbles.
- [ ] Add collapsed, expanded, and closed assistant states.
- [ ] Add a reopen floating action when closed.

### Task 4: Server API

**Files:**
- Modify remote server files under `/opt/toutiao-ai`.

- [ ] Add request/response models for `/api/ai/news-rank`.
- [ ] Add request/response models for `/api/ai/chat/message`.
- [ ] Implement prompts that use only app-provided news context.
- [ ] Keep old endpoints working.
- [ ] Smoke-test `/health`, `/api/ai/news-rank`, and `/api/ai/chat/message`.

### Task 5: Verification

**Files:**
- No new files.

- [ ] Run `.\gradlew.bat :app:testDebugUnitTest --no-daemon`.
- [ ] Run `.\gradlew.bat :app:assembleDebug --no-daemon`.
- [ ] Confirm server health responds.
- [ ] Summarize any residual risks.
