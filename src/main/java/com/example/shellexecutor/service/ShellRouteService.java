package com.example.shellexecutor.service;

import com.example.shellexecutor.api.ShellExecutorService;
import com.example.shellexecutor.model.ExecuteResult;
import com.example.shellexecutor.model.ShellExecutionOutput;
import com.example.shellexecutor.model.ShellExecutionRequest;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ShellRouteService {
    
    @DubboReference(version = "1.0.0", check = false)
    private ShellExecutorService shellExecutorService;
    
    /**
     * 构建Dubbo URL
     */
    private URL buildDubboUrl(String targetHost, Integer targetPort) {
        return URL.valueOf("dubbo://" + targetHost + ":" + targetPort + "/" + ShellExecutorService.class.getName())
            .addParameter("version", "1.0.0")
            .addParameter("timeout", "60000")
            .addParameter("retries", "0")
            .addParameter("connections", "1")
            .addParameter("check", "false");
    }
    
    /**
     * 在指定机器上执行命令
     */
    public ExecuteResult executeCommand(String command, String targetHost, Integer targetPort) {
        RpcContext.getContext().setUrl(buildDubboUrl(targetHost, targetPort));
        return shellExecutorService.executeCommand(command);
    }
    
    /**
     * 在指定机器上异步执行命令
     */
    public String executeCommandAsync(ShellExecutionRequest request) {
        if (request.getTargetHost() != null && request.getTargetPort() != null) {
            RpcContext.getContext().setUrl(buildDubboUrl(request.getTargetHost(), request.getTargetPort()));
        }
        return shellExecutorService.executeCommandAsync(request);
    }
    
    /**
     * 从指定机器获取执行输出
     */
    public ShellExecutionOutput getOutput(String executionId, String targetHost, Integer targetPort) {
        RpcContext.getContext().setUrl(buildDubboUrl(targetHost, targetPort));
        return shellExecutorService.getOutput(executionId);
    }
    
    /**
     * 获取所有可用的shell执行器节点
     */
    public Map<String, String> getAvailableNodes() {
        Map<String, String> nodes = new HashMap<>();
        // 这里需要通过Dubbo的元数据服务获取提供者信息
        // TODO: 实现获取提供者列表的逻辑
        return nodes;
    }
} 