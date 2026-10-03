#!/bin/bash
# ============================================================
# 校友安居 · 远程部署启动器（本地执行）
# 用法:
#   ./scripts/deploy-remote.sh                # 完整部署（拉代码→编译→重启）
#   ./scripts/deploy-remote.sh --skip-build   # 只重启，不编译
#   ./scripts/deploy-remote.sh --rollback     # 回滚到上一个版本
# ============================================================
set -e

HOST="ecs-alr"
REMOTE_SCRIPT="/root/goretire/scripts/deploy-ecs.sh"

echo "🚀 连接 $HOST 执行部署..."
ssh -o ConnectTimeout=10 "$HOST" "bash $REMOTE_SCRIPT $*"
