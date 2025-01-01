package com.example.shellexecutor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "shell.executor")
public class ShellExecutorConfig {
    
    // 命令黑名单
    private List<String> blockedCommands = new ArrayList<>();
    
    // 危险命令关键字
    private List<String> dangerousKeywords = new ArrayList<>(Arrays.asList(
        "rm -rf", "format", "mkfs", "dd if=", "fdisk",  // 文件系统危险命令
        ":(){:|:&};:", "fork bomb",                     // fork炸弹
        "chmod 777", "chmod -R 777",                    // 危险权限修改
        "> /dev/", "dd of=/dev/",                       // 设备操作
        "mv /* ", "rm /* ",                             // 根目录操作
        "> /etc/passwd", "> /etc/shadow",               // 系统文件修改
        "shutdown", "reboot", "init 0", "init 6"        // 系统控制
    ));
    
    // 命令执行超时时间(毫秒)
    private long commandTimeoutMs = 60000;
    
    // 输出缓存过期时间(毫秒)
    private long outputExpirationMs = 1800000; // 30分钟
    
    // 清理任务执行间隔(毫秒)
    private long cleanupIntervalMs = 300000; // 5分钟
    
    /**
     * 检查命令是否被禁止
     * @param command 要执行的命令
     * @return true如果命令被禁止，false如果命令允许执行
     */
    public boolean isCommandBlocked(String command) {
        if (command == null || command.trim().isEmpty()) {
            return true;
        }
        
        String normalizedCommand = command.toLowerCase().trim();
        
        // 检查完全匹配的黑名单命令
        for (String blockedCmd : blockedCommands) {
            if (normalizedCommand.equals(blockedCmd.toLowerCase())) {
                return true;
            }
        }
        
        // 检查危险关键字
        for (String keyword : dangerousKeywords) {
            if (normalizedCommand.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
} 