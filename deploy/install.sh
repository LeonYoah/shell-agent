#!/bin/bash

# 配置信息
APP_NAME="shell-executor"
INSTANCE_ID="${1:-1}"  # 通过参数传入实例ID
SERVICE_FILE="/etc/systemd/system/$APP_NAME-$INSTANCE_ID.service"
BASE_DIR="/opt/apps/$APP_NAME-$INSTANCE_ID"

# 检查是否为root用户
if [ "$(id -u)" != "0" ]; then
    echo "This script must be run as root" 1>&2
    exit 1
fi

# 创建应用目录
mkdir -p $BASE_DIR
mkdir -p $BASE_DIR/logs

# 生成服务配置文件
cat > $SERVICE_FILE << EOF
[Unit]
Description=Shell Executor Service (Instance $INSTANCE_ID)
After=network.target

[Service]
Type=forking
User=root
Group=root
Environment="JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64"
Environment="PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
Environment="PROFILES_ACTIVE=prod"
Environment="INSTANCE_ID=$INSTANCE_ID"
Environment="DUBBO_PORT=$((20880 + INSTANCE_ID - 1))"
Environment="HTTP_PORT=$((8080 + INSTANCE_ID - 1))"
Environment="DUBBO_HOST="
Environment="DUBBO_NETWORK_INTERFACE=eth0"
WorkingDirectory=$BASE_DIR
ExecStart=$BASE_DIR/service.sh start
ExecStop=$BASE_DIR/service.sh stop
ExecReload=$BASE_DIR/service.sh restart
PIDFile=$BASE_DIR/$APP_NAME.pid
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 复制服务脚本
cp service.sh $BASE_DIR/
chmod +x $BASE_DIR/service.sh

# 复制JAR包
cp $APP_NAME-*.jar $BASE_DIR/$APP_NAME-$APP_VERSION.jar

# 重新加载systemd配置
systemctl daemon-reload

# 启用服务开机自启动
systemctl enable $APP_NAME-$INSTANCE_ID.service

echo "Installation completed for instance $INSTANCE_ID. You can now manage the service using:"
echo "systemctl {start|stop|restart|status} $APP_NAME-$INSTANCE_ID"
echo "Or use the service script directly:"
echo "$BASE_DIR/service.sh {start|stop|restart|status|logs|clean}"

# 显示端口信息
echo -e "\nInstance ports:"
echo "HTTP port: $((8080 + INSTANCE_ID - 1))"
echo "Dubbo port: $((20880 + INSTANCE_ID - 1))" 