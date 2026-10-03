#!/bin/bash
# ============================================================
# 校友安居 · ECS 部署脚本（在 ECS 上执行）
# 用法:
#   ./scripts/deploy-ecs.sh                # 完整部署（拉代码→编译→重启）
#   ./scripts/deploy-ecs.sh --skip-build   # 只重启，不编译
#   ./scripts/deploy-ecs.sh --rollback     # 回滚到上一个版本
# ============================================================
set -e

# ---- 路径配置 ----
REPO_DIR="/root/goretire"
DATA_DIR="/root/aliren-data"
RELEASES_DIR="$DATA_DIR/releases"
LOGS_DIR="$DATA_DIR/logs"
ENV_FILE="$REPO_DIR/.env"
JAR_NAME="aliren-app"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LATEST_LINK="$RELEASES_DIR/current.jar"

# ---- 颜色 ----
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERR]${NC}  $*"; }

# ---- 前置检查 ----
pre_check() {
    if [ ! -d "$REPO_DIR/.git" ]; then
        err "仓库目录不存在: $REPO_DIR（请先 git clone）"
        exit 1
    fi
    if [ ! -f "$ENV_FILE" ]; then
        err "环境变量文件不存在: $ENV_FILE"
        err "请创建 .env 文件，至少包含: DB_PASSWORD, JWT_SECRET, DEV_CODE_ENABLED=false"
        exit 1
    fi
    mkdir -p "$RELEASES_DIR" "$LOGS_DIR"

    # 检查 Java
    if ! java -version &>/dev/null; then
        err "Java 未安装"
        exit 1
    fi

    # 检查 MySQL（从 .env 提取密码）
    DB_PASSWORD=$(grep -E '^DB_PASSWORD=' "$ENV_FILE" | cut -d'=' -f2- | tr -d "'\"")
    if ! mysql -uroot -p"$DB_PASSWORD" -e "USE aliren" &>/dev/null; then
        err "MySQL 连接失败或数据库 aliren 不存在"
        exit 1
    fi
    log "MySQL 连接正常"
}

# ---- 编译 ----
build() {
    log "===== 拉取最新代码 ====="
    cd "$REPO_DIR"
    git pull --ff-only
    echo ""

    log "===== 编译后端 ====="
    cd "$REPO_DIR/backend"
    mvn package -DskipTests -q
    echo ""

    JAR_FILE=$(find app/target -maxdepth 1 -name "aliren-app-*-exec.jar" | head -1)
    if [ -z "$JAR_FILE" ]; then
        err "未找到编译产物 aliren-app-*-exec.jar"
        exit 1
    fi

    RELEASE_JAR="$RELEASES_DIR/${JAR_NAME}-${TIMESTAMP}.jar"
    cp "$JAR_FILE" "$RELEASE_JAR"
    ln -sf "$RELEASE_JAR" "$LATEST_LINK"
    log "产物: $RELEASE_JAR ($(du -h "$RELEASE_JAR" | cut -f1))"
}

# ---- 停止 ----
stop() {
    log "===== 停止旧服务 ====="
    PID=$(pgrep -f "${JAR_NAME}-" 2>/dev/null || true)
    if [ -n "$PID" ]; then
        kill "$PID" 2>/dev/null || true
        log "已发送 SIGTERM (PID: $PID)"
        for i in $(seq 1 15); do
            if ! kill -0 "$PID" 2>/dev/null; then
                log "进程已退出"
                return 0
            fi
            sleep 1
        done
        warn "强制终止..."
        kill -9 "$PID" 2>/dev/null || true
    else
        log "无运行中的服务"
    fi
    sleep 2
}

# ---- 启动 ----
start() {
    log "===== 启动服务 ====="
    LAST_JAR=$(readlink "$LATEST_LINK" 2>/dev/null || ls -t "$RELEASES_DIR"/*.jar 2>/dev/null | head -1)
    if [ -z "$LAST_JAR" ]; then
        err "没有可用的 jar 包"
        exit 1
    fi

    # 加载 .env
    set -a
    source "$ENV_FILE"
    set +a

    nohup java -jar "$LAST_JAR" \
        --spring.profiles.active=prod \
        > "$LOGS_DIR/startup_${TIMESTAMP}.log" 2>&1 &
    NEW_PID=$!
    log "已启动 (PID: $NEW_PID)"

    # 等待就绪
    log "等待就绪..."
    for i in $(seq 1 40); do
        CODE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/ 2>/dev/null || true)
        if [ -n "$CODE" ] && [ "$CODE" != "000" ]; then
            log "就绪 (${i}s, HTTP $CODE)"
            echo ""
            log "==== 部署完成 ===="
            log "  PID:   $NEW_PID"
            log "  Jar:   $LAST_JAR"
            log "  Port:  8080 (nginx 反代 80→8080)"
            log "  日志:  tail -f $LOGS_DIR/startup_${TIMESTAMP}.log"
            return 0
        fi
        sleep 1
    done

    err "启动超时 40s，查看日志: tail -f $LOGS_DIR/startup_${TIMESTAMP}.log"
    return 1
}

# ---- 回滚 ----
rollback() {
    log "===== 回滚到上一个版本 ====="
    JARS=($(ls -t "$RELEASES_DIR"/*.jar 2>/dev/null))
    if [ ${#JARS[@]} -lt 2 ]; then
        err "没有可回滚的版本"
        exit 1
    fi
    PREV="${JARS[1]}"
    ln -sf "$PREV" "$LATEST_LINK"
    log "回滚到: $(basename $PREV)"
    stop
    start
}

# ---- 主入口 ----
MODE="${1:-full}"

pre_check

case "$MODE" in
    --skip-build)
        stop
        start
        ;;
    --rollback)
        rollback
        ;;
    *)
        build
        stop
        start
        ;;
esac
