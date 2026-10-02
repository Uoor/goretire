#!/bin/bash
# ============================================================
# 一起提前退休 · 统一前端发布启动器（本地执行：构建 → 上传 → 切换软链）
#
# 一次发布同时上线门户与租房 H5，两者永远同版本。
# 构建由 scripts/build-web.mjs 负责，产出统一产物树 dist/。
#
# 用法:
#   ./scripts/deploy-web-remote.sh                # 构建 + 发布
#   ./scripts/deploy-web-remote.sh --skip-build   # 用现有 dist 发布
#   ./scripts/deploy-web-remote.sh --rollback     # 回滚到上一个版本
#   ./scripts/deploy-web-remote.sh --current      # 查看当前版本
# ============================================================
set -e

HOST="ecs-alr"
DATA_DIR="/root/aliren-data"
RELEASES_DIR="$DATA_DIR/web/releases"
CURRENT_LINK="$DATA_DIR/web/current"
REMOTE_SCRIPT="/root/aliren/scripts/deploy-web-ecs.sh"
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="$REPO_DIR/dist"

GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'
log() { echo -e "${GREEN}[INFO]${NC} $*"; }
err() { echo -e "${RED}[ERR]${NC}  $*"; }

MODE="${1:-full}"

case "$MODE" in
    --rollback|--current)
        ssh -o ConnectTimeout=10 "$HOST" "bash $REMOTE_SCRIPT $MODE"
        exit 0
        ;;
esac

if [ "$MODE" != "--skip-build" ]; then
    log "===== 构建统一前端产物 ====="
    cd "$REPO_DIR"
    node scripts/build-web.mjs
fi

if [ ! -f "$DIST_DIR/index.html" ] || [ ! -f "$DIST_DIR/ali/house/index.html" ]; then
    err "dist/ 不完整（需要 index.html 与 ali/house/index.html），请先执行不带 --skip-build 的发布"
    exit 1
fi

TS=$(date +%Y%m%d_%H%M%S)
REMOTE_RELEASE="$RELEASES_DIR/$TS"

log "===== 上传到 $REMOTE_RELEASE ====="
ssh -o ConnectTimeout=10 "$HOST" "mkdir -p $REMOTE_RELEASE"
# COPYFILE_DISABLE 避免 macOS 打包出 ._* 资源分叉文件
COPYFILE_DISABLE=1 tar -czf - -C "$DIST_DIR" . \
    | ssh -o ConnectTimeout=10 "$HOST" "tar --warning=no-unknown-keyword -xzf - -C $REMOTE_RELEASE"
ssh -o ConnectTimeout=10 "$HOST" \
    "find $REMOTE_RELEASE -name '._*' -delete; chown -R root:root $REMOTE_RELEASE"

log "===== 切换版本 ====="
ssh -o ConnectTimeout=10 "$HOST" "bash $REMOTE_SCRIPT --activate $TS"

log "==== 发布完成 ===="
log "  版本: $REMOTE_RELEASE"
log "  软链: $CURRENT_LINK"
log "  门户: https://goretire.cn/"
log "  租房: https://goretire.cn/ali/house/"
