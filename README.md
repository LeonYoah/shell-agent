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

## 环境要求

- JDK 8+
- Maven 3.6+
- Nacos 2.x
- Spring Boot 2.3.12.RELEASE
- Dubbo 2.7.15

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

### 2. 启动服务

```bash
# 开发环境
mvn spring-boot:run -Pdev

# 测试环境
mvn spring-boot:run -Ptest

# 生产环境
mvn spring-boot:run -Pprod
```

## 部署说明

### 1. 服务器要求

- JDK 8+
- 至少512MB可用内存
- 建议使用Linux系统
- 需要访问Nacos服务器

### 2. 部署步骤

1. 准备部署环境：
```bash
# 创建部署目录
mkdir -p /opt/apps/shell-executor
mkdir -p /opt/apps/shell-executor/logs

# 设置权限
chmod +x deploy/*.sh
```

2. 复制文件到服务器：
```bash
# 复制JAR包和脚本
scp target/shell-executor-1.0.0.jar user@server:/opt/apps/shell-executor/
scp deploy/*.sh user@server:/opt/apps/shell-executor/
```

3. 部署和启动：
```bash
cd /opt/apps/shell-executor

# 部署新版本
./deploy.sh

# 或者使用服务管理脚本
./service.sh start   # 启动服务
./service.sh stop    # 停止服务
./service.sh restart # 重启服务
./service.sh status  # 查看状态
./service.sh logs    # 查看日志
```

### 3. 配置说明

1. JVM配置（在deploy/service.sh中修改）：
```bash
JAVA_OPTS="-server -Xms512m -Xmx512m -Xmn256m"
```

2. 环境配置：
```bash
PROFILES_ACTIVE="prod"  # 可选: dev, test, prod
```

3. 目录结构：
```
/opt/apps/shell-executor/
├── shell-executor-1.0.0.jar
├── deploy.sh
├── service.sh
└── logs/
    ├── startup.log
    └── ...
```

### 4. 日志查看

- 启动日志：`/opt/apps/shell-executor/logs/startup.log`
- 应用日志：`/opt/apps/shell-executor/logs/shell-executor.log`

### 5. 健康检查

1. HTTP接口检查：
```bash
curl http://localhost:8080/actuator/health
```

2. 进程检查：
```bash
ps -ef | grep shell-executor
```

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

### 3. 注入并使用服务

```java
@DubboReference(version = "1.0.0")
private ShellExecutorService shellExecutorService;

// 同步执行
public void executeSync() {
    ExecuteResult result = shellExecutorService.executeCommand("echo hello");
    System.out.println("输出: " + result.getOutput());
}

// 异步执行
public void executeAsync() {
    String executionId = shellExecutorService.executeCommandAsync(
        new ShellExecutionRequest("ping localhost")
    );
    
    // 获取执行结果
    ShellExecutionOutput output = shellExecutorService.getOutput(executionId);
    System.out.println("状态: " + output.getStatus());
    output.getOutputLines().forEach(System.out::println);
}
```

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