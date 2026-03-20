package io.github.linkbotw17.remote_executor.model;

import java.util.UUID;

public class ExecutionJob {
    private final String id;
    private final ExecutionRequest request;
    private ExecutionStatus status;
    private String errorMessage;

    public ExecutionJob(ExecutionRequest request) {
        this.id = UUID.randomUUID().toString(); // Auto-generate a unique ID
        this.request = request;
        this.status = ExecutionStatus.QUEUED;   // Always starts as QUEUED
    }

    // Getters
    public String getId() { return id; }
    public ExecutionRequest getRequest() { return request; }
    public ExecutionStatus getStatus() { return status; }
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
