#!/bin/bash
# ============================================================
# 校友安居 · 前端 ECS 部署脚本（在 ECS 上执行）
# 构建产物放入 /root/aliren-data/frontend/releases/，nginx 直接指向 current 软链。
# 用法:
#   ./scripts/deploy-frontend-ecs.sh              # 完整部署
#   ./scripts/deploy-frontend-ecs.sh --skip-build  # 只切换版本
#   ./scripts/deploy-frontend-ecs.sh --rollback    # 回滚
# ============================================================
set -e

# ---- 路径配置 ----
REPO_DIR="/root/aliren"
DATA_DIR="/root/aliren-data"
RELEASES_DIR="$DATA_DIR/frontend/releases"
CURRENT_LINK="$DATA_DIR/frontend/current"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERR]${NC}  $*"; }

# ---- 前置检查 ----
pre_check() {
    if [ ! -d "$REPO_DIR/.git" ]; then
        err "仓库目录不存在: $REPO_DIR"
        exit 1
    fi
    if ! node -v &>/dev/null; then
        err "Node.js 未安装"
        exit 1
    fi
    mkdir -p "$RELEASES_DIR"
    log "Node $(node -v) · npm $(npm -v)"
}

# ---- 构建 ----
build() {
    log "===== 拉取最新代码 ====="
    cd "$REPO_DIR"
    git pull --ff-only

    log "===== 安装依赖 ====="
    cd "$REPO_DIR/frontend"
    npm install --no-audit --no-fund

    log "===== 构建 ====="
    npm run build

    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    RELEASE_DIR="$RELEASES_DIR/$TIMESTAMP"
    cp -r dist "$RELEASE_DIR"
    ln -sfn "$RELEASE_DIR" "$CURRENT_LINK"
    log "产物: $RELEASE_DIR ($(du -h "$RELEASE_DIR" | cut -f1))"
}

# ---- 回滚 ----
rollback() {
    log "===== 回滚到上一个版本 ====="
    DIRS=($(ls -dt "$RELEASES_DIR"/*/ 2>/dev/null))
    if [ ${#DIRS[@]} -lt 2 ]; then
        err "没有可回滚的版本"
        exit 1
    fi
    PREV="${DIRS[1]}"
    ln -sfn "$PREV" "$CURRENT_LINK"
    log "回滚到: $PREV"
}

# ---- 主入口 ----
MODE="${1:-full}"
pre_check

case "$MODE" in
    --skip-build)
        log "跳过构建，当前版本: $(readlink "$CURRENT_LINK")"
        ;;
    --rollback)
        rollback
        ;;
    *)
        build
        ;;
esac

log "==== 前端部署完成 ===="
log "  当前: $(readlink "$CURRENT_LINK")"
log "  nginx → $CURRENT_LINK"
