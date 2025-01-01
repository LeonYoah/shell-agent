package com.example.shellexecutor.service;

import com.example.shellexecutor.config.ShellExecutorConfig;
import com.example.shellexecutor.model.ExecuteResult;
import com.example.shellexecutor.model.ShellExecutionOutput;
import com.example.shellexecutor.model.ShellExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Arrays;
import java.util.ArrayList;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
public class ShellServiceTest {

    @Mock
    private ShellExecutionManager executionManager;

    @Mock
    private ShellExecutorConfig config;

    @InjectMocks
    private ShellService shellService;

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        config.setBlockedCommands(Arrays.asList(
            "rm -rf /",
            "format c:",
            "shutdown"
        ));
        when(config.getCommandTimeoutMs()).thenReturn(5000L);
        log.info("操作系统类型: {}", IS_WINDOWS ? "Windows" : "Linux/Unix");
    }

    @Test
    void testExecuteCommand_Blocked() {
        when(config.isCommandBlocked(anyString())).thenReturn(true);
        
        log.info("测试执行被禁止的命令: rm -rf /");
        ExecuteResult result = shellService.executeCommand("rm -rf /");
        
        log.info("命令执行结果 - 退出码: {}, 错误信息: {}", result.getExitCode(), result.getError());
        assertEquals(-1, result.getExitCode());
        assertEquals("命令被禁止执行", result.getError());
        verify(config).isCommandBlocked("rm -rf /");
    }

    @Test
    void testExecuteCommand_Success() {
        when(config.isCommandBlocked(anyString())).thenReturn(false);
        
        log.info("测试执行echo命令");
        ExecuteResult result = shellService.executeCommand("echo hello");
        
        log.info("命令执行结果:");
        log.info("退出码: {}", result.getExitCode());
        log.info("标准输出: {}", result.getOutput());
        log.info("错误输出: {}", result.getError());
        
        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutput().contains("hello"));
        verify(config).isCommandBlocked("echo hello");
    }

    @Test
    void testExecuteCommand_Error() {
        when(config.isCommandBlocked(anyString())).thenReturn(false);
        
        log.info("测试执行不存在的命令");
        ExecuteResult result = shellService.executeCommand("nonexistentcommand");
        
        log.info("命令执行结果:");
        log.info("退出码: {}", result.getExitCode());
        log.info("标准输出: {}", result.getOutput());
        log.info("错误输出: {}", result.getError());
        
        assertNotNull(result);
        assertNotEquals(0, result.getExitCode());
        assertFalse(result.getError().isEmpty());
    }

    @Test
    void testExecuteCommandAsync_Blocked() {
        when(config.isCommandBlocked(anyString())).thenReturn(true);
        String executionId = "test-id";
        when(executionManager.createExecution(anyString())).thenReturn(executionId);
        
        log.info("测试异步执行被禁止的命令");
        ShellExecutionRequest request = new ShellExecutionRequest();
        request.setCommand("rm -rf /");
        
        String resultId = shellService.executeCommandAsync(request);
        
        log.info("异步执行ID: {}", resultId);
        assertEquals(executionId, resultId);
        verify(executionManager).createExecution("rm -rf /");
        verify(executionManager).setError(executionId, "命令被禁止执行");
    }

    @Test
    void testExecuteCommand_SystemCommands() {
        when(config.isCommandBlocked(anyString())).thenReturn(false);
        
        // 测试网络命令
        String netCmd = IS_WINDOWS ? "ipconfig" : "ifconfig";
        log.info("测试网络命令: {}", netCmd);
        ExecuteResult netResult = shellService.executeCommand(netCmd);
        
        log.info("网络命令执行结果:");
        log.info("退出码: {}", netResult.getExitCode());
        log.info("标准输出:\n{}", netResult.getOutput());
        if (!netResult.getError().isEmpty()) {
            log.info("错误输出:\n{}", netResult.getError());
        }
        
        assertEquals(0, netResult.getExitCode());
        assertFalse(netResult.getOutput().isEmpty());
        
        // 测试目录命令
        String dirCmd = IS_WINDOWS ? "dir" : "ls";
        log.info("\n测试目录命令: {}", dirCmd);
        ExecuteResult dirResult = shellService.executeCommand(dirCmd);
        
        log.info("目录命令执行结果:");
        log.info("退出码: {}", dirResult.getExitCode());
        log.info("标准输出:\n{}", dirResult.getOutput());
        if (!dirResult.getError().isEmpty()) {
            log.info("错误输出:\n{}", dirResult.getError());
        }
        
        assertEquals(0, dirResult.getExitCode());
        assertFalse(dirResult.getOutput().isEmpty());
    }

    @Test
    void testExecuteCommand_OutputEncoding() {
        when(config.isCommandBlocked(anyString())).thenReturn(false);
        
        log.info("测试中文输出命令");
        String command = IS_WINDOWS ? "echo 你好，世界" : "echo '你好，世界'";
        ExecuteResult result = shellService.executeCommand(command);
        
        log.info("命令执行结果:");
        log.info("退出码: {}", result.getExitCode());
        log.info("标准输出原始内容: [{}]", result.getOutput());
        log.info("错误输出: {}", result.getError());
        
        assertEquals(0, result.getExitCode());
        String output = result.getOutput().trim();
        log.info("输出内容字节: {}", Arrays.toString(output.getBytes(StandardCharsets.UTF_8)));
        assertTrue(output.contains("你好") || output.contains("你好，世界"), 
            String.format("输出 [%s] 应该包含中文", output));
    }

    @Test
    void testExecuteCommandAsync_OutputCollection() throws InterruptedException {
        when(config.isCommandBlocked(anyString())).thenReturn(false);
        String executionId = "test-id";
        when(executionManager.createExecution(anyString())).thenReturn(executionId);
        
        ShellExecutionOutput mockOutput = new ShellExecutionOutput();
        mockOutput.setExecutionId(executionId);
        mockOutput.setCommand("echo test");
        mockOutput.setOutputLines(new ArrayList<>());
        mockOutput.setErrorLines(new ArrayList<>());
        mockOutput.setStartTime(LocalDateTime.now());
        when(executionManager.getOutput(executionId)).thenReturn(mockOutput);
        
        log.info("测试异步命令输出收集");
        ShellExecutionRequest request = new ShellExecutionRequest();
        request.setCommand("echo test");
        
        String resultId = shellService.executeCommandAsync(request);
        log.info("异步执行ID: {}", resultId);
        
        // 等待一段时间让异步命令执行完成
        Thread.sleep(1000);
        
        ShellExecutionOutput output = executionManager.getOutput(resultId);
        if (output != null) {
            log.info("异步命令执行状态: {}", output.getStatus());
            log.info("标准输出行数: {}", output.getOutputLines().size());
            log.info("错误输出行数: {}", output.getErrorLines().size());
            if (!output.getOutputLines().isEmpty()) {
                log.info("输出内容:");
                output.getOutputLines().forEach(line -> log.info(line));
            }
        }
        
        assertEquals(executionId, resultId);
        verify(executionManager).createExecution("echo test");
    }

    @Test
    void testExecuteCommand_LongRunning() throws InterruptedException {
        when(config.isCommandBlocked(anyString())).thenReturn(false);
        
        log.info("测试长时间运行的命令");
        String command = IS_WINDOWS ? "ping -n 3 localhost" : "ping -c 3 localhost";
        log.info("执行命令: {}", command);
        
        ExecuteResult result = shellService.executeCommand(command);
        
        log.info("命令执行结果:");
        log.info("退出码: {}", result.getExitCode());
        log.info("标准输出:\n{}", result.getOutput());
        if (!result.getError().isEmpty()) {
            log.info("错误输出:\n{}", result.getError());
        }
        
        assertEquals(0, result.getExitCode());
        assertFalse(result.getOutput().isEmpty());
    }
} 