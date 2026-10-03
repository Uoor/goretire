# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

校友安居 (Alumni Housing) — 阿里/蚂蚁校友可信租房网络，钉钉内 H5 微应用。

- **Backend**: Spring Boot 3 (Java 17) + MySQL 8 + MyBatis-Plus，Maven 多模块
- **Frontend**: Vue 3 + Vant 4 + Vite + Pinia，钉钉 H5（hash 路由，postcss-px-to-viewport 按 375 设计稿转 vw），代码在 `frontend/`，线上挂在 `/ali/house/`
- **Portal**: React 19 + TS + Ant Design 5 + Webpack 5，对外门户（代码在 `root-site/`），线上服务 `/` 与 `/ali`；与 H5 由 `scripts/build-web.mjs` 汇总成同一个产物树、一次发布

## Commands

### Backend（在 `backend/` 下执行）

```bash
mvn package -DskipTests                         # 构建，产出 app/target/aliren-app-0.1.0-SNAPSHOT-exec.jar
mvn test                                        # 跑全部测试（集中在 test 模块，JUnit 5 + Mockito + AssertJ，H2 内存库）
mvn -pl test -am test -Dtest=<ClassName> -Dsurefire.failIfNoSpecifiedTests=false   # 单测，如 -Dtest=JwtUtilTest
java -jar app/target/aliren-app-0.1.0-SNAPSHOT-exec.jar --server.port=8081         # 启动（8080 被占时用 8081）
```

`-am` 会连带构建依赖模块（core/houserent/app），它们自身无测试类，所以 `-Dsurefire.failIfNoSpecifiedTests=false` 避免报“No tests matching pattern”。

### Frontend（在 `frontend/` 下执行）

```bash
npm install
npm run dev        # 开发服务器 5173，/api 与 /uploads 代理到 http://localhost:8081
npm run build      # 构建
npm test           # vitest run（全量）
npx vitest run src/utils/format.test.js   # 单测
```

### 数据库

```bash
mysql -h 127.0.0.1 -u root -p < backend/app/src/main/resources/db/schema.sql
mysql -h 127.0.0.1 -u root -p aliren < backend/app/src/main/resources/db/data-mysql.sql
```

密码见 `backend/app/src/main/resources/application.yml`（`spring.datasource.password`，默认 `Test!123`）。建表脚本无 Flyway，直接手改 `schema.sql`；`schema-h2.sql` / `data-h2.sql` 供 H2 联调。

## Architecture

### Backend 多模块

```
backend/
├── core/        # 基础设施：common(ApiResponse/BusinessException/GlobalExceptionHandler)、auth、user、config、interceptor、upload
├── houserent/   # 租房业务：house/demand/subscribe/match/robot/report/auditlog/pushlog/schedule
├── app/         # 唯一可执行入口：AlirenApplication + application.yml + db/*.sql
└── test/        # 集中全部测试（依赖 app，间接拿到 core + houserent）
```

- **依赖方向单向**：`core ← houserent ← app ← test`。新增业务模块在父 pom `<modules>` 与 `dependencyManagement` 各登记一行，并在 app 的 `pom.xml` 追加依赖。
- **包名按模块**：`com.aliren.core.*`、`com.aliren.houserent.*`、`com.aliren.app.*`。
- **启动类** [AlirenApplication.java](backend/app/src/main/java/com/aliren/app/AlirenApplication.java) 用 `@SpringBootApplication(scanBasePackages = "com.aliren")` 跨模块扫描组件，`@MapperScan(basePackages = "com.aliren", annotationClass = Mapper.class)` 跨模块扫 Mapper（按 `@Mapper` 注解过滤，避免误注册 `DingTalkClient` 这类普通接口）。
- **业务包结构**：每个领域是自包含包，如 `house/` 含 `House` + `HouseMapper` + `HouseService` + `HouseController` + `dto/*`。

### 外部服务的 Stub/抽象模式（贯穿全项目）

每个外部依赖（钉钉、LLM）都抽象为接口 + 两个实现，靠 `@ConditionalOnMissingBean` 让“桩”成为缺省 Bean，真实实现配了凭证才启用。无凭证环境下全链路可跑通：

- `PushClient` → `DingTalkPushClient`（`aliren.robot.enabled=true`）/ `PushClientStub`（日志模拟发送）
- `MatchClient` → `QwenMatchClient`（配 `aliren.llm.api-key`）/ `MatchClientStub`（返回 null 触发 `MatchService` 本地过滤降级）
- `DingTalkClient`（core 内）→ `DingTalkClientImpl` / `DingTalkClientStub`

新增外部能力时沿用此模式，而不是在 Service 里 `if (enabled)` 分支。

### 认证与配置

- **JWT 免登**：`/api/auth` 收钉钉 authCode → 换 userid → 查/建 User → 发 JWT。浏览器联调时前端 `getAuthCode()` 返回 `dev-code` 桩，后端 stub 放行；`dev-code` 种子用户是管理员。
- **拦截器**（[WebConfig.java](backend/core/src/main/java/com/aliren/core/config/WebConfig.java)）：`AuthInterceptor` 校验 `Authorization: Bearer <token>` 并写 `UserContext`（ThreadLocal），只做认证不做角色校验；`RateLimitInterceptor` 在鉴权之后。放行路径：`/api/auth`、`/api/dingtalk/jsapi-sign`、`/error`。管理员校验在业务层/专用接口。
- **配置**：全部走 `aliren.*` 前缀 + `${ENV_VAR:default}` 覆盖，见 [application.yml](backend/app/src/main/resources/application.yml)。图片上传存本地 `aliren.upload.dir`（`/uploads/**` 静态映射）。

### Frontend 壳 + 模块

```
frontend/src/
├── router|store|utils|api     # 壳层：hash 路由、Pinia 用户态、axios 封装、钉钉 JSAPI 封装(dd.js)
├── styles/                    # 设计令牌 tokens.css（DESIGN.md 的来源）
└── modules/houserent/         # 租房业务：views/ + components/ + api.js + data/
```

- **统一响应解包**：后端返回 `{code, msg, data}`（`code === 0` 成功）。[request.js](frontend/src/utils/request.js) 拦截器自动注入 `Bearer token` 并解包成 `data`，业务 API 直接拿数据。
- **路由壳**：`router/index.js` 懒加载 `modules/<module>/views/`，`meta.tab = true` 的页面显示底部 TabBar；`beforeEach` 里做免登（`ensureLogin`）+ 管理后台角色校验（`meta.admin`）。
- **钉钉环境判断**：必须用 `dd.js` 的 `isDingTalk()`（`dd.env.platform !== 'notInDingTalk'`），不要用 `typeof dd === 'undefined'`——官方 SDK 在普通浏览器也会注入 `dd`。
- **前端环境变量**：`frontend/.env`（已 gitignore）放 `VITE_DING_CORP_ID`；模板见 `frontend/.env.example`。
- **视觉规范**：新增/改 UI 前读 [frontend/DESIGN.md](frontend/DESIGN.md)（暖白 `#F5F5F4` + 活力橙 `#FF6A00`，数字用 `--num`/DM Sans，橙色只用于行动/价格/信任信号）。设计令牌在 `styles/tokens.css`。

### 前端样式规范（CSS 架构）

全局样式分三层，**禁止在页面 `<style scoped>` 里重复定义公共组件**：

```
frontend/src/styles/
├── tokens.css      # 设计令牌：--primary/--bg/--card/--fg/--border/--radius-* 等（唯一色值来源）
├── base.css        # 重置 + 通用布局 + 钉钉登录按钮等全局元素
└── components.css  # 公共组件：btn-primary/btn-ghost/op-btn/del-btn/icon-btn/
                    #   form-field/seg-item/qc-chip/tag/avatar/prompt-hero/prompt-input/
                    #   match-card/guide-strip/cond-strip/qh-row
```

- **色值只写 `var(--token)`**，不写死 `#hex`（渐变占位图除外，见 DESIGN.md §2）。
- **公共组件**（按钮/表单/标签/卡片/提示条）定义在 `components.css`，页面只保留**特有布局**与 **PC 端 `@media (min-width:768px)` 覆盖**（如主按钮限宽 320px 居中）。
- **按钮统一**：主按钮 `.btn-primary`（padding `10px 16px`、圆角 `--radius-md`、立体边 `0 2px 0 --primary-deep`）；次按钮 `.btn-ghost`；行内操作 `.op-btn`；危险 `.del-btn`/`.op-btn.danger`。改尺寸只改 `components.css` 一处。
- **按钮居中坑**：`<button>` 默认 `inline-block`，`margin:auto` 不生效；PC 限宽居中需 `display:block` + `margin:… auto`（或在 flex 容器用 `justify-content:center`）。
- 新增组件样式时：先查 `components.css` 是否已有；跨页面复用就放全局，仅单页使用才放页面 scoped。

## 编码规范

> **规范文件位于 `docs/harness/` 目录，开发前请查阅。**

| 文件 | 用途 |
|------|------|
| [alibaba-java-standard.md](docs/harness/alibaba-java-standard.md) | Java 命名、常量、异常、日志、并发等基础规范 |
| [spring-boot-practices.md](docs/harness/spring-boot-practices.md) | Spring Boot 依赖注入、Controller/Service/Repository 分层、事务、配置 |
| [test-guidelines.md](docs/harness/test-guidelines.md) | 测试框架、命名、隔离、断言规范 |
| [review-checklist.md](docs/harness/review-checklist.md) | Code Review 检查清单（命名、并发、事务、日志、SQL、测试） |

**注意**：`docs/harness/` 下的指南描述的是 TestNG + JPA + Flyway 技术栈，本仓库使用 **JUnit 5 + MyBatis-Plus**，仅借鉴其通用规则（命名、日志占位符、并发等），不套用框架/包名约定。

## 部署

生产环境部署到 **ecs-alr**（阿里云 ECS），通过 SSH 执行远程脚本。前后端分开部署，均有版本管理和回滚能力。

### 后端

```bash
./scripts/deploy-remote.sh               # 完整部署（拉代码 → mvn 编译 → 停旧服务 → 启动）
./scripts/deploy-remote.sh --skip-build   # 只重启，不编译
./scripts/deploy-remote.sh --rollback     # 回滚到上一个版本
```

本地 `deploy-remote.sh` 通过 SSH 连接 `ecs-alr`，执行远程的 `deploy-ecs.sh`。远程脚本在 ECS 上编译 jar 并放入 `/root/aliren-data/releases/`，通过软链 `current.jar` 切换版本。服务端口 8080，nginx 反代 80 → 8080。

### 前端（门户 + 租房 H5，统一产物树）

一个前端工程、一次构建、一次发布。`scripts/build-web.mjs` 把两个应用汇总成一个产物树：

```
dist/index.html + dist/assets/     门户（root-site/，React）    → /
dist/ali/index.html                门户 HTML 副本              → /ali
dist/ali/house/                    租房 H5（frontend/，Vue）   → /ali/house/
```

```bash
./scripts/deploy-web-remote.sh                # 构建 + 发布（构建 → 上传 → 切软链）
./scripts/deploy-web-remote.sh --skip-build   # 用现有 dist 发布
./scripts/deploy-web-remote.sh --rollback     # 回滚到上一个版本
./scripts/deploy-web-remote.sh --current      # 查看当前版本
```

本地 `deploy-web-remote.sh` 通过 SSH 连接 `ecs-alr`，执行远程的 `deploy-web-ecs.sh`。产物放入 `/root/aliren-data/web/releases/<时间戳>/`，软链 `web/current` 切换版本，nginx `location /` 的 root 指向 `web/current`。**门户与 H5 永远同版本，一次回滚退回两者的同一版本。**

构建顺序不能颠倒：门户的 webpack 配了 `output.clean`，会清空自己的 `dist`，所以**先建门户汇总到根 `dist/`，再建 H5 搬进 `dist/ali/house/`**。

**改 nginx 前必读的两个坑**：`dist/ali/index.html` 不能省 —— `dist/ali/` 目录一旦存在，请求 `/ali` 会先命中目录再找 index，缺了它 nginx 直接 403；同时 `/ali` 要用 `location = /ali` 精确匹配，否则会被 301 到 `/ali/`。

旧的 `deploy-frontend-*.sh`、`deploy-rootsite-*.sh` 与 `frontend/releases`、`root-site-releases` 是历史链路，已不再写入。

### 门户源码（root-site/）

**门户源码就在本仓库的 `root-site/`，改这里就是改线上门户。**

历史：`root-site/` 原本是独立工程 `goretire`（GitHub `Uoor/goretire`），2026-10-03 并入本仓库。**并入时做了删减，不是逐字节副本** —— 删掉了引用为零的死文件（`src/App.tsx`、`src/data/webpack.config.cjs`、`assets/{ali.css,site.js,content-data.js}`、`Bold_poster_style_*.png`、5 张无主配图、3 张与同名 `.png` 重复的群二维码），把 10 张 bot 配图压到 1200px/q80，并修好了 `tests/verify-site.mjs`。**所以不要从 goretire 整目录覆盖回来**，那会把这些改动全部冲掉。回看历史可以去那个仓库，但新增改动一律落在这里。

改完发布：`./scripts/deploy-web-remote.sh` —— **一条命令**，它内部会调 `scripts/build-web.mjs`（构建 → 上传 → 切软链）。`node scripts/build-web.mjs` 是只构建不发布的入口，单独跑不会上线。要单独跑检查就在 `root-site/` 里 `npm test`（它自己会 build）。

测试断言红了就改断言或改代码，**别"改一个跑一次"** —— 用 `node:assert` 打桩把 82 条断言逐条列出失败项，一次性对齐（做法见提交 `05e148b`）。

---

## 关键约定与坑

- **钉钉 markdown 换行**：Webhook markdown 单个 `\n` 不换行，必须用 `<br/>` 做行内换行，空行用 `\n\n`。见 [PublishOrchestrator.java](backend/houserent/src/main/java/com/aliren/houserent/robot/PublishOrchestrator.java) 的卡片构建。
- **发布编排目前是同步的**：`PublishOrchestrator.onHouseAudited`（审核通过 → 订阅/求租匹配 → 群卡片 + 私聊工作通知 + push_log 落库）在审核接口内同步执行，接真实 LLM/推送后应异步化。
- **定时任务**：`houserent/schedule/`（周五精选周推、周一安居故事），由 `aliren.schedule.enabled` 控制。
- **测试框架是 JUnit 5 + Mockito + AssertJ，不是 TestNG**。`docs/harness/` 下的指南文件描述的是另一套技术栈（TestNG、JPA、Flyway），**仅借鉴其通用规则**（命名、日志占位符、并发等），不套用框架/包名约定。
- **枚举/常量**：领域状态常量定义在实体类上（如 `House.AUDIT_ONLINE`、`House.LABEL_DIRECT`），不要散落魔法值。
- 产品/钉钉/前后端实现方案与 H5 原型见 `docs/design/`。
