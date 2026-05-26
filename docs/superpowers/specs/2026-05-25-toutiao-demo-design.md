# 今日头条首页 Demo —— 设计文档

**日期**：2026-05-25
**作者**：brainstorm 会话
**项目根目录**：`D:\toutiao-demo\`

## 1. 背景与目标

仿今日头条首页的列表页 Demo，用于练习 Kotlin + Compose + 协程 + 本地数据库 + 全栈技能栈。

**必须功能**：数据加载、卡片展示。
**进阶功能**：加载状态控制、下拉刷新和加载更多、数据库存储、多 Tab 分类、顶栏与底栏的静态 UI 还原。

## 2. 技术栈

| 层 | 技术 |
|---|---|
| 安卓客户端 | Kotlin、Jetpack Compose、Material3、Coroutines + Flow、Hilt（DI）、Retrofit + OkHttp、Room、Paging3 + RemoteMediator、Coil |
| 后端 | Ktor、Exposed ORM、HikariCP、Ktor Client（调 whyta）、kotlinx.serialization |
| 数据库 | 本地 MySQL（utf8mb4） |
| 外部数据源 | https://apis.whyta.cn/ （需 APIKEY，配置时填写） |
| 测试 | JUnit5/JUnit4、MockK、Turbine、H2（后端测试库）、Room in-memory |

## 3. 目录结构

```
D:\toutiao-demo\
├── backend\         Ktor 服务
│   └── src\main\kotlin\com\demo\toutiao\
│       ├── Application.kt
│       ├── routes\NewsRoute.kt
│       ├── service\NewsService.kt
│       ├── client\WhytaClient.kt
│       ├── repo\NewsRepository.kt
│       ├── db\Tables.kt
│       └── model\NewsDto.kt
└── android\         安卓 App
    └── app\src\main\java\com\demo\toutiao\
        ├── MainActivity.kt
        ├── ui\home\{HomeScreen,HomeViewModel,NewsCard}.kt
        ├── ui\theme\
        ├── data\api\NewsApi.kt
        ├── data\db\{AppDatabase,NewsDao,NewsEntity,RemoteKeysEntity}.kt
        ├── data\repo\NewsRepository.kt
        ├── data\paging\NewsRemoteMediator.kt
        └── di\AppModule.kt
```

## 4. 数据流

```
HomeScreen → ViewModel.refresh() → Repository (Paging3 + RemoteMediator)
   → Retrofit GET /api/news?category=推荐&page=1
       → Backend NewsService 检查 MySQL (10 min TTL)
          ├ 命中 → 直接返回
          └ 未命中 → WhytaClient 调 whyta → 入 MySQL → 返回
   → 客户端写入 Room → Flow 触发 UI 重组
```

双层缓存：MySQL（后端，10 分钟 TTL）+ Room（客户端，single source of truth）。

## 5. 后端接口契约

### 端点：`GET /api/news`

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `category` | string | 是 | - | `关注` / `推荐` / `热榜` / `新时代` / `小说` / `视频` |
| `page` | int | 否 | 1 | 翻页，从 1 开始 |
| `pageSize` | int | 否 | 20 | 1-50 |
| `forceRefresh` | bool | 否 | false | true = 跳过缓存，强制打 whyta |

### Tab → whyta 接口映射（后端硬编码）

| 中文 Tab | whyta 接口 |
|---|---|
| 关注 | `/api/tx/news`（国内新闻） |
| 推荐 | `/api/tx/generalnews`（混合全频道） |
| 热榜 | `/api/tx/hotnews`（互联网热搜） |
| 新时代 | `/api/tx/topnews`（今日头条，注意是 2019-2022 历史数据） |
| 小说 | 返回空 list + `msg="暂未接入"` |
| 视频 | 返回空 list + `msg="暂未接入"` |

### 成功响应

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "category": "推荐",
    "page": 1,
    "pageSize": 20,
    "hasMore": true,
    "fromCache": true,
    "list": [
      {
        "id": "6010bdf6...",
        "title": "...",
        "description": "...",
        "source": "...",
        "publishTime": "2024-01-15 10:30:00",
        "imageUrl": "https://...",
        "originalUrl": "https://...",
        "layoutType": "TEXT_WITH_THUMB"
      }
    ]
  }
}
```

### `layoutType` 由后端决定

- `TEXT_ONLY` — 无 `imageUrl`
- `TEXT_WITH_THUMB` — 有 `imageUrl` 时默认
- `BIG_IMAGE` — 每隔 5 条挑一个有大图的 item 改为此类型（简单轮换规则）

### 错误响应

```json
{ "code": 1001, "msg": "whyta api timeout", "data": null }
```

错误码：`1001` whyta 调用失败 / `1002` 参数非法 / `1003` 服务内部错误。

### TTL 缓存策略

- 每个 `(category, page)` 在 `category_cache_meta` 有一条 `last_fetched_at`
- `now - last_fetched_at < 10 分钟` → 返回 MySQL 缓存（`fromCache=true`）
- 否则 → 调 whyta → upsert 到 `news` → 更新 meta → 返回（`fromCache=false`）
- `forceRefresh=true` 时跳过 TTL 检查

## 6. 数据库 Schema

### 后端 MySQL（database: `toutiao_demo`，字符集 `utf8mb4_0900_ai_ci`）

```sql
CREATE TABLE news (
  id            VARCHAR(64)  PRIMARY KEY,
  category      VARCHAR(16)  NOT NULL,
  title         VARCHAR(512) NOT NULL,
  description   TEXT,
  source        VARCHAR(128),
  image_url     VARCHAR(1024),
  original_url  VARCHAR(1024),
  publish_time  DATETIME,
  layout_type   VARCHAR(32)  NOT NULL,
  fetched_at    DATETIME     NOT NULL,
  page          INT          NOT NULL,
  position      INT          NOT NULL,
  INDEX idx_cat_page (category, page, position),
  INDEX idx_publish (publish_time DESC)
);

CREATE TABLE category_cache_meta (
  category        VARCHAR(16) NOT NULL,
  page            INT         NOT NULL,
  last_fetched_at DATETIME    NOT NULL,
  has_more        BOOLEAN     NOT NULL DEFAULT TRUE,
  total_count     INT,
  PRIMARY KEY (category, page)
);
```

`id` 用 whyta 的 id 做主键，upsert 时同一条新闻多次出现以最新归属为准。Demo 不做精确的多分类关联表。

### 安卓 Room（database: `toutiao.db`，version 1）

```kotlin
@Entity(tableName = "news", primaryKeys = ["id", "category"])
data class NewsEntity(
  val id: String,
  val category: String,
  val title: String,
  val description: String?,
  val source: String?,
  val imageUrl: String?,
  val originalUrl: String?,
  val publishTime: String?,
  val layoutType: String,
  val position: Int,
  val cachedAt: Long
)

@Entity(tableName = "remote_keys")
data class RemoteKeysEntity(
  @PrimaryKey val category: String,
  val nextPage: Int?,
  val lastUpdated: Long
)
```

DAO 核心查询：
```kotlin
@Query("SELECT * FROM news WHERE category = :cat ORDER BY position ASC")
fun pagingSource(cat: String): PagingSource<Int, NewsEntity>

@Query("DELETE FROM news WHERE category = :cat")
suspend fun clearCategory(cat: String)

@Upsert
suspend fun upsertAll(items: List<NewsEntity>)
```

## 7. 客户端 UI 层

### 屏幕结构

```
┌─────────────────────────────────────┐
│  HomeTopBar   红底，状态条 + 14° + 搜索框 + A登录   │ 静态壳
├─────────────────────────────────────┤
│  CategoryTabRow   关注 推荐* 热榜 新时代 小说 视频     │
├─────────────────────────────────────┤
│  NewsList (HorizontalPager + 每个 Tab 一个 LazyColumn)│
│    NewsCard(TEXT_ONLY) / (TEXT_WITH_THUMB) / (BIG_IMAGE)│
│    LoadMoreFooter                   │
├─────────────────────────────────────┤
│  BottomNavBar   首页* 西瓜 放映厅 未登录 我的         │ 静态壳
└─────────────────────────────────────┘
```

### Compose 组件

- `HomeScreen(viewModel)` — Scaffold + topBar + bottomBar
- `HomeTopBar()` — 红底状态条 + 天气 + 搜索框（无交互）
- `CategoryTabRow(tabs, selected, onSelect)` — ScrollableTabRow
- `NewsPager(categories)` — HorizontalPager，每个 Tab 独立 ViewModel，首次进入时才请求
- `NewsList(lazyPagingItems, onRefresh)` — PullToRefreshBox + LazyColumn
- `NewsCard(item)` — `when(layoutType)` 三种渲染
- `LoadStateFooter(state)` — Paging3 append 状态
- `EmptyState()` / `ErrorState(onRetry)`

### ViewModel（每个 Tab 独立实例）

```kotlin
class NewsViewModel @AssistedInject constructor(
  @Assisted val category: String,
  private val repo: NewsRepository
) : ViewModel() {
  val pagingFlow: Flow<PagingData<NewsItem>> =
    repo.pagingFlow(category).cachedIn(viewModelScope)
}
```

### Repository + RemoteMediator

```kotlin
fun pagingFlow(category: String): Flow<PagingData<NewsItem>> = Pager(
  config = PagingConfig(pageSize = 20, prefetchDistance = 5),
  remoteMediator = NewsRemoteMediator(category, api, db),
  pagingSourceFactory = { db.newsDao().pagingSource(category) }
).flow.map { it.map(NewsEntity::toDomain) }
```

`NewsRemoteMediator.load()`：
- `REFRESH` → 调 `forceRefresh=true`，清表后插入
- `APPEND` → 用 `RemoteKeys.nextPage` 请求下一页，追加
- `PREPEND` → 直接返回 `endOfPaginationReached = true`

### 加载状态对照表

| 场景 | 渲染 |
|---|---|
| 首次进入 Tab，无缓存 | 全屏转圈 |
| 首次进入 Tab，有缓存 | 直接渲染 Room 数据，后台静默刷新 |
| 下拉刷新 | PullToRefreshBox 顶部加载条 |
| 上拉加载更多 | 底部 footer 转圈 |
| 请求失败且无数据 | 全屏 ErrorState + 重试 |
| 请求失败但有缓存 | Snackbar 提示，列表照常显示 |
| 列表为空（如小说/视频） | EmptyState "暂无内容" |

### 主题
- 主色 `#FF2A2A`（头条红），背景 `#F5F5F5`，卡片白底
- 标题 16sp 加粗 / 描述 13sp / 来源时间 12sp 灰
- 字体：系统默认

## 8. 错误处理与离线策略

### 后端 whyta 调用容错（`WhytaClient`）

| 情况 | 处理 |
|---|---|
| HTTP 4xx | `WhytaApiException` → 返客户端 `code=1001` |
| HTTP 5xx / 超时（10s） | 重试 1 次（指数退避 500ms），仍败 → 看 MySQL 是否有旧缓存，有则返 `fromCache=true, stale=true` |
| whyta 业务 `code != 200` | 同 4xx |
| 网络异常 | 同 5xx |

全局异常用 Ktor `StatusPages` 统一兜底为 `{ code, msg, data: null }`。

### 客户端

- Retrofit + OkHttp 超时 15s
- 拦截器把后端 `code != 0` 抛成 `BackendException(code, msg)`
- 无网络时 `IOException` → Repository 不抛，直接走 Room 数据

错误展示对照见 §7 加载状态表。Room 是 single source of truth，断网时 UI 直接渲染缓存。

## 9. 测试策略

> Demo 性质，只测关键逻辑。

### 后端（JUnit5 + MockK + H2 内存库 + Exposed）

| 测试目标 | 测试类 |
|---|---|
| TTL 命中 → 不调 whyta | `NewsServiceTest` |
| TTL 过期 → 调 whyta + upsert | `NewsServiceTest` |
| whyta 挂 → 返回 stale 缓存 | `NewsServiceTest` |
| whyta JSON → DTO 解析 | `WhytaClientTest` |
| layoutType 决策 | `LayoutDeciderTest` |
| 路由参数校验 + 响应包装 | `NewsRouteTest`（Ktor testApplication） |

### 客户端（JUnit4 + Turbine + MockK + Room in-memory）

| 测试目标 | 测试类 |
|---|---|
| RemoteMediator REFRESH 清表写新页 | `NewsRemoteMediatorTest` |
| RemoteMediator APPEND 用 RemoteKeys 翻页 | 同上 |
| RemoteMediator 网络错 → Error | 同上 |
| DTO → Entity 映射 | `NewsRepositoryTest` |
| ViewModel PagingFlow 发出数据 | `NewsViewModelTest`（Turbine） |

不写 Compose UI 测试，配置成本相对 demo 收益过高。

### 手测 checklist
- [ ] 后端启动 `/api/news?category=推荐` 200，list 非空
- [ ] 安卓 6 个 Tab 可切换；小说/视频显示"暂无内容"
- [ ] 下拉刷新触发顶部加载条
- [ ] 滑到底自动加载下一页
- [ ] 关闭网络重启 App，Tab 显示上次缓存
- [ ] 关后端，App 显示 Snackbar，列表保留
- [ ] 三种 layoutType 都有渲染

## 10. 配置项（运行前需准备）

- 本机 MySQL：`localhost:3306`，账号密码自定义，建库 `toutiao_demo`
- 后端 `application.conf`：
  - `db.url` / `db.user` / `db.password`
  - `whyta.apiKey`（用户后续填写）
  - `whyta.baseUrl = https://whyta.cn`
  - `cache.ttlMinutes = 10`
- 安卓 `local.properties` 或 `BuildConfig`：
  - `BACKEND_BASE_URL = http://10.0.2.2:8080`（模拟器 → 宿主机）

## 11. 范围外（YAGNI）

- 用户系统、登录、关注关系
- 点赞 / 评论 / 分享 / 收藏
- 详情页（卡片点击只是跳浏览器开 `originalUrl`，不做内置 WebView 详情）
- 多 Tab 之间数据共享/去重
- 图片缓存策略调优（用 Coil 默认）
- 主题切换 / 多语言
- CI/CD、Crash 上报、埋点
