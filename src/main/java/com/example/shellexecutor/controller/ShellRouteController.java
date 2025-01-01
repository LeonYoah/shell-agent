package com.example.shellexecutor.controller;

import com.example.shellexecutor.model.ExecuteResult;
import com.example.shellexecutor.model.ShellExecutionOutput;
import com.example.shellexecutor.model.ShellExecutionRequest;
import com.example.shellexecutor.service.ShellRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shell/route")
public class ShellRouteController {
    
    @Autowired
    private ShellRouteService shellRouteService;
    
    @PostMapping("/execute")
    public ExecuteResult executeCommand(@RequestParam String command,
                                      @RequestParam String targetHost,
                                      @RequestParam Integer targetPort) {
        return shellRouteService.executeCommand(command, targetHost, targetPort);
    }
    
    @PostMapping("/execute/async")
    public String executeCommandAsync(@RequestBody ShellExecutionRequest request) {
        return shellRouteService.executeCommandAsync(request);
    }
    
    @GetMapping("/output/{executionId}")
    public ShellExecutionOutput getOutput(@PathVariable String executionId,
                                        @RequestParam String targetHost,
                                        @RequestParam Integer targetPort) {
        return shellRouteService.getOutput(executionId, targetHost, targetPort);
    }
    
    @GetMapping("/nodes")
    public Map<String, String> getAvailableNodes() {
        return shellRouteService.getAvailableNodes();
    }
} 