# Pinned News Feature Specification

## Overview
Add a "pinned news" section at the top of news feeds, displaying high-value/influential news from the day. Styled to match real Toutiao app with red titles and banner background.

## Requirements

### Functional
- Display 3 pinned news items at the top of each feed (configurable, max 10)
- Fetch pinned news from whyta hotnews API
- Show pinned section in all tabs EXCEPT "热榜" (Hot) tab
- Pinned news titles displayed in red color
- First pinned item has red background banner
- Clicking pinned item navigates to news detail

### Non-Functional
- Cache pinned news for 5 minutes (shorter than regular 10min cache)
- Graceful degradation: if pinned fetch fails, show regular news only
- Network timeout: 5 seconds for pinned news requests

## Architecture

### Backend Changes

**New endpoint:**
```
GET /api/news/pinned?limit=3
```

**Response:**
```json
{
  "items": [
    {
      "id": "...",
      "title": "...",
      "description": "...",
      "source": "...",
      "imageUrl": "...",
      "originalUrl": "...",
      "publishTime": "...",
      "layoutType": "..."
    }
  ]
}
```

**Implementation:**
- Reuse `WhytaClient.fetch(category="热榜", page=1, pageSize=limit)`
- Cache in `pinned_news` table (separate from regular news cache)
- TTL: 5 minutes

**Database:**
- Reuse existing `news` table with `category="__pinned__"`, `page=0`
- Reuse existing `category_cache_meta` table for cache metadata

### Android Changes

**ViewModel:**
- Add `pinnedNewsFlow: StateFlow<List<NewsDto>>` to `HomeViewModel`
- Fetch pinned news on init
- Expose to UI

**UI Components:**

1. **PinnedNewsSection** (new composable):
   - Renders list of pinned items
   - Applies red styling
   - Adds divider separator

2. **PinnedNewsCard** (new composable):
   - Single pinned news item
   - Red title text: `Color(0xFFE03E2D)`
   - First item background: `Color(0xFFFFEBE8)` (light red banner)
   - Optional "置顶" badge

3. **NewsList** (modified):
   - Accept optional `pinnedItems: List<NewsDto>?` parameter
   - Render `PinnedNewsSection` as LazyColumn header if non-null

4. **HomeScreen** (modified):
   - Pass `viewModel.pinnedNewsFlow` to each page
   - Skip pinned section for "热榜" tab

## Data Flow

```
1. App launch
   ↓
2. HomeViewModel.init() calls repository.getPinnedNews()
   ↓
3. Backend checks cache (5min TTL)
   ↓
4. If expired: fetch from whyta hotnews API
   ↓
5. Cache results in DB
   ↓
6. Return to Android
   ↓
7. HomeScreen renders pinned section in each tab (except 热榜)
```

## Error Handling

- **Network failure:** Serve stale cache if available, otherwise show regular news only
- **API error:** Log error, show regular news only (no error UI to user)
- **Timeout:** 5 seconds, then fallback to regular news
- **Empty result:** Show regular news only

## Configuration

Add to `backend/src/main/resources/application.yaml`:

```yaml
pinned:
  enabled: true
  limit: 3
  ttlMinutes: 5
```

## UI Mockup

```
┌─────────────────────────────────────┐
│ [置顶] 重大新闻标题                    │  ← Red background banner
│        (红色文字)                      │     Red title text
├─────────────────────────────────────┤
│ [置顶] 第二条置顶新闻                  │  ← Normal background
│        (红色文字)                      │     Red title text
├─────────────────────────────────────┤
│ [置顶] 第三条置顶新闻                  │  ← Normal background
│        (红色文字)                      │     Red title text
├─────────────────────────────────────┤  ← Divider
│ 普通新闻标题                           │
│ (黑色文字)                             │
├─────────────────────────────────────┤
│ ...                                 │
└─────────────────────────────────────┘
```

## Testing

- Verify pinned news appears in all tabs except "热榜"
- Verify red styling (title color, first item background)
- Verify cache expiration (5 minutes)
- Verify graceful degradation on network failure
- Verify click navigation to detail screen

## Future Enhancements

- Allow user to dismiss pinned items
- Personalized pinned news based on user interests
- Admin panel to manually curate pinned items
