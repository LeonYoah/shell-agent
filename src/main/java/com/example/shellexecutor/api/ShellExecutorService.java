package com.example.shellexecutor.api;

import com.example.shellexecutor.model.ExecuteResult;
import com.example.shellexecutor.model.ShellExecutionOutput;
import com.example.shellexecutor.model.ShellExecutionRequest;

public interface ShellExecutorService {
    
    /**
     * 同步执行shell命令
     */
    ExecuteResult executeCommand(String command);
    
    /**
     * 异步执行shell命令
     */
    String executeCommandAsync(ShellExecutionRequest request);
    
    /**
     * 获取执行输出
     */
    ShellExecutionOutput getOutput(String executionId);
} 