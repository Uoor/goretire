# ali 子模块 · 租房业务（H5）

「一起提前退休」项目 `ali` 子模块下的租房业务，钉钉内 H5 微应用，线上 `/ali/house/`。

Vue 3 + Vant 4 + Vite + Pinia，hash 路由，`base: '/ali/house/'`，postcss-px-to-viewport 按 375 设计稿转 vw。

## 本地开发

```sh
npm install
npm run dev        # 5173；/ali/house/api 与 /ali/house/uploads 代理到线上后端
npm run build
npm test           # vitest run
```

## 结构

壳层 `src/{router,store,utils,api,styles}` + 业务模块 `src/modules/houserent/`（views / components / api.js）。
设计系统见 `DESIGN.md`，设计令牌在 `src/styles/tokens.css`。

## 部署

**不要单独发布本目录。** 它与首页（`root-site/`）由仓库根的 `scripts/build-web.mjs` 汇总成一个产物树、一次发布：

```sh
./scripts/deploy-web-remote.sh      # 构建 → 上传 → 切软链（一步）
```

细节见仓库根 `CLAUDE.md`。
