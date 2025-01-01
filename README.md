# Shell Executor Service

Shell命令执行服务，支持同步/异步执行Shell命令，并通过HTTP和Dubbo方式调用。

## 功能特性

- 支持同步和异步执行Shell命令
- 支持Windows和Linux系统
- 命令黑名单机制，防止危险命令执行
- 命令执行超时控制
- 输出自动清理机制
- 分布式部署支持（通过Dubbo+Nacos）
- 中文编码自动处理（Windows使用GBK，Linux使用UTF-8）
- 支持系统服务方式运行
- 支持开机自启动
- 自动服务重启机制

## 环境要求

- JDK 8+
- Maven 3.6+
- Nacos 2.x
- Spring Boot 2.3.12.RELEASE
- Dubbo 2.7.15
- systemd (Linux系统服务管理)

## 快速开始

### 1. 配置Nacos

1. 启动Nacos服务器
2. 在Nacos中创建配置：
   - Data ID: `shell-executor.yml`
   - Group: `DEFAULT_GROUP`
   - 配置格式: `YAML`

配置内容示例：
```yaml
server:
  port: 8080

shell:
  executor:
    blocked-commands:
      - "rm -rf /"
      - "format c:"
      - "del /f /s /q"
      - "shutdown"
      - "reboot"
    command-timeout-ms: 60000
    output-expiration-ms: 1800000
    cleanup-interval-ms: 300000
```

### 2. 部署方式

#### 2.1 脚本部署

```bash
# 创建部署目录
mkdir -p /opt/apps/shell-executor
mkdir -p /opt/apps/shell-executor/logs

# 设置权限
chmod +x deploy/*.sh

# 复制文件到服务器
scp target/shell-executor-1.0.0.jar user@server:/opt/apps/shell-executor/
scp deploy/*.sh user@server:/opt/apps/shell-executor/

# 部署新版本
./deploy.sh
```

#### 2.2 系统服务方式部署

1. 安装服务：
```bash
# 复制部署文件
sudo cp deploy/* /opt/apps/shell-executor/

# 运行安装脚本
sudo ./install.sh
```

2. 服务管理：
```bash
# 使用systemctl管理服务
sudo systemctl start shell-executor    # 启动服务
sudo systemctl stop shell-executor     # 停止服务
sudo systemctl restart shell-executor  # 重启服务
sudo systemctl status shell-executor   # 查看状态
sudo systemctl enable shell-executor   # 启用开机自启动
sudo systemctl disable shell-executor  # 禁用开机自启动

# 或使用服务脚本
sudo /opt/apps/shell-executor/service.sh start    # 启动服务
sudo /opt/apps/shell-executor/service.sh stop     # 停止服务
sudo /opt/apps/shell-executor/service.sh restart  # 重启服务
sudo /opt/apps/shell-executor/service.sh status   # 查看状态
sudo /opt/apps/shell-executor/service.sh logs     # 查看日志
sudo /opt/apps/shell-executor/service.sh clean    # 清理日志
```

### 3. 服务特性

#### 3.1 自动重启机制
- 服务异常退出时自动重启
- 配置10秒的重启延迟
- 使用PID文件跟踪进程状态

#### 3.2 健康检查
- 启动时等待服务就绪
- 通过actuator接口验证服务状态
- 提供详细的启动状态信息

#### 3.3 日志管理
- 自动清理7天前的日志
- 支持实时查看日志
- 优化日志输出格式

#### 3.4 服务配置
服务配置文件：`/etc/systemd/system/shell-executor.service`
```ini
[Unit]
Description=Shell Executor Service
After=network.target

[Service]
Type=forking
User=root
Group=root
Environment="JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64"
Environment="PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
Environment="PROFILES_ACTIVE=prod"
WorkingDirectory=/opt/apps/shell-executor
ExecStart=/opt/apps/shell-executor/service.sh start
ExecStop=/opt/apps/shell-executor/service.sh stop
ExecReload=/opt/apps/shell-executor/service.sh restart
PIDFile=/opt/apps/shell-executor/shell-executor.pid
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 4. 配置说明

#### 4.1 JVM配置
在`service.sh`中修改：
```bash
JAVA_OPTS="-server -Xms512m -Xmx512m -Xmn256m"
```

#### 4.2 环境配置
在`service.sh`或系统服务配置中修改：
```bash
PROFILES_ACTIVE="prod"  # 可选: dev, test, prod
```

#### 4.3 Dubbo网卡配置
1. 在`application.yml`中配置：
```yaml
dubbo:
  protocol:
    name: dubbo
    port: 20880
    host: ${DUBBO_HOST:}  # 可通过环境变量指定
    # 指定网卡
    preferred-network-interface: eth0  # 优先使用的网卡名称
    # 或者使用正则匹配网卡
    preferred-network-interface-pattern: eth.*  # 网卡名称匹配模式
```

2. 在启动脚本中指定（推荐）：
```bash
# 在service.sh中添加JVM参数
JAVA_OPTS="$JAVA_OPTS \
    -Ddubbo.protocol.host=192.168.1.100 \
    -Ddubbo.protocol.port=20880 \
    -Ddubbo.protocol.preferred-network-interface=eth0"
```

3. 使用环境变量：
在`shell-executor.service`中添加：
```ini
[Service]
Environment="DUBBO_HOST=192.168.1.100"
Environment="DUBBO_PORT=20880"
Environment="DUBBO_NETWORK_INTERFACE=eth0"
```

4. 网卡配置优先级：
   - 命令行参数 (-D) > 环境变量 > 配置文件
   - 具体IP地址 > 网卡名称 > 网卡匹配模式

5. 多网卡环境建议：
   - 明确指定host地址
   - 或指定固定的网卡名称
   - 避免使用自动探测

#### 4.4 日志配置
- 日志路径：`/opt/apps/shell-executor/logs`
- 日志文件：
  - `startup.log`: 启动日志
  - `info.log`: 信息日志
  - `error.log`: 错误日志
- 日志保留策略：自动清理7天前的日志

### 5. 注意事项

1. 系统要求：
   - 支持systemd的Linux系统
   - JDK 8或更高版本
   - 足够的磁盘空间用于日志存储

2. 权限要求：
   - 安装服务需要root权限
   - 服务运行用户需要对应目录的读写权限

3. 网络要求：
   - 确保8080端口可用
   - 需要访问Nacos服务器

4. 安全建议：
   - 定期检查日志文件
   - 及时更新JDK版本
   - 适当配置命令黑名单

5. 维护建议：
   - 定期检查服务状态
   - 监控日志输出
   - 定期清理旧的备份文件

## HTTP接口调用

### 1. 同步执行命令

```bash
POST /api/shell/execute
Content-Type: application/json

{
    "command": "echo hello"
}
```

响应示例：
```json
{
    "exitCode": 0,
    "output": "hello\n",
    "error": "",
    "success": true
}
```

### 2. 异步执行命令

```bash
POST /api/shell/execute/async
Content-Type: application/json

{
    "command": "ping localhost"
}
```

响应示例：
```json
{
    "executionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 3. 获取异步执行结果

```bash
GET /api/shell/output/{executionId}
```

响应示例：
```json
{
    "executionId": "550e8400-e29b-41d4-a716-446655440000",
    "command": "ping localhost",
    "outputLines": [
        "Pinging localhost [::1] with 32 bytes of data:",
        "Reply from ::1: time<1ms"
    ],
    "errorLines": [],
    "finished": true,
    "exitCode": 0,
    "status": "COMPLETED",
    "startTime": "2024-01-20T20:24:04.434",
    "endTime": "2024-01-20T20:24:05.528",
    "executionTimeMs": 1094
}
```

## Dubbo接口调用

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>shell-executor-api</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置Dubbo消费者

```yaml
dubbo:
  application:
    name: your-application-name
  registry:
    address: nacos://localhost:8848
    parameters:
      namespace: dev
      group: DEFAULT_GROUP
  consumer:
    timeout: 60000
    check: false
```

### 3. 指定机器执行命令

#### 3.1 通过IP和端口指定

```java
@DubboReference(version = "1.0.0")
private ShellExecutorService shellExecutorService;

public void executeOnSpecificMachine() {
    // 创建请求对象，指定目标机器
    ShellExecutionRequest request = new ShellExecutionRequest();
    request.setCommand("echo hello");
    request.setTargetHost("192.168.1.100");  // 目标机器IP
    request.setTargetPort(20880);            // 目标机器端口
    
    // 同步执行
    ExecuteResult result = shellExecutorService.executeCommand(request);
    System.out.println("输出: " + result.getOutput());
    
    // 异步执行
    String executionId = shellExecutorService.executeCommandAsync(request);
    
    // 获取执行结果（注意：获取结果时也需要指定目标机器）
    ShellExecutionOutput output = shellExecutorService.getOutput(executionId, request.getTargetHost(), request.getTargetPort());
    System.out.println("状态: " + output.getStatus());
    output.getOutputLines().forEach(System.out::println);
}
```

#### 3.2 使用RpcContext直接指定URL

```java
@DubboReference(version = "1.0.0")
private ShellExecutorService shellExecutorService;

public void executeWithRpcContext() {
    // 设置目标机器的URL
    RpcContext.getContext().setUrl("dubbo://192.168.1.100:20880");
    
    try {
        // 执行命令
        ExecuteResult result = shellExecutorService.executeCommand("echo hello");
        System.out.println("输出: " + result.getOutput());
    } finally {
        // 清除URL设置，避免影响后续调用
        RpcContext.getContext().setUrl(null);
    }
}
```

#### 3.3 获取可用节点

```java
@DubboReference(version = "1.0.0")
private ShellExecutorService shellExecutorService;

public List<String> getAvailableNodes() {
    // 获取所有可用的执行节点
    List<URL> urls = shellExecutorService.getAvailableNodes();
    
    // 转换为地址列表
    return urls.stream()
        .map(url -> url.getHost() + ":" + url.getPort())
        .collect(Collectors.toList());
}
```

#### 3.4 负载均衡策略

可以在@DubboReference注解中配置负载均衡策略：

```java
// 随机策略
@DubboReference(version = "1.0.0", loadbalance = "random")
private ShellExecutorService shellExecutorService;

// 轮询策略
@DubboReference(version = "1.0.0", loadbalance = "roundrobin")
private ShellExecutorService shellExecutorService;

// 最少活跃调用数
@DubboReference(version = "1.0.0", loadbalance = "leastactive")
private ShellExecutorService shellExecutorService;

// 一致性Hash
@DubboReference(version = "1.0.0", loadbalance = "consistenthash")
private ShellExecutorService shellExecutorService;
```

### 4. 注意事项

1. 指定机器执行时的注意点：
   - 确保目标机器的IP和端口正确
   - 检查网络连通性
   - 验证目标机器的服务是否正常运行
   - 注意清理RpcContext的URL设置

2. 异步执行注意事项：
   - 获取异步执行结果时需要指定同一台机器
   - 建议保存执行机器的信息，以便后续查询结果

3. 错误处理：
   - 目标机器不可用时会抛出RpcException
   - 网络超时需要适当配置timeout参数
   - 建议实现重试机制

4. 最佳实践：
   - 使用服务发现获取可用节点
   - 实现机器健康检查
   - 合理配置负载均衡策略
   - 保存执行记录便于追踪

## 注意事项

1. 命令执行安全
   - 服务内置了命令黑名单机制
   - 可以通过配置文件添加更多的禁止命令
   - 建议在生产环境中限制可执行的命令范围

2. 编码处理
   - Windows系统下使用GBK编码
   - Linux系统下使用UTF-8编码
   - 输出结果会自动处理编码

3. 超时控制
   - 默认命令执行超时时间为60秒
   - 可以通过配置文件修改超时时间
   - 异步执行的命令也受超时控制

4. 分布式部署
   - 支持通过Dubbo+Nacos实现分布式部署
   - 可以指定目标机器执行命令
   - 建议在相同网段部署以提高性能

## 常见问题

1. 命令执行失败
   - 检查命令是否在黑名单中
   - 检查系统是否有执行权限
   - 检查命令格式是否正确

2. 中文乱码
   - Windows系统默认使用GBK编码
   - 确保客户端使用正确的编码解析结果

3. 执行超时
   - 检查命令是否需要较长执行时间
   - 可以通过配置调整超时时间

4. 获取不到异步结果
   - 检查executionId是否正确
   - 检查命令是否已经执行完成
   - 注意输出结果的过期清理机制

## 许可证

[Apache License 2.0](LICENSE) 