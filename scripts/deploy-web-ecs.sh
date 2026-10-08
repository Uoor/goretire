#!/bin/bash
# ============================================================
# 一起提前退休 · 统一前端 ECS 侧发布脚本（在 ECS 上执行）
#
# **构建在服务器上进行**，与后端 deploy-ecs.sh 同构：
#   前置检查 → git pull → npm ci → build-web.mjs → 落版本目录 → 切软链
# 早期版本是「本地构建 + 上传产物」，2026-10-08 线上登录页故障正是那条路
# 造成的（本机 frontend/.env 缺 VITE_DING_APP_KEY，而服务器那份是对的），
# 现已废弃：产物只有一个来源 —— 这台服务器。
#
# 产物目录：/root/aliren-data/web/releases/<时间戳>/
#   ├── index.html        门户（React）   → /
#   ├── 404.html
#   ├── assets/                           → /assets/*
#   └── ali/
#       ├── index.html    门户副本        → /ali
#       └── house/        租房 H5（Vue）  → /ali/house/
# 软链：/root/aliren-data/web/current → 最新版本
# nginx：location / 的 root 指向 web/current
#
# 用法:
#   ./scripts/deploy-web-ecs.sh                     # 拉代码 + 构建 + 切换
#   ./scripts/deploy-web-ecs.sh --activate <版本名>  # 只切软链，不构建
#   ./scripts/deploy-web-ecs.sh --rollback          # 回滚到上一个版本
#   ./scripts/deploy-web-ecs.sh --current           # 查看当前版本
#
# 依赖：node/npm（Vite 8 要求 Node ^20.19 或 >=22.12）、frontend/.env
# ============================================================
set -e

# ---- 路径与常量 ----
REPO_DIR="/root/goretire"
DATA_DIR="/root/aliren-data"
RELEASES_DIR="$DATA_DIR/web/releases"
CURRENT_LINK="$DATA_DIR/web/current"
ENV_FILE="$REPO_DIR/frontend/.env"
REQUIRED_ENV_KEYS="VITE_DING_APP_KEY"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERR]${NC}  $*"; }

# ---- 前置检查（仅在构建模式下执行；回滚/切版本不该被配置问题挡住）----
pre_check() {
    if [ ! -d "$REPO_DIR/.git" ]; then
        err "仓库目录不存在: $REPO_DIR（请先 git clone）"
        exit 1
    fi

    # 未提交的 tracked 改动会让 git pull --ff-only 失败，或产生意外结果
    if [ -n "$(git -C "$REPO_DIR" status --porcelain -uno)" ]; then
        err "仓库有未提交的改动，请先处理再发布："
        git -C "$REPO_DIR" status --short -uno | sed 's/^/    /'
        exit 1
    fi

    # 构建期配置：本次事故的直接原因，必须在构建前拦住。
    # 注意 Vite 在构建期内联 VITE_*，值缺失不会报错，只会让代码路径被消除。
    if [ ! -f "$ENV_FILE" ]; then
        err "缺少构建期配置: $ENV_FILE"
        err "参考 frontend/.env.example 创建，至少包含: $REQUIRED_ENV_KEYS"
        exit 1
    fi
    local key value
    for key in $REQUIRED_ENV_KEYS; do
        value=$(grep -E "^${key}=" "$ENV_FILE" | tail -1 | cut -d'=' -f2- | tr -d "'\"")
        if [ -z "$value" ]; then
            err "$ENV_FILE 里 $key 为空"
            err "Vite 构建期内联 VITE_*，缺它会让钉钉扫码登录在线上静默失效"
            exit 1
        fi
    done
    log "构建期配置就绪: $ENV_FILE（$REQUIRED_ENV_KEYS 非空）"

    # node/npm：Vite 8 要求 Node ^20.19 或 >=22.12
    command -v node >/dev/null || { err "未找到 node"; exit 1; }
    command -v npm  >/dev/null || { err "未找到 npm"; exit 1; }
    if [ "$(node -p '(v=>{const[a,b]=v.split(".").map(Number);return (a===20&&b>=19)||(a===22&&b>=12)||a>22}) (process.versions.node)')" != "true" ]; then
        err "node 版本不满足 Vite 8 要求（^20.19 或 >=22.12）: $(node -v)"
        exit 1
    fi
    log "工具链就绪: node $(node -v) / npm $(npm -v)"

    mkdir -p "$RELEASES_DIR"
}

# ---- 构建（失败时绝不触碰 web/current，线上继续服务上一版）----
build() {
    log "===== 拉取最新代码 ====="
    git -C "$REPO_DIR" pull --ff-only
    echo ""

    log "===== 安装依赖（npm ci，严格按 lockfile）====="
    ( cd "$REPO_DIR/root-site" && npm ci --no-audit --no-fund )
    ( cd "$REPO_DIR/frontend"  && npm ci --no-audit --no-fund )
    echo ""

    log "===== 构建统一产物 ====="
    # 依赖已由上面的 npm ci 按 lockfile 装好，这里跳过脚本内的 npm install
    ( cd "$REPO_DIR" && node scripts/build-web.mjs --skip-install )
    echo ""

    if [ ! -f "$REPO_DIR/dist/index.html" ] || [ ! -f "$REPO_DIR/dist/ali/house/index.html" ]; then
        err "构建产物不完整（需要 dist/index.html 与 dist/ali/house/index.html）"
        exit 1
    fi

    # 用 mv 而非 cp：产物直接成为版本目录，不留半成品、不重复占磁盘
    local release_dir="$RELEASES_DIR/$TIMESTAMP"
    mv "$REPO_DIR/dist" "$release_dir"
    log "产物: $release_dir ($(du -sh "$release_dir" | cut -f1))"
}

# ---- 切换软链 ----
activate() {
    local name="$1"
    local target="$RELEASES_DIR/$name"
    if [ ! -d "$target" ]; then
        err "版本目录不存在: $target"
        exit 1
    fi
    # 两个入口都必须齐，否则发出去就是半残的站
    if [ ! -f "$target/index.html" ]; then
        err "版本目录缺少 index.html（门户）: $target"
        exit 1
    fi
    if [ ! -f "$target/ali/house/index.html" ]; then
        err "版本目录缺少 ali/house/index.html（租房 H5）: $target"
        exit 1
    fi
    ln -sfn "$target" "$CURRENT_LINK"
    log "已切换: $CURRENT_LINK -> $target"
}

# ---- 按修改时间倒序列出所有版本 ----
releases_desc() {
    ls -dt "$RELEASES_DIR"/*/ 2>/dev/null || true
}

# ---- 回滚 ----
rollback() {
    local current target="" dir
    current="$(readlink -f "$CURRENT_LINK" 2>/dev/null || true)"
    for dir in $(releases_desc); do
        if [ "$(readlink -f "$dir")" != "$current" ]; then
            target="$dir"
            break
        fi
    done
    if [ -z "$target" ]; then
        err "没有可回滚的版本"
        exit 1
    fi
    ln -sfn "$target" "$CURRENT_LINK"
    warn "已回滚: $CURRENT_LINK -> $target"
}

# ---- 主入口 ----
case "${1:-}" in
    --activate)
        [ -n "${2:-}" ] || { err "缺少版本目录名，例: --activate 20261008_161358"; exit 1; }
        activate "$2"
        ;;
    --rollback)
        rollback
        ;;
    --current)
        ;;
    "")
        pre_check
        build
        activate "$TIMESTAMP"
        ;;
    *)
        err "用法: $0 [--activate <版本名> | --rollback | --current]"
        exit 1
        ;;
esac

log "当前版本: $(readlink -f "$CURRENT_LINK" 2>/dev/null || echo '-')"
