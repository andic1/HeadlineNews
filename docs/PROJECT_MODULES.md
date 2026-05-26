# 今日头条 Demo — 项目模块分析文档

## 一、项目概述

本项目是一个仿今日头条新闻客户端的完整前后端实现，包含 **Android 客户端** 和 **Kotlin 后端服务**两大部分。

| 维度 | 技术选型 |
|------|---------|
| **Android 前端** | Kotlin · Jetpack Compose · Hilt · Room · Paging3 · Retrofit · Coil · Navigation Compose |
| **后端服务** | Kotlin · Ktor (Netty) · Exposed ORM · HikariCP · MySQL 8 |
| **数据来源** | whyta.cn 新闻聚合 API |
| **编译环境** | JDK 17 · Gradle · Android SDK 34 (minSdk 24) |

### 整体架构

```
┌────────────────────┐         ┌──────────────────────┐        ┌─────────────┐
│   Android 客户端    │  HTTP   │    Ktor 后端服务       │  HTTP  │  whyta API  │
│  (Compose + MVVM)  │ ──────▸ │ (路由→服务→仓库→MySQL) │ ─────▸ │ (新闻数据源) │
└────────────────────┘         └──────────────────────┘        └─────────────┘
```

- **Android 端**通过 Retrofit 调用后端 `/api/news` 接口
- **后端**接收请求后，先查 MySQL 缓存，过期则调 whyta API 拉取最新数据并入库
- **Android 端**使用 Room + Paging3 做本地缓存与分页加载

---

## 二、项目目录结构

```
toutiao-demo/
├── android/                          # Android 客户端
│   └── app/src/main/java/com/demo/toutiao/
│       ├── MainActivity.kt           # 入口 Activity + 导航配置
│       ├── ToutiaoApp.kt             # Application（Hilt 初始化）
│       ├── data/
│       │   ├── api/
│       │   │   ├── Dto.kt            # 网络层数据模型
│       │   │   └── NewsApi.kt        # Retrofit 接口定义
│       │   ├── db/
│       │   │   ├── AppDatabase.kt    # Room 数据库
│       │   │   ├── NewsDao.kt        # DAO 接口
│       │   │   └── NewsEntity.kt     # 数据库实体 + 转换函数
│       │   ├── model/
│       │   │   └── NewsItem.kt       # UI 领域模型
│       │   ├── paging/
│       │   │   └── NewsRemoteMediator.kt  # Paging3 RemoteMediator
│       │   └── repo/
│       │       └── NewsRepository.kt # 数据仓库（Pager 构建）
│       ├── di/
│       │   └── AppModule.kt          # Hilt 依赖注入模块
│       └── ui/
│           ├── detail/
│           │   └── DetailScreen.kt   # 新闻详情页（WebView）
│           ├── home/
│           │   ├── BottomNavBar.kt    # 底部导航栏
│           │   ├── CategoryTabRow.kt  # 分类标签页
│           │   ├── HomeScreen.kt      # 首页主界面
│           │   ├── HomeTopBar.kt      # 顶部搜索栏
│           │   ├── HomeViewModel.kt   # ViewModel
│           │   ├── NewsCard.kt        # 新闻卡片组件
│           │   └── NewsList.kt        # 分页列表 + 下拉刷新
│           └── theme/
│               └── Theme.kt          # 主题色彩定义
│
├── backend/                          # Ktor 后端服务
│   └── src/main/kotlin/com/demo/toutiao/
│       ├── Application.kt            # 服务入口 + Ktor 插件配置
│       ├── client/
│       │   └── WhytaClient.kt        # whyta API HTTP 客户端
│       ├── db/
│       │   ├── Database.kt           # MySQL 连接池初始化
│       │   └── Tables.kt             # Exposed 表定义
│       ├── model/
│       │   └── Dto.kt                # 响应数据模型 + 错误码
│       ├── repo/
│       │   └── NewsRepository.kt     # 数据库操作（缓存读写）
│       ├── routes/
│       │   └── NewsRoute.kt          # API 路由定义
│       └── service/
│           └── NewsService.kt        # 核心业务逻辑
│
└── docs/
    └── PROJECT_MODULES.md            # 本文档
```

---

## 三、后端模块详解

### 3.1 服务入口 — `Application.kt`

**文件**：`backend/src/main/kotlin/com/demo/toutiao/Application.kt`

本文件是整个 Ktor 后端的启动入口，职责：

1. **读取配置**：从 `application.yaml` 加载数据库、whyta API、缓存等配置项
2. **初始化数据库**：调用 `initDatabase()` 创建 HikariCP 连接池并连接 MySQL
3. **组装依赖**：手动创建 `WhytaClient` → `NewsRepository` → `NewsService` 实例链
4. **安装 Ktor 插件**：
   - `ContentNegotiation`：JSON 序列化/反序列化（kotlinx.serialization）
   - `CallLogging`：请求日志（INFO 级别）
   - `CORS`：跨域支持（`anyHost()`）
   - `StatusPages`：全局异常处理，包括：
     - `WhytaApiException` → 返回 `code=1001` 的业务错误
     - `IllegalArgumentException` → 返回 `code=1002` 的参数错误
     - `Throwable` → 返回 `code=1003` 的内部错误
5. **注册路由**：挂载 `newsRoutes(service)`

### 3.2 API 路由 — `NewsRoute.kt`

**文件**：`backend/src/main/kotlin/com/demo/toutiao/routes/NewsRoute.kt`

定义了两个端点：

| 端点 | 方法 | 功能 |
|------|------|------|
| `GET /api/news` | GET | 获取新闻列表 |
| `GET /health` | GET | 健康检查 |

**`/api/news` 参数**：

| 参数 | 类型 | 必选 | 默认值 | 说明 |
|------|------|------|--------|------|
| `category` | String | 是 | — | 分类名，必须为：`关注`、`推荐`、`热榜`、`新时代` |
| `page` | Int | 否 | 1 | 页码（≥1） |
| `pageSize` | Int | 否 | 20 | 每页条数（1~50） |
| `forceRefresh` | Boolean | 否 | false | 是否强制刷新缓存 |

**校验逻辑**：
- `category` 为空或不在合法集合中 → 400 + `BAD_PARAM`
- `page < 1` 或 `pageSize` 不在 1..50 → 400 + `BAD_PARAM`

### 3.3 核心服务 — `NewsService.kt`

**文件**：`backend/src/main/kotlin/com/demo/toutiao/service/NewsService.kt`

核心业务逻辑类，主要流程：

```
loadNews(category, page, pageSize, forceRefresh)
    │
    ├── 缓存有效（TTL 内且有数据）？ → 直接返回缓存数据（fromCache=true）
    │
    ├── 分类为"推荐"？ → 走聚合模式 fetchAggregate()
    │                     并发请求多个 whyta 端点 → 合并去重打乱 → 返回
    │
    ├── 其他分类 → fetchWithRetry()
    │              首次失败且非 4xx → 延迟 500ms 后重试一次
    │
    └── 所有远程请求失败 → 降级返回过期缓存（兜底策略）
```

**关键设计**：
- **TTL 缓存**：配置 `cache.ttlMinutes=10`，10 分钟内同一 category+page 直接返回缓存
- **聚合模式**（"推荐"频道）：使用 `coroutineScope + async` 并发调用 4 个 whyta 端点，每个取少量条目，合并后 `distinctBy { id }` 去重 → `shuffled()` 随机打乱 → `take(pageSize)` 截取
- **重试策略**：非 4xx 错误自动重试一次，延迟 500ms
- **降级策略**：远程拉取全部失败时，返回 MySQL 中的过期缓存数据
- **布局分配**：根据新闻是否有图片和在列表中的位置，分配 `TEXT_ONLY`、`TEXT_WITH_THUMB`、`BIG_IMAGE` 三种布局类型

### 3.4 外部 API 客户端 — `WhytaClient.kt`

**文件**：`backend/src/main/kotlin/com/demo/toutiao/client/WhytaClient.kt`

封装对 whyta.cn 新闻 API 的 HTTP 调用：

- **HTTP 客户端**：Ktor CIO 引擎 + `ContentNegotiation`（JSON） + `HttpTimeout`（可配置超时）
- **分类映射**：

| 分类 | whyta 端点 |
|------|-----------|
| 关注 | `/api/tx/news` |
| 推荐 | `/api/tx/generalnews` |
| 热榜 | `/api/tx/hotnews` |
| 新时代 | `/api/tx/topnews` |

- **响应解析**（`parseList`）：
  - whyta 不同端点返回的 JSON 结构不同（`list` / `newslist` / `data`），统一适配
  - 提取 `id`、`title`、`description`、`source`、`picUrl`、`url`、`ctime` 等字段
- **URL 规范化**：自动补全 `//` 开头或裸域名的图片/文章 URL 为完整 `https://` 地址

### 3.5 数据库层

#### 3.5.1 连接初始化 — `Database.kt`

**文件**：`backend/src/main/kotlin/com/demo/toutiao/db/Database.kt`

- 使用 **HikariCP** 连接池管理 MySQL 连接
- 驱动：`com.mysql.cj.jdbc.Driver`（MySQL 8.x）
- 事务隔离级别：`REPEATABLE_READ`
- 连接池大小通过 `application.yaml` 配置（默认 8）

#### 3.5.2 表定义 — `Tables.kt`

**文件**：`backend/src/main/kotlin/com/demo/toutiao/db/Tables.kt`

使用 Exposed ORM 定义两张表：

**`news` 表**（新闻缓存）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | VARCHAR(64) | 主键，新闻唯一 ID |
| `category` | VARCHAR(16) | 所属分类 |
| `title` | VARCHAR(512) | 标题 |
| `description` | TEXT | 摘要（可空） |
| `source` | VARCHAR(128) | 来源（可空） |
| `image_url` | VARCHAR(1024) | 图片 URL（可空） |
| `original_url` | VARCHAR(1024) | 原文 URL（可空） |
| `publish_time` | DATETIME | 发布时间（可空） |
| `layout_type` | VARCHAR(32) | 布局类型 |
| `fetched_at` | DATETIME | 后端拉取时间 |
| `page` | INT | 所属页码 |
| `position` | INT | 页内排序位置 |

**`category_cache_meta` 表**（缓存元信息）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `category` | VARCHAR(16) | 联合主键 |
| `page` | INT | 联合主键 |
| `last_fetched_at` | DATETIME | 上次拉取时间 |
| `has_more` | BOOL | 是否有下一页 |
| `total_count` | INT | 该页条数（可空） |

#### 3.5.3 数据仓库 — `NewsRepository.kt`

**文件**：`backend/src/main/kotlin/com/demo/toutiao/repo/NewsRepository.kt`

封装所有数据库读写操作（使用 Exposed 的 `newSuspendedTransaction` 实现协程友好的事务）：

- `lastFetchedAt(category, page)` — 查询某分类某页的上次拉取时间（用于 TTL 判断）
- `loadPage(category, page, pageSize)` — 按 position 升序读取缓存数据
- `upsertPage(category, page, items, hasMore)` — 先删旧数据再插入新数据（保证页内顺序一致），同时更新 `category_cache_meta`

### 3.6 数据模型 — `Dto.kt`

**文件**：`backend/src/main/kotlin/com/demo/toutiao/model/Dto.kt`

定义后端 API 的统一响应格式：

```kotlin
ApiResponse<T>(code: Int, msg: String, data: T?)
```

**错误码常量** (`ErrorCode`)：

| 常量 | 值 | 含义 |
|------|----|------|
| `OK` | 0 | 成功 |
| `WHYTA_FAIL` | 1001 | whyta API 调用失败 |
| `BAD_PARAM` | 1002 | 请求参数错误 |
| `INTERNAL` | 1003 | 服务器内部错误 |

**`NewsListResponse`** — 新闻列表响应体：包含 `category`、`page`、`pageSize`、`hasMore`、`fromCache`（是否来自缓存）、`list`（新闻列表）

**`NewsDto`** — 单条新闻：`id`、`title`、`description`、`source`、`publishTime`、`imageUrl`、`originalUrl`、`layoutType`

### 3.7 配置文件

**`application.yaml`**：

```yaml
ktor:
  deployment:
    port: 8080           # 服务端口
    host: 0.0.0.0        # 监听地址

db:
  url: "jdbc:mysql://localhost:3306/toutiao_demo?..."  # MySQL 连接串
  user: "root"
  password: "123456"
  maxPoolSize: 8         # 连接池大小

whyta:
  baseUrl: "https://whyta.cn"
  apiKey: "7e379d7f3330"  # API 密钥
  timeoutMs: 10000        # 超时（毫秒）

cache:
  ttlMinutes: 10          # 缓存有效期（分钟）
```

**`logback.xml`**：使用 Logback 控制台输出，格式为 `时间 [线程] 级别 Logger - 消息`

---

## 四、Android 客户端模块详解

### 4.1 应用入口

#### 4.1.1 `ToutiaoApp.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/ToutiaoApp.kt`

继承 `Application`，添加 `@HiltAndroidApp` 注解，触发 Hilt 依赖注入框架的代码生成与初始化。

#### 4.1.2 `MainActivity.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/MainActivity.kt`

唯一的 Activity，使用 `@AndroidEntryPoint` 标注以启用 Hilt 注入。核心职责：

1. **设置 Compose UI**：`setContent { ToutiaoTheme { ... } }`
2. **配置 Navigation 导航图**：

| 路由 | 页面 | 说明 |
|------|------|------|
| `home` | `HomeScreen` | 首页（起始页） |
| `detail/{title}/{source}/{url}` | `DetailScreen` | 新闻详情页 |

3. **页面间传参**：使用 `URLEncoder` / `URLDecoder` 对标题、来源、URL 进行编解码，避免特殊字符破坏路由

### 4.2 网络层 (`data/api/`)

#### 4.2.1 `NewsApi.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/data/api/NewsApi.kt`

Retrofit 接口，定义与后端通信的 API：

```kotlin
@GET("/api/news")
suspend fun getNews(
    category: String,
    page: Int,
    pageSize: Int = 20,
    forceRefresh: Boolean = false,
): ApiResponse<NewsListData>
```

同时定义 `BackendException` 用于业务错误的异常抛出。

#### 4.2.2 `Dto.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/data/api/Dto.kt`

定义网络响应的数据结构（使用 `kotlinx.serialization` 序列化）：
- `ApiResponse<T>` — 统一响应包装（`code` + `msg` + `data`）
- `NewsListData` — 新闻列表数据（包含 `category`、`page`、`pageSize`、`hasMore`、`fromCache`、`list`）
- `NewsDto` — 单条新闻 DTO（`id`、`title`、`description`、`source`、`publishTime`、`imageUrl`、`originalUrl`、`layoutType`）

### 4.3 本地数据库层 (`data/db/`)

#### 4.3.1 `AppDatabase.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/data/db/AppDatabase.kt`

Room 数据库定义，包含两个实体表：
- `NewsEntity` — 新闻本地缓存
- `RemoteKeysEntity` — 分页游标（远程分页键）

数据库名：`toutiao.db`，使用 `fallbackToDestructiveMigration()` 策略处理版本升级。

#### 4.3.2 `NewsEntity.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/data/db/NewsEntity.kt`

**`NewsEntity`** — 新闻缓存实体：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` + `category` | String | 联合主键 |
| `title` | String | 标题 |
| `description` | String? | 摘要 |
| `source` | String? | 来源 |
| `imageUrl` | String? | 图片 URL |
| `originalUrl` | String? | 原文链接 |
| `publishTime` | String? | 发布时间 |
| `layoutType` | String | 布局类型 |
| `position` | Int | 排序位置 |
| `cachedAt` | Long | 缓存时间戳 |

提供两个扩展转换函数：
- `NewsDto.toEntity()` — 网络 DTO → 数据库实体
- `NewsEntity.toDomain()` — 数据库实体 → UI 领域模型

**`RemoteKeysEntity`** — 远程分页键：

| 字段 | 类型 | 说明 |
|------|------|------|
| `category` | String | 主键 |
| `nextPage` | Int? | 下一页页码（null 表示已到末页） |
| `lastUpdated` | Long | 上次更新时间 |

#### 4.3.3 `NewsDao.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/data/db/NewsDao.kt`

Room DAO 接口：

| 方法 | 功能 |
|------|------|
| `pagingSource(cat)` | 返回按 position 排序的 `PagingSource`，供 Paging3 使用 |
| `clearCategory(cat)` | 清空某分类的所有缓存 |
| `upsertAll(items)` | 批量插入/更新新闻 |

`RemoteKeysDao`：

| 方法 | 功能 |
|------|------|
| `upsert(key)` | 插入/更新分页键 |
| `get(cat)` | 查询某分类的当前分页键 |
| `delete(cat)` | 删除某分类的分页键 |

### 4.4 分页机制 (`data/paging/`)

#### `NewsRemoteMediator.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/data/paging/NewsRemoteMediator.kt`

实现 Paging3 的 `RemoteMediator`，是**网络 + 本地缓存**协同分页的核心组件：

```
load(loadType, state)
    │
    ├── REFRESH → page=1, forceRefresh=true
    │              清空本地缓存 → 拉取第一页 → 写入 Room
    │
    ├── PREPEND → 直接返回（不支持向前加载）
    │
    └── APPEND  → 从 RemoteKeysEntity 读取 nextPage
                  nextPage=null → 已到末页
                  否则 → 拉取下一页 → 追加写入 Room
```

**事务保证**：使用 `db.withTransaction { ... }` 确保 REFRESH 时"清除旧数据 + 写入新数据 + 更新游标"在同一事务内完成。

**异常处理**：捕获 `IOException`、`HttpException`、`BackendException` 并包装为 `MediatorResult.Error`。

### 4.5 数据仓库 (`data/repo/`)

#### `NewsRepository.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/data/repo/NewsRepository.kt`

使用 `@Singleton` 注入，构建 Paging3 的 `Pager`：

```kotlin
Pager(
    config = PagingConfig(pageSize=20, prefetchDistance=5),
    remoteMediator = NewsRemoteMediator(...),
    pagingSourceFactory = { db.newsDao().pagingSource(category) },
).flow.map { pagingData -> pagingData.map { it.toDomain() } }
```

- `PagingConfig`：每页 20 条，预取距离 5 条
- 数据源：Room 的 `PagingSource`（自动感知数据库变化）
- `RemoteMediator`：网络数据自动写入 Room，触发 PagingSource 刷新

### 4.6 领域模型 (`data/model/`)

#### `NewsItem.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/data/model/NewsItem.kt`

- `Categories` — 分类常量：`ALL = ["关注", "推荐", "热榜", "新时代"]`，默认 `"推荐"`
- `LayoutType` — 布局类型常量：`TEXT_ONLY`、`TEXT_WITH_THUMB`、`BIG_IMAGE`
- `NewsItem` — UI 层使用的领域模型（`id`、`category`、`title`、`description`、`source`、`imageUrl`、`originalUrl`、`publishTime`、`layoutType`）

### 4.7 依赖注入 (`di/`)

#### `AppModule.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/di/AppModule.kt`

Hilt Module（`@InstallIn(SingletonComponent::class)`），提供全局单例：

| 提供项 | 说明 |
|--------|------|
| `Json` | kotlinx.serialization 实例（`ignoreUnknownKeys`、`isLenient`） |
| `OkHttpClient` | HTTP 客户端，15s 超时，DEBUG 模式启用日志拦截器 |
| `Retrofit` | 基于 `BuildConfig.BACKEND_BASE_URL` 构建（模拟器默认 `10.0.2.2:8080`） |
| `NewsApi` | Retrofit 创建的接口实现 |
| `AppDatabase` | Room 数据库实例（`toutiao.db`） |

### 4.8 UI 层 (`ui/`)

#### 4.8.1 主题 — `Theme.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/ui/theme/Theme.kt`

定义今日头条风格的配色方案：

| 颜色常量 | 色值 | 用途 |
|----------|------|------|
| `ToutiaoRed` | `#FF2A2A` | 主色调（顶栏、选中态） |
| `Bg` | `#F5F5F5` | 页面背景 |
| `CardBg` | `#FFFFFF` | 卡片背景 |
| `TextPrimary` | `#1A1A1A` | 标题文字 |
| `TextSecondary` | `#8A8A8A` | 辅助文字 |
| `DividerColor` | `#EEEEEE` | 分割线 |

使用 Material3 `lightColorScheme` 构建主题。

#### 4.8.2 首页 — `HomeScreen.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/ui/home/HomeScreen.kt`

首页主界面，使用 `Scaffold` 布局：

- **顶部**：`HomeTopBar`（红色搜索栏）+ `CategoryTabRow`（分类标签页）
- **内容**：`HorizontalPager`（左右滑动切换分类）
  - 每页对应一个 `NewsList`，通过 `viewModel.pagingFlow(category).collectAsLazyPagingItems()` 获取分页数据
- **底部**：`BottomNavBar`（五个导航入口）

`HorizontalPager` 与 `CategoryTabRow` 联动：点击标签 → `animateScrollToPage`，滑动页面 → 自动高亮对应标签。

#### 4.8.3 顶部栏 — `HomeTopBar.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/ui/home/HomeTopBar.kt`

仿今日头条红色顶栏：
- 第一行：时间 + 天气（静态展示）
- 第二行：搜索框（圆角半透明背景 + 搜索图标 + 热搜文字）+ 登录按钮

#### 4.8.4 分类标签 — `CategoryTabRow.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/ui/home/CategoryTabRow.kt`

使用 `ScrollableTabRow` 实现可滚动的分类标签栏：
- 选中态：17sp 加粗白色
- 未选中态：15sp 正常白色（70% 透明度）
- 红色背景，无底部分割线

#### 4.8.5 底部导航 — `BottomNavBar.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/ui/home/BottomNavBar.kt`

五个底部导航项：首页、西瓜视频、放映厅、未登录、我的。当前仅"首页"高亮，其余为静态展示。

#### 4.8.6 新闻卡片 — `NewsCard.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/ui/home/NewsCard.kt`

根据 `layoutType` 渲染三种布局：

| 布局类型 | 样式 | 说明 |
|----------|------|------|
| `TEXT_ONLY` | 纯文字 | 标题（2 行）+ 来源/时间 |
| `TEXT_WITH_THUMB` | 左文右图 | 标题（3 行）+ 来源/时间 ｜ 右侧 108×76dp 缩略图 |
| `BIG_IMAGE` | 大图模式 | 标题（2 行）+ 全宽 190dp 大图 + 来源/时间 |

- 图片加载：使用 **Coil** 的 `AsyncImage`，`ContentScale.Crop` 裁切填充
- 点击事件：`onNewsClick(title, source, url)` → 导航到详情页
- 底部分割线：`HorizontalDivider`（0.5dp）

#### 4.8.7 新闻列表 — `NewsList.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/ui/home/NewsList.kt`

核心列表组件，集成 Paging3 + 下拉刷新：

- **下拉刷新**：`PullToRefreshContainer` + `rememberPullToRefreshState()`
  - 下拉触发 → `items.refresh()` → 重新拉取第一页
  - 加载完成 → `endRefresh()` 收起刷新指示器
- **状态处理**：
  - 首次加载中 → `FullScreenLoading`（居中转圈）
  - 加载失败且无数据 → `FullScreenError`（错误提示 + 重试按钮）
  - 无数据 → `FullScreenEmpty`（"暂无内容"）
  - 正常 → `LazyColumn` 渲染 `NewsCard` 列表
- **底部加载更多**：`AppendFooter` 根据 `append` 状态显示加载转圈 / 重试 / "没有更多了"

#### 4.8.8 新闻详情 — `DetailScreen.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/ui/detail/DetailScreen.kt`

基于 WebView 的新闻全文阅读页面：

- **顶栏**（`TopAppBar`）：
  - 返回按钮 → `navController.popBackStack()`
  - 标题显示新闻来源名称
  - 加载中时显示"加载中..."副标题
  - 分享 / 更多操作按钮（预留）
- **进度条**：`LinearProgressIndicator`，跟踪 WebView 加载进度（0~100%），加载完成后淡出隐藏
- **WebView 配置**：
  - 启用 JavaScript + DOM Storage
  - 启用 `loadWithOverviewMode` + `useWideViewPort` 自适应页面宽度
  - 禁用缩放控件
- **广告过滤**：页面加载完成后注入 CSS 规则，隐藏常见的下载引导、广告容器、底部栏等干扰元素

#### 4.8.9 ViewModel — `HomeViewModel.kt`

**文件**：`android/app/src/main/java/com/demo/toutiao/ui/home/HomeViewModel.kt`

使用 `@HiltViewModel` 注入，职责：

- 持有分类列表 `categories = Categories.ALL`
- 为每个分类懒创建并缓存 `PagingFlow`（`cachedIn(viewModelScope)`），避免重复创建和配置变更时丢失分页状态

### 4.9 构建配置

#### `build.gradle.kts`（Android App）

**文件**：`android/app/build.gradle.kts`

- **compileSdk / targetSdk**：34（Android 14）
- **minSdk**：24（Android 7.0）
- **后端地址**：优先从 `local.properties` 读取 `backend.url`，默认 `http://10.0.2.2:8080`（模拟器映射宿主机 localhost）
- **主要依赖**：

| 类别 | 库 |
|------|----|
| Compose | BOM 2024.06.00 · Material3 · Foundation · Icons Extended |
| DI | Hilt 2.51.1 + KSP |
| 网络 | Retrofit 2.11 · OkHttp 4.12 · kotlinx-serialization |
| 数据库 | Room 2.6.1 (runtime + ktx + paging + compiler) |
| 分页 | Paging3 3.3.0 (runtime + compose) |
| 图片 | Coil 2.6.0 |
| 导航 | Navigation Compose 2.7.7 |

#### `build.gradle.kts`（Backend）

**文件**：`backend/build.gradle.kts`

| 类别 | 库 | 版本 |
|------|----|------|
| 服务端 | Ktor (Netty) | 2.3.12 |
| HTTP 客户端 | Ktor Client (CIO) | 2.3.12 |
| ORM | Exposed (core + dao + jdbc + java-time) | 0.50.1 |
| 连接池 | HikariCP | 5.1.0 |
| 数据库驱动 | MySQL Connector/J | 8.4.0 |
| 日志 | Logback | 1.5.6 |
| 序列化 | kotlinx-serialization | 1.6.3 |

---

## 五、数据流全链路

```
用户滑动/刷新
    ↓
HorizontalPager → NewsList → LazyPagingItems
    ↓
Paging3 触发 RemoteMediator.load()
    ↓
Retrofit 调用后端 GET /api/news?category=推荐&page=1
    ↓
后端 NewsRoute 参数校验 → NewsService.loadNews()
    ↓
检查 MySQL 缓存是否过期（TTL 10 分钟）
    ├── 未过期 → 直接返回 MySQL 数据
    └── 已过期 → 调用 WhytaClient 拉取 whyta.cn API
                  ↓
              解析 JSON → 写入 MySQL（upsertPage）
                  ↓
              返回 NewsListResponse 给 Android
    ↓
Android RemoteMediator 写入 Room 数据库
    ↓
Room PagingSource 自动通知 → LazyColumn 更新 UI
    ↓
用户点击新闻卡片 → Navigation 跳转 DetailScreen
    ↓
WebView 加载原文 URL + 注入 CSS 隐藏广告
```

---

## 六、关键技术亮点

1. **双层缓存架构**：后端 MySQL 缓存（TTL 过期策略）+ 客户端 Room 离线缓存，保证弱网下仍可阅读
2. **Paging3 + RemoteMediator**：实现"网络 → 本地数据库 → UI"的标准三层分页架构，支持无限滚动和下拉刷新
3. **多源聚合**：推荐频道并发请求 4 个 API 端点，合并去重打乱，提升内容多样性
4. **优雅降级**：后端获取失败时降级为过期缓存；客户端通过 Room 保证离线可用
5. **WebView 阅读优化**：注入 JS/CSS 隐藏广告和下载引导，提供干净的阅读体验
6. **MVVM + 依赖注入**：ViewModel + Hilt + Repository 模式，职责清晰、易于测试
7. **统一错误处理**：后端 StatusPages 全局异常拦截 + Android 端 LoadState 错误提示 + 重试机制
8. **协程并发**：后端使用 `coroutineScope + async` 并发拉取多源数据，前端使用 Flow 响应式更新

---

## 七、模块间依赖关系图

```
┌───────────────────────────── Android 客户端 ─────────────────────────────┐
│                                                                          │
│   ┌─────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────┐   │
│   │   UI    │ ←─ │  ViewModel   │ ←─ │  Repository  │ ←─ │  NewsApi │   │
│   │(Compose)│    │ (Hilt注入)    │    │  (Pager)     │    │(Retrofit)│   │
│   └─────────┘    └──────────────┘    └──────┬───────┘    └────┬─────┘   │
│        │                                     │                 │         │
│        │                              ┌──────┴───────┐         │         │
│        │                              │RemoteMediator│─────────┘         │
│        │                              └──────┬───────┘                   │
│        │                                     │                           │
│        │                              ┌──────┴───────┐                   │
│        └─────────────────────────────▸│  Room (SQLite)│                   │
│                                       └──────────────┘                   │
└──────────────────────────────────────────────────────────────────────────┘
                                    │ HTTP
                                    ▼
┌───────────────────────────── Ktor 后端服务 ──────────────────────────────┐
│                                                                          │
│   ┌──────────┐    ┌─────────────┐    ┌──────────────┐    ┌───────────┐  │
│   │  Route   │ →─ │ NewsService │ →─ │  Repository  │ →─ │   MySQL   │  │
│   │(路由层)   │    │ (业务逻辑)   │    │ (Exposed)    │    │ (HikariCP)│  │
│   └──────────┘    └──────┬──────┘    └──────────────┘    └───────────┘  │
│                          │                                               │
│                   ┌──────┴──────┐                                        │
│                   │ WhytaClient │ ──── HTTP ────▸ whyta.cn API           │
│                   └─────────────┘                                        │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 八、运行与部署

### 8.1 环境要求

- JDK 17+
- MySQL 8.x（本地端口 3306）
- Android Studio Hedgehog 2023.1.1+（含 Android SDK 34）
- whyta.cn API Key

### 8.2 启动步骤

1. **创建数据库**：执行 `backend/sql/init.sql` 建库建表
2. **配置 API Key**：修改 `backend/src/main/resources/application.yaml` 中的 `whyta.apiKey`
3. **启动后端**：在 `backend/` 目录执行 `./gradlew run`
4. **启动 Android**：Android Studio 打开 `android/` 目录，运行到模拟器/真机
