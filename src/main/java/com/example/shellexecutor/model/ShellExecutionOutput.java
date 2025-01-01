package com.example.shellexecutor.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ShellExecutionOutput {
    private String executionId;
    private String command;
    private List<String> outputLines;
    private List<String> errorLines;
    private Boolean finished;
    private Integer exitCode;
    
    // 新增字段
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, TIMEOUT
    private Long executionTimeMs;
    private String errorMessage;
} 