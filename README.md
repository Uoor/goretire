# 校友安居（Alumni Housing）

> 阿里/蚂蚁校友可信租房网络 · 钉内 H5 微应用
> 技术栈：Spring Boot 3（Java 17）+ MySQL 8 + MyBatis-Plus ｜ Vue 3 + Vant 4 + Vite

## 工程结构

```
aliren/
├── backend/                 # 后端（Maven 多模块）
│   ├── core/                #   基础设施：认证/用户/统一响应/拦截器
│   ├── houserent/           #   租房业务：房源/求租/订阅/匹配/审核/举报/定时
│   ├── app/                 #   应用入口（唯一可执行，exec fat jar + 配置 + 建表脚本）
│   └── test/                #   集中测试（59 个用例）
├── frontend/                # 前端（Vue3 + Vant4，壳 + modules/houserent 业务模块）
│   ├── DESIGN.md            #   设计系统（源自原型，供 AI/开发者遵循）
│   └── src/
│       ├── styles/          #   设计令牌 tokens.css
│       ├── router|store|utils|api   # 壳层
│       └── modules/houserent/       # 租房页面/组件/接口
└── docs/design/             # 产品/钉钉/前后端实现方案 + H5 原型
```

## 启动指南

### 0. 前置：MySQL（127.0.0.1:3306，用户 root，密码见 `backend/app/src/main/resources/application.yml`）

```bash
# 建库建表 + 种子数据（含管理员 dev-code、5 套房源、需求/订阅样例）
mysql -h 127.0.0.1 -u root -p < backend/app/src/main/resources/db/schema.sql
mysql -h 127.0.0.1 -u root -p aliren < backend/app/src/main/resources/db/data-mysql.sql
```

### 1. 后端（默认 8080；本机 8080 被占时用 8081，前端代理已指向 8081）

```bash
cd backend && mvn package -DskipTests
java -jar app/target/aliren-app-0.1.0-SNAPSHOT-exec.jar --server.port=8081
```

### 2. 前端（5173）

```bash
cd frontend && npm install && npm run dev
# 浏览器打开 http://localhost:5173
```

### 3. 联调说明

- **体验方式（二选一）**：
  1. **桌面浏览器**：打开 http://localhost:5173，页面自动呈"手机壳"模式（深色画布 + 圆角手机容器），观感与操作接近真机
  2. **真机预览（推荐，最接近钉钉内）**：dev server 已 `--host` 暴露局域网，手机与电脑连同一 WiFi，浏览器访问 `http://<电脑局域网IP>:5173`（如 http://192.168.31.126:5173），`/api` 请求经 Vite 代理直达后端
- **免登**：浏览器联调自动用 `dev-code` 桩登录（后端 stub 放行）；种子用户 `dev-code` 为管理员，可同时体验校友端与管理后台（我的 → 管理后台，或直接 `#/admin`）
- **LLM 匹配**：未配置 `LLM_API_KEY` 时匹配引擎自动降级为本地过滤（返回"本地降级匹配"理由）；配置后走通义千问
- **钉钉真实能力**（免登换号/机器人推送）：配置 `DING_APP_KEY/DING_APP_SECRET` 与 `ROBOT_ENABLED=true` 后启用，当前为桩实现
- **定时任务**：周五 19:00 精选周推、周一 10:00 安居故事（`aliren.schedule.enabled` 控制；推送走日志桩）

## 已实现功能（MVP 闭环）

- 发布房源 → 管理员审核（通过/驳回）→ 已上架 → 列表/详情/筛选
- 一句话找房（LLM 或降级）、求租墙、订阅中心（暂停/恢复）
- 我的发布（状态跟踪 + 已租出下架 + 轻问句）、举报、管理看板
- 审核/下架/举报处理审计日志；周推/安居故事定时任务骨架
- 后端 59 个测试全绿；前端构建通过，与后端全接口联调验证

## 未完成（需真实凭证）

- 钉钉免登真实换号（authCode→unionId→userid）、机器人真实发送
- 图片上传（当前发布页用占位图）、OSS 接入
