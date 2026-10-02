#!/bin/bash
# ============================================================
# 一起提前退休官网 · ECS 侧发布脚本（在 ECS 上执行）
#
# 站点目录：/root/aliren-data/root-site（软链，指向 root-site-releases/site_<时间戳>）
# nginx：location / → root /root/aliren-data/root-site
#
# 用法:
#   ./scripts/deploy-rootsite-ecs.sh --activate site_20261003_000751
#   ./scripts/deploy-rootsite-ecs.sh --rollback
#   ./scripts/deploy-rootsite-ecs.sh --current
# ============================================================
set -e

# ---- 路径配置 ----
DATA_DIR="/root/aliren-data"
RELEASES_DIR="$DATA_DIR/root-site-releases"
CURRENT_LINK="$DATA_DIR/root-site"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERR]${NC}  $*"; }

# 按修改时间倒序列出正式版本（忽略 legacy_* 归档）
releases_desc() {
    ls -dt "$RELEASES_DIR"/site_*/ 2>/dev/null || true
}

activate() {
    local name="$1"
    local target="$RELEASES_DIR/$name"
    if [ ! -d "$target" ]; then
        err "版本目录不存在: $target"
        exit 1
    fi
    if [ ! -f "$target/index.html" ]; then
        err "版本目录缺少 index.html，拒绝发布: $target"
        exit 1
    fi
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
        [ -n "${2:-}" ] || { err "缺少版本目录名，例: --activate site_20261003_000751"; exit 1; }
        activate "$2"
        ;;
    --rollback)
        rollback
        ;;
    --current)
        ;;
    *)
        err "用法: $0 --activate <site_xxx> | --rollback | --current"
        exit 1
        ;;
esac

log "当前版本: $(readlink -f "$CURRENT_LINK" 2>/dev/null || echo '-')"
