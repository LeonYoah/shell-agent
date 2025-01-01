package com.example.shellexecutor.service;

import com.example.shellexecutor.config.ShellExecutorConfig;
import com.example.shellexecutor.model.ExecuteResult;
import com.example.shellexecutor.model.ShellExecutionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.*;

@Slf4j
@Service
public class ShellService {
    
    @Autowired
    private ShellExecutionManager executionManager;
    
    @Autowired
    private ShellExecutorConfig config;
    
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final Charset CHARSET = IS_WINDOWS ? Charset.forName("GBK") : Charset.forName("UTF-8");
    
    private String[] buildCommand(String command) {
        if (IS_WINDOWS) {
            return new String[]{"cmd", "/c", command};
        } else {
            return new String[]{"/bin/sh", "-c", command};
        }
    }

    private String readInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, CHARSET))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }
    
    public ExecuteResult executeCommand(String command) {
        ExecuteResult result = new ExecuteResult();
        
        log.info("开始执行命令: {}", command);
        String[] cmdArray = buildCommand(command);
        log.info("实际执行的命令数组: {}", Arrays.toString(cmdArray));
        
        if (config.isCommandBlocked(command)) {
            log.warn("命令被禁止执行: {}", command);
            result.setExitCode(-1);
            result.setError("命令被禁止执行");
            return result;
        }
        
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmdArray);
            Process process = processBuilder.start();
            boolean completed = process.waitFor(config.getCommandTimeoutMs(), TimeUnit.MILLISECONDS);
            
            if (!completed) {
                log.warn("命令执行超时 {} 毫秒: {}", config.getCommandTimeoutMs(), command);
                process.destroyForcibly();
                result.setExitCode(-1);
                result.setError("命令执行超时");
                return result;
            }
            
            result.setExitCode(process.exitValue());
            result.setOutput(readInputStream(process.getInputStream()));
            result.setError(readInputStream(process.getErrorStream()));
            
            log.info("命令执行完成，退出码 {}: {}", result.getExitCode(), command);
            if (!result.getError().isEmpty()) {
                log.warn("命令执行出现错误输出: {}", result.getError());
            }
        } catch (Exception e) {
            log.error("命令执行失败: {} - {}", command, e.getMessage(), e);
            result.setExitCode(1);
            result.setError(e.getMessage());
        }
        return result;
    }
    
    public String executeCommandAsync(ShellExecutionRequest request) {
        String command = request.getCommand();
        log.info("开始异步执行命令: {}", command);
        String[] cmdArray = buildCommand(command);
        log.info("实际执行的异步命令数组: {}", Arrays.toString(cmdArray));
        
        if (config.isCommandBlocked(command)) {
            log.warn("异步命令被禁止执行: {}", command);
            String executionId = executionManager.createExecution(command);
            executionManager.setError(executionId, "命令被禁止执行");
            return executionId;
        }
        
        String executionId = request.getExecutionId();
        if (executionId == null || executionId.isEmpty()) {
            executionId = executionManager.createExecution(command);
        }
        
        final String finalExecutionId = executionId;
        log.info("异步命令开始执行，执行ID {}: {}", executionId, command);
        
        CompletableFuture.runAsync(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(cmdArray);
                Process process = processBuilder.start();
                
                // 启动输出流读取线程
                startOutputReader(process.getInputStream(), finalExecutionId, false);
                startOutputReader(process.getErrorStream(), finalExecutionId, true);
                
                // 等待进程完成或超时
                boolean completed = process.waitFor(config.getCommandTimeoutMs(), TimeUnit.MILLISECONDS);
                
                if (!completed) {
                    log.warn("异步命令执行超时 {} 毫秒: {}", config.getCommandTimeoutMs(), command);
                    process.destroyForcibly();
                    executionManager.setTimeout(finalExecutionId);
                } else {
                    int exitCode = process.exitValue();
                    log.info("异步命令执行完成，退出码 {}: {}", exitCode, command);
                    executionManager.setFinished(finalExecutionId, exitCode);
                }
                
            } catch (Exception e) {
                log.error("异步命令执行失败: {} - {}", command, e.getMessage(), e);
                executionManager.setError(finalExecutionId, e.getMessage());
            }
        });
        
        return executionId;
    }
    
    private void startOutputReader(InputStream inputStream, String executionId, boolean isError) {
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, CHARSET))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isError) {
                        log.debug("命令错误输出 [执行ID:{}]: {}", executionId, line);
                        executionManager.appendError(executionId, line);
                    } else {
                        log.debug("命令标准输出 [执行ID:{}]: {}", executionId, line);
                        executionManager.appendOutput(executionId, line);
                    }
                }
            } catch (IOException e) {
                log.error("读取命令输出失败: {}", e.getMessage(), e);
            }
        });
    }
} 