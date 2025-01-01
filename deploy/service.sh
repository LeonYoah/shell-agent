#!/bin/bash

# 配置信息
APP_NAME="shell-executor"
APP_VERSION="1.0.0"
JAR_NAME="$APP_NAME-$APP_VERSION.jar"
DEPLOY_DIR="/opt/apps/$APP_NAME"
LOG_DIR="/opt/apps/$APP_NAME/logs"
JAVA_OPTS="-server -Xms512m -Xmx512m -Xmn256m"
PROFILES_ACTIVE="prod"

# 获取应用PID
get_pid() {
    echo $(ps -ef | grep $JAR_NAME | grep -v grep | awk '{print $2}')
}

# 启动应用
start() {
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo "$APP_NAME is already running (PID: $PID)"
        return
    fi
    
    echo "Starting $APP_NAME..."
    nohup java $JAVA_OPTS \
        -Dspring.profiles.active=$PROFILES_ACTIVE \
        -jar $DEPLOY_DIR/$JAR_NAME \
        > $LOG_DIR/startup.log 2>&1 &
        
    sleep 5
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo "$APP_NAME started successfully (PID: $PID)"
    else
        echo "$APP_NAME failed to start, please check logs"
        exit 1
    fi
}

# 停止应用
stop() {
    PID=$(get_pid)
    if [ -z "$PID" ]; then
        echo "$APP_NAME is not running"
        return
    fi
    
    echo "Stopping $APP_NAME (PID: $PID)..."
    kill $PID
    sleep 5
    
    if ps -p $PID > /dev/null; then
        echo "Force stopping $APP_NAME..."
        kill -9 $PID
    fi
    
    echo "$APP_NAME stopped"
}

# 重启应用
restart() {
    stop
    sleep 2
    start
}

# 查看状态
status() {
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo "$APP_NAME is running (PID: $PID)"
    else
        echo "$APP_NAME is not running"
    fi
}

# 查看日志
logs() {
    tail -f $LOG_DIR/startup.log
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
    *)
        echo "Usage: $0 {start|stop|restart|status|logs}"
        exit 1
esac

exit 0 