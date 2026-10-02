import { spawnSync } from "node:child_process";
import { rm } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const webpack = path.join(root, "node_modules", "webpack", "bin", "webpack.js");
await rm(path.join(root, "dist"), { recursive: true, force: true });
const result = spawnSync(process.execPath, [webpack, "--mode", "production"], { cwd: root, stdio: "inherit" });

if (result.error) throw result.error;
if (result.status !== 0) process.exit(result.status ?? 1);
