# 校友安居 · 目录结构重构设计（backend 多模块 + frontend）

> 日期：2026-08-16
> 状态：已批准（用户确认 core 边界与包名策略）
> 关联计划：`docs/superpowers/plans/2026-08-16-aliren-backend-core.md`

## 背景

当前工程为单模块 Spring Boot 应用（`com.aliren.*` 根包），全部代码为租房相关后端。
后续将新增 demand（求租）、subscribe（订阅）、match（LLM 匹配）、robot（机器人）等多个后端模块，以及 Vue3 H5 前端。
需要提前将工程拆分为可扩展的多模块结构。

## 目标结构

```
aliren/
├── backend/                              # 后端父工程（Maven 多模块）
│   ├── pom.xml                           # 父 pom：packaging=pom，聚合 core/rent/test
│   ├── core/                             # 核心公共模块（唯一含启动类的模块）
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/aliren/core/
│   │       │   ├── AlirenApplication.java    # 启动类，scanBasePackages="com.aliren"
│   │       │   ├── common/                   # ApiResponse / BusinessException / GlobalExceptionHandler
│   │       │   ├── auth/                     # AuthController / AuthService / JwtUtil / LoginUser / UserContext / DingTalkClient(+Impl/Stub) / dto
│   │       │   ├── user/                     # User / UserMapper
│   │       │   ├── config/                   # WebConfig
│   │       │   └── interceptor/              # AuthInterceptor
│   │       └── resources/                    # application.yml + db/schema.sql
│   ├── rent/                             # 租房业务模块（依赖 core）
│   │   ├── pom.xml
│   │   └── src/main/java/com/aliren/rent/
│   │       ├── house/                        # House / Mapper / Service / Controller / dto/*
│   │       └── admin/                        # AdminAuditService / Controller / dto/AuditRequest
│   └── test/                             # 测试模块（依赖 rent，集中全部测试）
│       ├── pom.xml
│       └── src/test/
│           ├── java/com/aliren/              # 包名跟随被测代码
│           └── resources/application.yml     # H2 测试配置
└── frontend/                             # 前端目录（占位 README，后续放 Vue3 H5）
    └── README.md
```

## 关键决策

1. **模块边界**：core 包含认证+用户体系（common/auth/user/config/interceptor），
   是所有业务模块的公共依赖；rent 仅含租房业务（house/admin）。
2. **包名**：按模块拆分——`com.aliren.core.*` 与 `com.aliren.rent.*`；
   后续新模块如 `com.aliren.demand.*`。同步更新全部 package/import。
3. **依赖方向单向**：`core ← rent ← test`；父 pom 聚合，`<module>` 新增一行即可扩展模块。
4. **启动类**：放 core（`com.aliren.core.AlirenApplication`），
   加 `@SpringBootApplication(scanBasePackages = "com.aliren")` 以扫描全部模块；
   `spring-boot-maven-plugin`（fat jar）仅配置在 core。
   - 注意：core 的 fat jar 不含 rent 等业务模块的类，开发期以 IDE 运行（reactor classpath 完整）；
     若需独立部署包，后续可增加 app 聚合模块（本设计不做，YAGNI）。
5. **测试集中**：现有 30 个测试均为 public API 的 Mockito 单测，无包私有访问，可整体迁入 test 模块；
   `ContextLoadsTest` 显式指定 `@SpringBootTest(classes = AlirenApplication.class)`。
6. **配置归属**：application.yml / schema.sql 为应用级，随启动类进 core 资源目录；
   测试 H2 配置在 test 模块，classpath 优先级保证覆盖。
7. **Mapper**：UserMapper / HouseMapper 均已用 `@Mapper` 注解，跨模块组件扫描无需额外 `@MapperScan`。
8. **迁移方式**：`git mv` 保留文件历史；顶层 docs/、.github/、.gitignore 不动。
9. **前端**：仅建占位 README，不初始化框架（技术选型后续单独决策）。

## 验证标准

- `cd backend && mvn -q test` 全部通过（30 个测试 + 上下文加载）。
- 原功能行为不变：免登换号（stub）、JWT 鉴权、房源发布/列表/详情、管理员审核/下架。

## 提交拆分

1. `chore: restructure backend into multi-module (pom + core)`
2. `refactor: move rent business code to rent module`
3. `refactor: consolidate tests into test module + frontend placeholder`
