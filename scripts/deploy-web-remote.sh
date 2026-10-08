#!/bin/bash
# ============================================================
# 一起提前退休 · 统一前端发布启动器（本地执行）
#
# 构建在服务器上进行（与后端 deploy-remote.sh 同构）：本脚本只把命令转给
# ECS 上的 deploy-web-ecs.sh。本机不需要 node 工具链，也不传输产物 ——
# 前端产物只有一个来源：服务器。
#
# 用法:
#   ./scripts/deploy-web-remote.sh                     # 服务器拉代码 + 构建 + 切换
#   ./scripts/deploy-web-remote.sh --activate <版本名>  # 只切软链，不构建
#   ./scripts/deploy-web-remote.sh --rollback          # 回滚到上一个版本
#   ./scripts/deploy-web-remote.sh --current           # 查看当前版本
#
# 本地想自己看产物用 `node scripts/build-web.mjs`，那是只看不发布的入口。
# ============================================================
set -e

HOST="ecs-alr"
REMOTE_SCRIPT="/root/goretire/scripts/deploy-web-ecs.sh"

GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'
log() { echo -e "${GREEN}[INFO]${NC} $*"; }
err() { echo -e "${RED}[ERR]${NC}  $*"; }

MODE="${1:-}"
case "$MODE" in
    ""|--activate|--rollback|--current) ;;
    *)
        err "用法: $0 [--activate <版本名> | --rollback | --current]"
        exit 1
        ;;
esac

log "===== 在 $HOST 上执行前端发布（构建也在服务器上）====="
# 透传参数；无参时远程脚本走「拉代码 + 构建 + 切换」
ssh -o ConnectTimeout=10 "$HOST" "bash $REMOTE_SCRIPT ${MODE} ${2:-}"

log "=== 发布流程结束 ==="
log "  门户: https://goretire.cn/"
log "  租房: https://goretire.cn/ali/house/"
