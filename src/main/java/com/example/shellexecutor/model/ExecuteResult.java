package com.example.shellexecutor.model;

import lombok.Data;

@Data
public class ExecuteResult {
    private int exitCode;
    private String output;
    private String error;
    private boolean success;
    
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
        this.success = exitCode == 0;
    }
} 