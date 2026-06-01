# Headline News

Headline News 是一套面向移动端交付的智能资讯应用源码，包含 Android 原生客户端和 Python AI 服务端。客户端负责新闻聚合、分页加载、缓存、原生正文阅读和 AI 交互体验；服务端负责文章解析、AI 摘要、AI 速读、对话问答、结果缓存和可选联网搜索。

本仓库已按交付形态整理：公开仓库只保留可运行源代码、构建脚本、服务端配置模板和交付说明，不包含 IDE 配置、构建产物、本地日志、测试样例、AI 辅助工具目录或本机密钥。

## 功能概览

| 模块 | 能力 |
| --- | --- |
| 新闻聚合 | 接入 `v2ex`、`thepaper`、`zhihu`、`geekpark`、`tieba` 等资讯源，并在 4 个 Tab 中混合展示 |
| 首页体验 | 启动预加载、Room 本地缓存、下拉刷新、分页加载、15 条首屏加载、加载更多状态提示 |
| 原生详情 | 根据新闻 URL 抓取标题、来源、发布时间、正文段落和图片，尽量在 App 内原生排版展示 |
| AI 速读 | 根据当前新闻池生成精选新闻和推荐理由，支持关闭、展开、收起和点击进入原新闻 |
| AI 助手 | 针对当前新闻进行上下文问答，支持对话历史、建议问题、文章来源展示和服务端缓存 |
| 服务端能力 | FastAPI + httpx + BeautifulSoup + LangGraph，可配置 DeepSeek/OpenAI 兼容接口和可选联网搜索 |

## 技术栈

| 端 | 技术 |
| --- | --- |
| Android | Kotlin、Jetpack Compose、Material 3、Hilt、Room、Paging 3、Retrofit、OkHttp、Coil、Jsoup |
| Server | Python 3.11+、FastAPI、Uvicorn、httpx、Pydantic、BeautifulSoup、LangGraph、SQLite Cache |
| 数据源 | `https://apiserver.alcex.cn/daily-hot/{platform}` |
| AI 接口 | OpenAI-compatible Chat Completions，默认按 DeepSeek 配置 |

## 仓库结构

```text
Headline-News-List-App/
├── android/                  # Android 原生客户端
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/headline/news/
│   │       │   ├── data/     # API、Room、Repository、Paging、正文解析
│   │       │   ├── di/       # Hilt 依赖注入
│   │       │   ├── ui/       # 首页、详情、AI、启动页、主题
│   │       │   ├── MainActivity.kt
│   │       │   └── HeadlineNewsApp.kt
│   │       └── res/
│   ├── gradle/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── server/                   # AI 与文章解析服务
│   ├── app/
│   │   ├── __init__.py
│   │   └── main.py
│   ├── .env.example
│   └── requirements.txt
├── .gitignore
└── README.md
```

## Android 客户端

### 核心入口

| 文件 | 说明 |
| --- | --- |
| `android/app/src/main/java/com/headline/news/MainActivity.kt` | App 启动、启动动画、导航容器 |
| `android/app/src/main/java/com/headline/news/ui/home/HomeScreen.kt` | 首页 Tab、新闻列表、AI 速读卡片 |
| `android/app/src/main/java/com/headline/news/ui/home/HomeViewModel.kt` | 首页状态管理、预加载、刷新、AI 速读请求 |
| `android/app/src/main/java/com/headline/news/data/repo/NewsRepository.kt` | 新闻源聚合、本地缓存、分页数据流 |
| `android/app/src/main/java/com/headline/news/data/paging/NewsRemoteMediator.kt` | Paging 远端加载和 Room 写入 |
| `android/app/src/main/java/com/headline/news/ui/detail/DetailScreen.kt` | 原生新闻详情、AI 摘要、AI 对话 |
| `android/app/src/main/java/com/headline/news/data/repo/AiRepository.kt` | 客户端 AI API 封装 |

### 本地配置

客户端默认使用公开服务端地址，也可以通过本地配置覆盖。新建 `android/ai.local.properties`：

```properties
ai.baseUrl=https://your-api-domain.com/
ai.appToken=your-app-token
```

注意：`android/ai.local.properties` 已加入 `.gitignore`，不要提交真实 Token。

### Android 构建

```powershell
cd android
.\gradlew.bat assembleDebug
```

项目已配置阿里云 Maven 镜像，国内网络下同步依赖会更稳定。正式给用户分发前建议：

- 将 `AI_BASE_URL` 切换为 HTTPS 域名，不建议长期使用裸 IP + HTTP。
- 使用正式签名文件生成 Release 包。
- 根据隐私策略决定是否继续关闭备份，当前 `allowBackup=false` 更适合商业分发。
- 后端服务端口、防火墙、安全组、域名解析和 HTTPS 证书全部配置完成后再交付安装包。

## AI 服务端

### 环境准备

```powershell
cd server
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
```

Linux 服务器示例：

```bash
cd server
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 服务端配置

复制配置模板：

```bash
cp .env.example .env
```

核心变量：

```env
AI_PROVIDER=deepseek
AI_BASE_URL=https://api.deepseek.com
AI_API_KEY=your-api-key
AI_MODEL=deepseek-chat
AI_TIMEOUT_SECONDS=45
AI_MAX_TOKENS=900
AI_CHAT_MAX_TOKENS=520
AI_THINKING=disabled
AI_CACHE_TTL_SECONDS=86400
AI_WEB_SEARCH_ENABLED=false
AI_SEARCH_MAX_RESULTS=3
AI_ARTICLE_FETCH_TIMEOUT_SECONDS=5
TAVILY_API_KEY=
```

说明：

- `AI_API_KEY` 必须只放在服务端 `.env`，不能写进 Android 源码。
- `AI_APP_TOKEN` 可选；配置后客户端请求必须携带 `X-App-Token`，适合公开部署时做基础访问控制。
- `AI_WEB_SEARCH_ENABLED=true` 后，服务端会在适合的问题上补充联网搜索；未配置 `TAVILY_API_KEY` 时会尝试 DuckDuckGo 兜底。
- 服务端使用 SQLite 文件缓存，默认路径是 `server/data/cache.sqlite3`，该目录不会提交到 Git。

### 启动服务

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

健康检查：

```bash
curl http://127.0.0.1:8000/health
```

生产部署建议使用 Nginx/Caddy 反向代理到 HTTPS 域名，并用 systemd、Supervisor 或容器守护进程托管 Uvicorn。

## API 摘要

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/health` | 服务健康检查 |
| `POST` | `/api/article/extract` | 根据新闻 URL 提取原生正文内容 |
| `POST` | `/api/ai/summary` | 单篇新闻摘要 |
| `POST` | `/api/ai/daily-brief` | 今日 AI 速读 |
| `POST` | `/api/ai/news-rank` | 新闻价值排序 |
| `POST` | `/api/ai/chat` | 单轮新闻问答 |
| `POST` | `/api/ai/chat/message` | 多轮新闻对话 |

## 交付注意事项

- 新闻源来自第三方公开接口，生产可用性取决于第三方接口稳定性、用户网络和服务端网络出口。
- 原生正文解析依赖目标网页结构，已做通用正文抽取和失败兜底，但部分站点可能因反爬、登录墙或页面结构变化无法完整解析。
- AI 能力依赖服务端模型供应商，建议在正式上线前配置限流、日志、异常告警和 HTTPS。
- 当前仓库只提交源码和交付说明，真实 `.env`、签名证书、构建产物、IDE 配置、本地缓存都不应进入 GitHub。

## 商业化前检查清单

- 配置正式域名和 HTTPS。
- 服务端 `.env` 填入真实 AI Key，并确认 Key 未进入 Git 历史。
- Android `ai.local.properties` 指向正式 API 地址。
- 使用 Release 签名重新打包。
- 在真实手机和不同网络环境下验证首页加载、下拉刷新、加载更多、原生详情、AI 速读和 AI 对话。
- 为生产服务增加访问日志、错误日志、基础监控和备份策略。
