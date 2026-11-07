# 新帆 · 启航｜完全原生 Android APP（GitHub 构建版）

**Jetpack Compose（Kotlin）纯原生实现，工程里没有任何 WebView、没有 HTML 加载。**
UI 按 `index.html` 的布局与配色 1:1 重画，功能对齐原项目，APP 内不写任何 token / API Key，
后端统一走 `https://ai.ffurry.cc.cd`，用 GitHub Actions 自动出 APK。

---

## 一、三步拿到 APK

1. 把本目录全部文件推到 GitHub 仓库（默认分支 `main` / `master`）。
2. 打开 **Actions → 构建原生 Android APK → Run workflow**（push 也会自动触发）。
3. 构建完在 **Artifacts** 下载 `app-debug.apk`；打 `v1.0` 这类 Tag 自动发到 Release。

Actions 自动装 JDK 17、Android SDK 34、Gradle 8.7，本地不用装 Android 环境。

技术栈：AGP 8.5.2 / Kotlin 2.0.21 / Compose BOM 2024.10.00 / OkHttp 4.12.0 / minSdk 23。

---

## 二、布局与配色对照

全部取自 `index.html` 的 `:root` 与 CSS，常量集中在 `Theme.kt`：

| 项目 | HTML | 原生 |
| --- | --- | --- |
| 主色 | `#007aff` | `Primary` |
| 页面背景 | `#f5f7fa` | `BgColor` |
| 正文 | `#333` | `TextMain` |
| 边框 | `#e1e4e8` | `Border` |
| 侧栏宽 | 280px | 280dp |
| 头部高 | 60px | 60dp |
| 用户气泡 | 蓝色、右下角 4px、最宽 85% | `UserShape`、最宽屏宽 85% |
| AI 气泡 | 白卡片、左下角 4px、边框 | `AssistantShape` |
| 输入区 | 白底、顶部细线、12px 内边距、圆角 12 | `Composer` |
| 宽屏 | ≥768px 侧栏常驻并隐藏头部 | `BoxWithConstraints` 同样判断 768dp |

页面结构：抽屉（模式切换 / 导航 / 历史 / 用户信息）+ 头部（☰、标题、+）+ 消息区 + 底部输入区，
以及通知中心、积分明细、个人中心、登录、注册、待审核 6 个页面，全部按原 HTML 排布。

---

## 三、功能对照

| 功能 | 实现 |
| --- | --- |
| 密码 / 邮箱验证码登录、邮箱检查、60s 冷却 | ✅ `AppViewModel` |
| 注册（校验、邀请码、注册开关）、待审核、清除状态 | ✅ |
| HttpOnly Cookie 登录态 | ✅ 自定义 `CookieJar`，只存 `auth_token`，APP 无 token |
| 聊天 / 代码双模式，模式持久化 | ✅ SharedPreferences |
| 模型列表、倍率、禁用态、日限提示、选择记忆 | ✅ `/api/models` |
| 思考模式 / 联网搜索开关 | ✅ 本地持久化并随请求发送 |
| SSE 流式输出（status / chat_id / reasoning / delta / billing / error / build_start / project_files / image_generated） | ✅ OkHttp 手动解析 event-stream，100ms 节流渲染 |
| 停止生成、错误重试 | ✅ `Call.cancel()` + 重试按钮 |
| Markdown（标题、粗体、行内代码、代码块、列表、引用、链接、图片） | ✅ 自绘 `MarkdownView`，无第三方 Web 渲染 |
| 图片多选上传（≤5 张、≤5MB、png/jpg）、预览、删除 | ✅ `GetMultipleContents` + multipart |
| 历史会话、长按删除、打开历史、构建卡片回显 | ✅ |
| 云端构建轮询、Jar 下载、源码 ZIP 打包下载 | ✅ MediaStore 写入「下载」目录 |
| 通知中心（未读红点、已读回写）、系统公告弹窗（24h 去重） | ✅ |
| 积分明细（近 30 天）、个人中心改用户名 / 密码 / 邮箱 / 邀请码 / 兑换码 / 退出 | ✅ |
| 维护模式、注册开关 | ✅ `/api/settings` |

---

## 四、不写 token 是怎么做到的

- APP 里只有 `BASE = "https://ai.ffurry.cc.cd"`，**没有任何 `sk-` / `cfut_` / 管理员口令**。
- 登录成功后 Worker 下发 `Set-Cookie: auth_token=...; HttpOnly; SameSite=Strict`；
  `Api.kt` 的 `CookieJar` 把它存进 SharedPreferences，后续所有 `/api/*` 请求自动带上。
- 真正的模型密钥只存在于 Worker 的 `AI_CONFIGS`，永远不下载到手机。

> ⚠️ 原 `worker.js` 里是**明文模型 Token**。本仓库不含 worker.js；
> 若要公开仓库，请先把 `token: 'sk-...'` 改成 `env.MODEL_XXX_TOKEN` 并用
> `wrangler secret put` 配置，否则会泄露。

---

## 五、目录结构

```
.
├── .github/workflows/build-apk.yml   # GitHub Actions 构建
├── build.gradle / settings.gradle / gradle.properties
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/cc/cd/ffurry/ai/xinfan/
│           ├── MainActivity.kt     # 入口 + 全局 Toast / 弹窗
│           ├── Api.kt              # OkHttp、CookieJar、SSE Call、上传/下载/打包
│           ├── AppViewModel.kt     # 全部状态与业务逻辑
│           ├── Theme.kt            # 配色常量
│           ├── Components.kt       # 输入框、按钮、头像、Toast、弹窗、网络图片
│           ├── Markdown.kt         # 原生 Markdown 渲染
│           ├── ChatScreen.kt       # 消息气泡 / 流式 / 模型面板 / 输入区
│           ├── MainScreen.kt       # 抽屉、头部、页面切换、宽屏布局
│           └── Screens.kt          # 登录/注册/审核/通知/积分/个人中心
└── README.md
```

---

## 六、与 HTML 有意保留的细小差异

按「细小变化可忽略」处理，不影响主布局和功能：

1. Markdown 是自绘的近似渲染（marked.js 的完整语法、表格、嵌套列表未 100% 复刻）。
2. 软键盘用「发送」键触发发送；网页端是 Enter 发送 / Shift+Enter 换行（手机无法稳定区分 Shift）。
3. 模型折叠面板从输入区上方展开，而不是绝对定位浮在聊天区上（视觉接近）。
4. 加载动画用系统 `CircularProgressIndicator` 代替 CSS 旋转边框。
5. 构建中的进度条是静态条，没有 CSS 位移动画。
6. 错误 / 计费 / 构建卡片放在 AI 气泡内部，网页端是气泡内并列或独立气泡。
7. 历史图片、生成图用原生 `Image` + OkHttp 加载（自动带 Cookie），不是 `<img>`。

---

## 七、改配置看这里

- 后端地址：`Api.kt` 顶部 `BASE` 与 `HOST`（两处一起改）。
- 签名：产物是 debug 签名自用包；上架前在 `app/build.gradle` 换成自己的 release 签名并开混淆。
- 本地调试：Android Studio（Ladybug 及以上）打开本目录，JDK 17 + Gradle 8.7，Sync 后 Run。
  或 `gradle wrapper --gradle-version 8.7 && ./gradlew assembleDebug`。
- 横竖屏不重建 Activity，聊天上下文不丢；返回键是「退出 APP」（原生没有网页历史栈）。
