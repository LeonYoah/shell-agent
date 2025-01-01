#!/bin/bash

# 配置信息
APP_NAME="shell-executor"
APP_VERSION="1.0.0"
JAR_NAME="$APP_NAME-$APP_VERSION.jar"
BASE_DIR="/opt/apps/$APP_NAME"
DEPLOY_DIR="$BASE_DIR"
LOG_DIR="$BASE_DIR/logs"
JAVA_OPTS="-server -Xms512m -Xmx512m -Xmn256m"
PROFILES_ACTIVE="prod"

# 创建必要的目录
mkdir -p $DEPLOY_DIR
mkdir -p $LOG_DIR

# 检查是否已运行
PID=$(ps -ef | grep $JAR_NAME | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "Stopping $APP_NAME (PID: $PID)..."
    kill $PID
    sleep 5
    
    # 检查是否仍在运行
    if ps -p $PID > /dev/null; then
        echo "Force stopping $APP_NAME..."
        kill -9 $PID
    fi
fi

# 备份旧版本
if [ -f "$DEPLOY_DIR/$JAR_NAME" ]; then
    mv $DEPLOY_DIR/$JAR_NAME $DEPLOY_DIR/$JAR_NAME.$(date +%Y%m%d%H%M%S).bak
fi

# 复制新版本
cp $JAR_NAME $DEPLOY_DIR/

# 启动应用
echo "Starting $APP_NAME..."
nohup java $JAVA_OPTS \
    -Dspring.profiles.active=$PROFILES_ACTIVE \
    -jar $DEPLOY_DIR/$JAR_NAME \
    > $LOG_DIR/startup.log 2>&1 &

# 等待启动
sleep 5

# 检查是否启动成功
PID=$(ps -ef | grep $JAR_NAME | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "$APP_NAME started successfully (PID: $PID)"
else
    echo "$APP_NAME failed to start, please check logs"
    exit 1
fi 