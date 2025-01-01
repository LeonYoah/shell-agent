package com.example.shellexecutor.service;

import com.example.shellexecutor.config.ShellExecutorConfig;
import com.example.shellexecutor.model.ShellExecutionOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class ShellExecutionManager {
    private final Map<String, ShellExecutionOutput> executionOutputs = new ConcurrentHashMap<>();
    
    @Autowired
    private ShellExecutorConfig config;
    
    public String createExecution(String command) {
        String executionId = UUID.randomUUID().toString();
        ShellExecutionOutput output = new ShellExecutionOutput();
        output.setExecutionId(executionId);
        output.setCommand(command);
        output.setOutputLines(new ArrayList<>());
        output.setErrorLines(new ArrayList<>());
        output.setFinished(false);
        output.setStartTime(LocalDateTime.now());
        output.setStatus("RUNNING");
        executionOutputs.put(executionId, output);
        return executionId;
    }
    
    public void appendOutput(String executionId, String line) {
        ShellExecutionOutput output = executionOutputs.get(executionId);
        if (output != null) {
            output.getOutputLines().add(line);
        }
    }
    
    public void appendError(String executionId, String line) {
        ShellExecutionOutput output = executionOutputs.get(executionId);
        if (output != null) {
            output.getErrorLines().add(line);
        }
    }
    
    public void setFinished(String executionId, int exitCode) {
        ShellExecutionOutput output = executionOutputs.get(executionId);
        if (output != null) {
            output.setFinished(true);
            output.setExitCode(exitCode);
            output.setEndTime(LocalDateTime.now());
            output.setExecutionTimeMs(ChronoUnit.MILLIS.between(output.getStartTime(), output.getEndTime()));
            output.setStatus(exitCode == 0 ? "COMPLETED" : "FAILED");
        }
    }
    
    public void setError(String executionId, String errorMessage) {
        ShellExecutionOutput output = executionOutputs.get(executionId);
        if (output != null) {
            output.setFinished(true);
            output.setExitCode(1);
            output.setEndTime(LocalDateTime.now());
            output.setExecutionTimeMs(ChronoUnit.MILLIS.between(output.getStartTime(), output.getEndTime()));
            output.setStatus("FAILED");
            output.setErrorMessage(errorMessage);
        }
    }
    
    public void setTimeout(String executionId) {
        ShellExecutionOutput output = executionOutputs.get(executionId);
        if (output != null) {
            output.setFinished(true);
            output.setExitCode(-1);
            output.setEndTime(LocalDateTime.now());
            output.setExecutionTimeMs(ChronoUnit.MILLIS.between(output.getStartTime(), output.getEndTime()));
            output.setStatus("TIMEOUT");
            output.setErrorMessage("Command execution timed out");
        }
    }
    
    public ShellExecutionOutput getOutput(String executionId) {
        return executionOutputs.get(executionId);
    }
    
    @Scheduled(fixedDelayString = "${shell.executor.cleanupIntervalMs}")
    public void cleanupExpiredOutputs() {
        LocalDateTime expirationTime = LocalDateTime.now().minus(config.getOutputExpirationMs(), ChronoUnit.MILLIS);
        executionOutputs.entrySet().removeIf(entry -> {
            ShellExecutionOutput output = entry.getValue();
            return Boolean.TRUE.equals(output.getFinished()) && output.getEndTime() != null && output.getEndTime().isBefore(expirationTime);
        });
    }
} 