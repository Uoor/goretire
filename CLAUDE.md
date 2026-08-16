# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

校友安居 (Alumni Housing) — 阿里/蚂蚁校友可信租房网络，钉钉内 H5 微应用。

- **Backend**: Spring Boot 3 (Java 17) + MySQL 8 + MyBatis-Plus，Maven 多模块
- **Frontend**: Vue 3 + Vant 4 + Vite + Pinia，钉钉 H5（hash 路由，postcss-px-to-viewport 按 375 设计稿转 vw）

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

## 关键约定与坑

- **钉钉 markdown 换行**：Webhook markdown 单个 `\n` 不换行，必须用 `<br/>` 做行内换行，空行用 `\n\n`。见 [PublishOrchestrator.java](backend/houserent/src/main/java/com/aliren/houserent/robot/PublishOrchestrator.java) 的卡片构建。
- **发布编排目前是同步的**：`PublishOrchestrator.onHouseAudited`（审核通过 → 订阅/求租匹配 → 群卡片 + 私聊工作通知 + push_log 落库）在审核接口内同步执行，接真实 LLM/推送后应异步化。
- **定时任务**：`houserent/schedule/`（周五精选周推、周一安居故事），由 `aliren.schedule.enabled` 控制。
- **测试框架是 JUnit 5 + Mockito + AssertJ，不是 TestNG**。`docs/harness/` 下的 `alibaba-java-standard.md` / `spring-boot-practices.md` / `test-guidelines.md` / `review-checklist.md` 描述的是另一套技术栈（`com.aibrg`、TestNG、JPA、Flyway），**与本仓库不匹配**，勿直接套用其框架/包名约定；如需借鉴仅取其通用规则（命名、日志占位符、并发等）。
- **枚举/常量**：领域状态常量定义在实体类上（如 `House.AUDIT_ONLINE`、`House.LABEL_DIRECT`），不要散落魔法值。
- 产品/钉钉/前后端实现方案与 H5 原型见 `docs/design/`。
