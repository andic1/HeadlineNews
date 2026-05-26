# 今日头条首页 Demo

仿今日头条首页列表的端到端 Demo：

- **客户端**：Android（Kotlin + Compose + Hilt + Room + Paging3 + Coroutines + Coil）
- **后端**：Ktor + Exposed + HikariCP + MySQL（utf8mb4）
- **数据源**：[whyta.cn](https://apis.whyta.cn/) 新闻接口
- **设计文档**：`docs/superpowers/specs/2026-05-25-toutiao-demo-design.md`

## 目录结构

```
D:\toutiao-demo\
├── backend\          Ktor 后端
├── android\          安卓 App
├── docs\             设计文档
└── README.md
```

## 一、环境准备（Windows 本机）

| 工具 | 版本 | 安装 |
|---|---|---|
| JDK 17 | 17+（必须） | Eclipse Temurin / Oracle / Microsoft |
| Android Studio | Hedgehog 2023.1.1+ | 自带 Android SDK 34 + JDK 17 |
| MySQL Server | 8.x | 官网 .msi 安装包，本地端口 3306 |
| Gradle | 不用手装 | 用项目内的 wrapper（Android Studio / Ktor plugin 自动下载） |

> **重要**：后端用 JDK 17。请把环境变量 `JAVA_HOME` 指到 JDK 17，并确保 `java -version` 显示 17。

## 二、初始化数据库

1. 启动本机 MySQL（默认端口 3306）
2. 用 MySQL Workbench / 命令行执行：

   ```bat
   mysql -u root -p < D:\toutiao-demo\backend\sql\init.sql
   ```

   会创建 `toutiao_demo` 库 + `news` / `category_cache_meta` 两张表。

3. 如果你的 MySQL root 密码不是 `root`，改 `backend\src\main\resources\application.yaml` 的 `db.password`。

## 三、领取 whyta APIKEY

1. 打开 https://apis.whyta.cn/
2. 任意接口文档底部有"扫码关注公众号获取 APIKEY"
3. 拿到 key 后填到 `backend\src\main\resources\application.yaml`：

   ```yaml
   whyta:
     apiKey: "你的真实 key"
   ```

## 四、跑后端

在 `backend/` 目录下：

```bat
cd D:\toutiao-demo\backend
gradlew.bat run
```

> 第一次会下载 gradle 和依赖，需要几分钟。
>
> 如果项目还没有 gradle wrapper：在 backend 目录运行 `gradle wrapper --gradle-version 8.7` 一次（需要本机装过 gradle 8.x），之后用 `gradlew.bat` 即可。或者用 Android Studio 打开 backend 文件夹，IDE 会自动补 wrapper。

启动成功后访问：

- http://localhost:8080/health → `ok`
- http://localhost:8080/api/news?category=推荐 → 返回新闻 JSON

## 五、跑安卓 App

1. Android Studio 打开 `D:\toutiao-demo\android\`
2. 等 Gradle Sync 完成（自动下载依赖）
3. 启动一个模拟器（建议 API 34 / Pixel 7）
4. 点 Run

模拟器通过 `10.0.2.2:8080` 访问 Windows 宿主机的后端（已在 `app/build.gradle.kts` 的 `BACKEND_BASE_URL` 配好）。

### 如果你要在真机上跑

需要把 `BACKEND_BASE_URL` 改成手机能访问的地址：

1. Windows 防火墙允许 8080 端口入站
2. 查 Windows 局域网 IP（如 `192.168.1.10`）
3. 改 `android/app/build.gradle.kts`：
   ```kotlin
   buildConfigField("String", "BACKEND_BASE_URL", "\"http://192.168.1.10:8080\"")
   ```
4. 手机和电脑接同一个 Wi-Fi

## 六、功能对照

| 设计需求 | 实现位置 |
|---|---|
| 数据加载 | `NewsRemoteMediator` + `NewsRepository`（端到端） |
| 卡片展示（三种样式） | `ui/home/NewsCard.kt`（TEXT_ONLY / TEXT_WITH_THUMB / BIG_IMAGE） |
| 加载状态控制 | `ui/home/NewsList.kt`（loading / error / empty / footer） |
| 下拉刷新 | `PullToRefreshContainer` |
| 加载更多 | Paging3 `LoadType.APPEND` |
| 数据库存储 | 后端 MySQL（Exposed）+ 客户端 Room |
| 多 Tab | `HorizontalPager` + 每 Tab 独立 PagingFlow |
| 顶栏 / 底栏 | `HomeTopBar` / `BottomNavBar`（静态壳） |

## 七、已知限制 / 范围外

- "小说" / "视频" Tab 暂时显示空（whyta 没对应接口）
- "新时代" Tab 走 `tx/topnews`，**只有 2019-2022 历史数据**
- 没有详情页，点卡片直接用系统浏览器打开 `originalUrl`
- 没做收藏 / 登录 / 点赞 / 分享
- Compose UI 测试没写

## 八、常见问题

**Q: 后端起不来，连不上 MySQL？**
A: 检查 `application.yaml` 的 `db.url` / `db.user` / `db.password`，确认本机 mysql 服务在跑。

**Q: 安卓显示"加载失败"？**
A: 先开浏览器访问 `http://10.0.2.2:8080/health`（在模拟器里）确认后端可达。Windows 防火墙可能拦了。

**Q: whyta 返回 `code != 200`？**
A: 大概率是 APIKEY 错或没填，或者超配额。日志会打 `whyta business err: ...`。
