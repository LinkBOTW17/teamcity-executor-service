package io.github.linkbotw17.remote_executor.model;

public record ExecutionRequest(
        String script,
        double cpuCount // Using double because Docker allows fractional CPUs (e.g., 0.5)
) { }