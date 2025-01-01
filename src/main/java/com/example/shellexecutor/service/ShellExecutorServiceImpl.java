package com.example.shellexecutor.service;

import com.example.shellexecutor.api.ShellExecutorService;
import com.example.shellexecutor.model.ExecuteResult;
import com.example.shellexecutor.model.ShellExecutionOutput;
import com.example.shellexecutor.model.ShellExecutionRequest;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@DubboService(version = "1.0.0")
public class ShellExecutorServiceImpl implements ShellExecutorService {
    
    @Autowired
    private ShellService shellService;
    
    @Autowired
    private ShellExecutionManager executionManager;
    
    @Override
    public ExecuteResult executeCommand(String command) {
        return shellService.executeCommand(command);
    }
    
    @Override
    public String executeCommandAsync(ShellExecutionRequest request) {
        return shellService.executeCommandAsync(request);
    }
    
    @Override
    public ShellExecutionOutput getOutput(String executionId) {
        return executionManager.getOutput(executionId);
    }
} 