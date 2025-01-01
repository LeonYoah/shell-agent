#!/bin/bash

# 配置信息
APP_NAME="shell-executor"
APP_VERSION="1.0.0"
# 添加实例ID支持
INSTANCE_ID="${INSTANCE_ID:-1}"
JAR_NAME="$APP_NAME-$APP_VERSION.jar"
BASE_DIR="/opt/apps/$APP_NAME-$INSTANCE_ID"
DEPLOY_DIR="$BASE_DIR"
LOG_DIR="$BASE_DIR/logs"
PID_FILE="$BASE_DIR/$APP_NAME.pid"

# JVM配置
JAVA_OPTS="-server -Xms512m -Xmx512m -Xmn256m"

# Dubbo配置
# 不同实例使用不同的dubbo端口
DUBBO_PORT="${DUBBO_PORT:-$((20880 + INSTANCE_ID - 1))}"
DUBBO_HOST="${DUBBO_HOST:-}"
DUBBO_NETWORK_INTERFACE="${DUBBO_NETWORK_INTERFACE:-}"

# HTTP端口配置
# 不同实例使用不同的HTTP端口
HTTP_PORT="${HTTP_PORT:-$((8080 + INSTANCE_ID - 1))}"
JAVA_OPTS="$JAVA_OPTS -Dserver.port=$HTTP_PORT"

# 如果没有指定DUBBO_HOST，尝试自动获取IP
if [ -z "$DUBBO_HOST" ] && [ -n "$DUBBO_NETWORK_INTERFACE" ]; then
    DUBBO_HOST=$(ip -4 addr show $DUBBO_NETWORK_INTERFACE | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | head -n 1)
fi

# 添加Dubbo配置到JAVA_OPTS
if [ -n "$DUBBO_HOST" ]; then
    JAVA_OPTS="$JAVA_OPTS -Ddubbo.protocol.host=$DUBBO_HOST"
fi
JAVA_OPTS="$JAVA_OPTS -Ddubbo.protocol.port=$DUBBO_PORT"
if [ -n "$DUBBO_NETWORK_INTERFACE" ]; then
    JAVA_OPTS="$JAVA_OPTS -Ddubbo.protocol.preferred-network-interface=$DUBBO_NETWORK_INTERFACE"
fi

# 环境配置
PROFILES_ACTIVE="prod"

# 确保目录存在
mkdir -p $DEPLOY_DIR
mkdir -p $LOG_DIR

# 获取应用PID
get_pid() {
    if [ -f $PID_FILE ]; then
        PID=$(cat $PID_FILE)
        if [ -n "$PID" ] && ps -p $PID > /dev/null; then
            echo $PID
            return
        fi
    fi
    echo $(ps -ef | grep $JAR_NAME | grep -v grep | awk '{print $2}')
}

# 等待进程启动
wait_for_startup() {
    local timeout=60
    local counter=0
    echo -n "Waiting for application startup"
    while [ $counter -lt $timeout ]; do
        if curl -s http://localhost:8080/actuator/health > /dev/null; then
            echo " OK"
            return 0
        fi
        echo -n "."
        sleep 1
        let counter+=1
    done
    echo " Failed"
    return 1
}

# 启动应用
start() {
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo "$APP_NAME is already running (PID: $PID)"
        return 1
    fi
    
    echo "Starting $APP_NAME..."
    echo "Using Dubbo configuration:"
    echo "  Host: ${DUBBO_HOST:-auto-detect}"
    echo "  Port: $DUBBO_PORT"
    echo "  Network Interface: ${DUBBO_NETWORK_INTERFACE:-auto-detect}"
    
    nohup java $JAVA_OPTS \
        -Dspring.profiles.active=$PROFILES_ACTIVE \
        -jar $DEPLOY_DIR/$JAR_NAME \
        > $LOG_DIR/startup.log 2>&1 &
        
    PID=$!
    echo $PID > $PID_FILE
    
    sleep 2
    if ps -p $PID > /dev/null; then
        echo "$APP_NAME started successfully (PID: $PID)"
        wait_for_startup
        return $?
    else
        echo "$APP_NAME failed to start, please check logs"
        rm -f $PID_FILE
        return 1
    fi
}

# 停止应用
stop() {
    PID=$(get_pid)
    if [ -z "$PID" ]; then
        echo "$APP_NAME is not running"
        rm -f $PID_FILE
        return 0
    fi
    
    echo "Stopping $APP_NAME (PID: $PID)..."
    kill $PID
    
    local timeout=30
    local counter=0
    while [ $counter -lt $timeout ] && ps -p $PID > /dev/null; do
        echo -n "."
        sleep 1
        let counter+=1
    done
    echo
    
    if ps -p $PID > /dev/null; then
        echo "Force stopping $APP_NAME..."
        kill -9 $PID
    fi
    
    rm -f $PID_FILE
    echo "$APP_NAME stopped"
}

# 重启应用
restart() {
    stop
    sleep 2
    start
}

# 检查状态
status() {
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo "$APP_NAME is running (PID: $PID)"
        return 0
    else
        echo "$APP_NAME is not running"
        return 1
    fi
}

# 查看日志
logs() {
    tail -f $LOG_DIR/startup.log
}

# 清理日志
clean_logs() {
    echo "Cleaning logs older than 7 days..."
    find $LOG_DIR -name "*.log*" -type f -mtime +7 -exec rm -f {} \;
    echo "Logs cleaned"
}

# 命令处理
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs
        ;;
    clean)
        clean_logs
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|logs|clean}"
        exit 1
esac

exit $? 