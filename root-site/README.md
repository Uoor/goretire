# 一起提前退休 · 门户

React + TypeScript + React Router + Ant Design + SCSS + Webpack 社区门户。站点采用单 HTML 入口，首页 `/` 和阿里社区 `/ali` 由客户端路由切换。

> **本目录是 aliren 仓库的前端门户源码，改这里就是改线上门户。** 历史来源是独立工程 `goretire`（GitHub `Uoor/goretire`），已于 2026-10-03 并入本仓库，之后不再从那边做手工同步。

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

生产文件输出到 `dist/`，只生成根级 `index.html` 和 `404.html`，不会为 `/ali` 创建子目录。

## 部署

**不要单独发布本目录。** 门户与租房 H5（仓库根的 `frontend/`）由编排脚本汇总成一个产物树、一次发布：

```sh
node scripts/build-web.mjs          # 产出根 dist/：门户 → /，门户副本 → /ali，H5 → /ali/house/
./scripts/deploy-web-remote.sh      # 构建 + 上传 + 切软链
```

细节见仓库根 `CLAUDE.md` 的「前端（门户 + 租房 H5，统一产物树）」，含构建顺序与 nginx 的两个坑。
