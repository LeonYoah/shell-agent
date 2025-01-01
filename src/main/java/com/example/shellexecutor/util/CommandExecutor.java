package com.example.shellexecutor.util;

import com.example.shellexecutor.model.ExecuteResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Component
public class CommandExecutor {
    
    public ExecuteResult executeCommand(String command, long timeout) {
        log.info("开始执行命令: {}", command);
        ExecuteResult result = new ExecuteResult();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        
        CommandLine commandLine = CommandLine.parse(command);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(outputStream, errorStream));
        
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
        executor.setWatchdog(watchdog);
        
        try {
            log.debug("命令执行中...");
            int exitCode = executor.execute(commandLine);
            String output = outputStream.toString("UTF-8");
            String error = errorStream.toString("UTF-8");
            
            result.setExitCode(exitCode);
            result.setOutput(output);
            result.setError(error);
            
            log.info("命令执行完成, 退出码: {}", exitCode);
            log.debug("命令输出: {}", output);
            if (!error.isEmpty()) {
                log.warn("命令错误输出: {}", error);
            }
        } catch (IOException e) {
            log.error("命令执行失败: {}", e.getMessage(), e);
            result.setExitCode(-1);
            result.setError(e.getMessage());
        }
        
        return result;
    }
} 