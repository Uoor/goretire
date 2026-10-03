#!/usr/bin/env node
// ============================================================
// 一起提前退休 · 统一前端构建
//
// 一次构建产出统一产物树（两个应用，一套工程）：
//   dist/                  门户（React + webpack）  → /
//   dist/ali/index.html    门户 HTML 副本           → /ali
//   dist/ali/house/        租房 H5（Vue + Vite）    → /ali/house/
//
// 顺序很关键：门户的 webpack 配了 output.clean，会清空自己的 dist，
// 所以先建门户汇总到根 dist/，再建 H5 搬进 dist/ali/house/，两者永不互清。
//
// 用法:
//   node scripts/build-web.mjs                  # 安装依赖 + 构建
//   node scripts/build-web.mjs --skip-install   # 跳过 npm install
// ============================================================
import { cp, mkdir, rm } from "node:fs/promises";
import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const dist = path.join(root, "dist");
const portalDir = path.join(root, "root-site");
const houseDir = path.join(root, "frontend");
const skipInstall = process.argv.includes("--skip-install");

function run(command, args, cwd) {
  const result = spawnSync(command, args, { cwd, stdio: "inherit" });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    console.error(`\n[build-web] 失败：${command} ${args.join(" ")}  (cwd=${cwd})`);
    process.exit(result.status ?? 1);
  }
}

const step = (message) => console.log(`\n[build-web] ${message}`);

step("清理 dist/");
await rm(dist, { recursive: true, force: true });

if (!skipInstall) {
  step("安装门户依赖 root-site/");
  run("npm", ["install", "--no-audit", "--no-fund"], portalDir);
}

step("构建门户 root-site/");
run("npm", ["run", "build"], portalDir);

step("汇总门户产物 → dist/");
await cp(path.join(portalDir, "dist"), dist, { recursive: true });

// dist/ali/ 一旦存在，请求 /ali 会命中目录再找 index.html；
// 少了这份副本，nginx 会直接 403 而不是回退到门户首页。
step("生成 dist/ali/index.html（门户副本，避免 /ali 命中目录后 403）");
await mkdir(path.join(dist, "ali"), { recursive: true });
await cp(path.join(dist, "index.html"), path.join(dist, "ali", "index.html"));

if (!skipInstall) {
  step("安装租房 H5 依赖 frontend/");
  run("npm", ["install", "--no-audit", "--no-fund"], houseDir);
}

step("构建租房 H5 frontend/");
run("npm", ["run", "build"], houseDir);

step("汇总租房 H5 → dist/ali/house/");
await cp(path.join(houseDir, "dist"), path.join(dist, "ali", "house"), { recursive: true });
await rm(path.join(houseDir, "dist"), { recursive: true, force: true });

step("完成，产物在 dist/");
