#!/bin/bash
# ============================================================
# 一起提前退休 · 统一前端 ECS 侧发布脚本（在 ECS 上执行）
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
#   ./scripts/deploy-web-ecs.sh --activate 20261003_010000
#   ./scripts/deploy-web-ecs.sh --rollback
#   ./scripts/deploy-web-ecs.sh --current
# ============================================================
set -e

# ---- 路径配置 ----
DATA_DIR="/root/aliren-data"
RELEASES_DIR="$DATA_DIR/web/releases"
CURRENT_LINK="$DATA_DIR/web/current"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERR]${NC}  $*"; }

# 按修改时间倒序列出所有版本
releases_desc() {
    ls -dt "$RELEASES_DIR"/*/ 2>/dev/null || true
}

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
    mkdir -p "$DATA_DIR/web"
    ln -sfn "$target" "$CURRENT_LINK"
    log "已切换: $CURRENT_LINK -> $target"
}

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

case "${1:-}" in
    --activate)
        [ -n "${2:-}" ] || { err "缺少版本目录名，例: --activate 20261003_010000"; exit 1; }
        activate "$2"
        ;;
    --rollback)
        rollback
        ;;
    --current)
        ;;
    *)
        err "用法: $0 --activate <版本名> | --rollback | --current"
        exit 1
        ;;
esac

log "当前版本: $(readlink -f "$CURRENT_LINK" 2>/dev/null || echo '-')"
