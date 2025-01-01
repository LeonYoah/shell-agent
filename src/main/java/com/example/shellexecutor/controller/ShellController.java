package com.example.shellexecutor.controller;

import com.example.shellexecutor.model.ExecuteResult;
import com.example.shellexecutor.model.ShellExecutionOutput;
import com.example.shellexecutor.model.ShellExecutionRequest;
import com.example.shellexecutor.service.ShellExecutionManager;
import com.example.shellexecutor.service.ShellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shell")
public class ShellController {
    
    @Autowired
    private ShellService shellService;
    
    @Autowired
    private ShellExecutionManager executionManager;
    
    @PostMapping("/execute")
    public ExecuteResult executeCommand(@RequestBody String command) {
        return shellService.executeCommand(command);
    }
    
    @PostMapping("/execute/async")
    public String executeCommandAsync(@RequestBody ShellExecutionRequest request) {
        return shellService.executeCommandAsync(request);
    }
    
    @GetMapping("/output/{executionId}")
    public ShellExecutionOutput getOutput(@PathVariable String executionId) {
        return executionManager.getOutput(executionId);
    }
} 