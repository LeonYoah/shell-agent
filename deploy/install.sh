#!/bin/bash

# 配置信息
APP_NAME="shell-executor"
SERVICE_FILE="/etc/systemd/system/$APP_NAME.service"
BASE_DIR="/opt/apps/$APP_NAME"

# 检查是否为root用户
if [ "$(id -u)" != "0" ]; then
    echo "This script must be run as root" 1>&2
    exit 1
fi

# 创建应用目录
mkdir -p $BASE_DIR
mkdir -p $BASE_DIR/logs

# 复制服务文件
cp shell-executor.service $SERVICE_FILE
chmod 644 $SERVICE_FILE

# 复制服务脚本
cp service.sh $BASE_DIR/
chmod +x $BASE_DIR/service.sh

# 重新加载systemd配置
systemctl daemon-reload

# 启用服务开机自启动
systemctl enable $APP_NAME.service

echo "Installation completed. You can now manage the service using:"
echo "systemctl {start|stop|restart|status} $APP_NAME"
echo "Or use the service script directly:"
echo "$BASE_DIR/service.sh {start|stop|restart|status|logs|clean}" 