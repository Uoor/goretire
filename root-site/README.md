# 一起提前退休 · 首页（门户）

React + TypeScript + React Router + Ant Design + SCSS + Webpack 社区门户。站点采用单 HTML 入口，首页 `/` 和阿里社区 `/ali` 由客户端路由切换。

> **本目录是「一起提前退休」首页（门户）的源码，改这里就是改线上首页。** 历史来源是独立工程 `goretire`（GitHub `Uoor/goretire`），已于 2026-10-03 并入本仓库，之后不再从那边做手工同步。

页面按职责拆分：`src/app` 管路由，`src/pages` 放页面组合，`src/components/site` 放共享站点组件，`src/data` 放页面数据；全站样式位于 `assets/site.scss`。

## 本地开发

```sh
npm install
npm start
```

开发服务器地址为 `http://localhost:8774/`；阿里社区变体位于 `http://localhost:8774/ali`。

## 检查与构建

```sh
npm run typecheck
npm test          # npm run build && node tests/verify-site.mjs
```

`npm test` 是静态门禁。端到端浏览器 QA 是独立脚本，需要本机有 playwright 与 Chrome，且站点已在运行：

```sh
npm start            # 另开一个终端
node tests/browser-qa.mjs
```

缺 playwright 时它会明确提示并跳过，不会误报通过；`PLAYWRIGHT_MODULE` 与 `CHROME_PATH` 可指定非默认安装位置。

生产文件输出到 `dist/`，只生成根级 `index.html` 和 `404.html`，不会为 `/ali` 创建子目录。

## 部署

**不要单独发布本目录。** 门户与租房 H5（仓库根的 `frontend/`）由编排脚本汇总成一个产物树、**一条命令发布**。构建在服务器上进行（2026-10-08 从「本地构建 + 上传产物」改过来），所以**改动必须先 commit + push**：

```sh
./scripts/deploy-web-remote.sh      # 服务器拉代码 → 构建 → 切软链（一步）
```

`node scripts/build-web.mjs` 是**只构建、不发布**的入口，本地跑它只用于自己看产物；发布路径由服务器上的 `scripts/deploy-web-ecs.sh` 调它。

细节见仓库根 `CLAUDE.md` 的「前端（门户 + 租房 H5，统一产物树）」，含构建顺序与 nginx 的两个坑。
