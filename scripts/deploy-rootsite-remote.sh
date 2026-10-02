#!/bin/bash
# ============================================================
# 一起提前退休官网 · 发布启动器（本地执行：构建 → 上传 → 切换软链）
#
# 源码：root-site/（React + TS + Ant Design + Webpack）
# 产物：root-site/dist/（index.html + 404.html + assets/）
# 远端：/root/aliren-data/root-site-releases/site_<时间戳>，软链 root-site 指向最新版
#
# 用法:
#   ./scripts/deploy-rootsite-remote.sh                # 构建 + 发布
#   ./scripts/deploy-rootsite-remote.sh --skip-build   # 用现有 dist 发布
#   ./scripts/deploy-rootsite-remote.sh --rollback     # 回滚到上一个版本
#   ./scripts/deploy-rootsite-remote.sh --current      # 查看当前版本
# ============================================================
set -e

HOST="ecs-alr"
DATA_DIR="/root/aliren-data"
RELEASES_DIR="$DATA_DIR/root-site-releases"
CURRENT_LINK="$DATA_DIR/root-site"
REMOTE_SCRIPT="/root/aliren/scripts/deploy-rootsite-ecs.sh"
SITE_DIR="$(cd "$(dirname "$0")/../root-site" && pwd)"

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
    log "===== 构建官网 ====="
    cd "$SITE_DIR"
    npm install --no-audit --no-fund
    npm run build
fi

if [ ! -f "$SITE_DIR/dist/index.html" ]; then
    err "缺少 $SITE_DIR/dist/index.html，请先执行不带 --skip-build 的发布"
    exit 1
fi

TS=$(date +%Y%m%d_%H%M%S)
RELEASE_NAME="site_$TS"
REMOTE_RELEASE="$RELEASES_DIR/$RELEASE_NAME"

log "===== 上传到 $REMOTE_RELEASE ====="
ssh -o ConnectTimeout=10 "$HOST" "mkdir -p $REMOTE_RELEASE"
# COPYFILE_DISABLE 避免 macOS 打包出 ._* 资源分叉文件
COPYFILE_DISABLE=1 tar -czf - -C "$SITE_DIR/dist" . \
    | ssh -o ConnectTimeout=10 "$HOST" "tar --warning=no-unknown-keyword -xzf - -C $REMOTE_RELEASE"
ssh -o ConnectTimeout=10 "$HOST" \
    "find $REMOTE_RELEASE -name '._*' -delete; chown -R root:root $REMOTE_RELEASE"

log "===== 切换版本 ====="
ssh -o ConnectTimeout=10 "$HOST" "bash $REMOTE_SCRIPT --activate $RELEASE_NAME"

log "==== 发布完成 ===="
log "  版本: $REMOTE_RELEASE"
log "  软链: $CURRENT_LINK"
log "  域名: https://goretire.cn/"
