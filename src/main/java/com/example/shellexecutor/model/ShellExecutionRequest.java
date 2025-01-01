package com.example.shellexecutor.model;

import lombok.Data;

@Data
public class ShellExecutionRequest {
    private String command;
    private String executionId;  // 可以为空,由服务端生成
    private String targetHost;   // 目标机器IP
    private Integer targetPort;  // 目标机器端口
} 