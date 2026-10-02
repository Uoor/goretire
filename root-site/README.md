# 一起提前退休

React + TypeScript + React Router + Ant Design + SCSS + Webpack 社区门户。站点采用单 HTML 入口，首页 `/` 和阿里社区 `/ali` 由客户端路由切换。

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
npm test
```

生产文件输出到 `dist/`，只生成根级 `index.html` 和 `404.html`，不会为 `/ali` 创建子目录。静态托管发布此目录即可；`404.html` 用于 GitHub Pages 等静态主机上的深路径回退。自托管 Nginx/CDN 应将未知路径 rewrite 到 `index.html`。

部署到域名子路径时设置 `PUBLIC_URL=/仓库名` 后再执行 `npm run build`，Router 和 JS/CSS 资源会使用同一 basename。