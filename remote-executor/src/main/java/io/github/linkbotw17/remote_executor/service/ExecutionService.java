package io.github.linkbotw17.remote_executor.service;

import io.github.linkbotw17.remote_executor.model.ExecutionJob;
import io.github.linkbotw17.remote_executor.model.ExecutionRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExecutionService {
    // An in-memory database to store jobs by their ID
    private final Map<String, ExecutionJob> jobs = new ConcurrentHashMap<>();

    public ExecutionJob submitJob(ExecutionRequest request) {
        ExecutionJob job = new ExecutionJob(request);
        jobs.put(job.getId(), job); // Save it to our map

        // TODO: trigger the Docker execution

        return job;
    }

    public ExecutionJob getJob(String id) {
        return jobs.get(id); // Returns the job, or null if the ID doesn't exist
    }
}
