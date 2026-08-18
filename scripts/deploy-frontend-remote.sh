#!/bin/bash
# ============================================================
# 校友安居 · 前端远程部署启动器（本地执行）
# 用法:
#   ./scripts/deploy-frontend-remote.sh              # 完整部署（拉代码→npm install→构建）
#   ./scripts/deploy-frontend-remote.sh --skip-build  # 只切换版本，不构建
#   ./scripts/deploy-frontend-remote.sh --rollback    # 回滚到上一个版本
# ============================================================
set -e

HOST="ecs-alr"
REMOTE_SCRIPT="/root/aliren/scripts/deploy-frontend-ecs.sh"

echo "🚀 连接 $HOST 执行前端部署..."
ssh -o ConnectTimeout=10 "$HOST" "bash $REMOTE_SCRIPT $*"
