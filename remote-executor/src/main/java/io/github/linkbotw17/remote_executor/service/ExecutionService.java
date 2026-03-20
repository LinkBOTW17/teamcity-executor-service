package io.github.linkbotw17.remote_executor.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import io.github.linkbotw17.remote_executor.model.ExecutionJob;
import io.github.linkbotw17.remote_executor.model.ExecutionRequest;
import io.github.linkbotw17.remote_executor.model.ExecutionStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExecutionService {
    // An in-memory database to store jobs by their ID
    private final Map<String, ExecutionJob> jobs = new ConcurrentHashMap<>();
    private final DockerWorker dockerWorker;

    // Inject the configured DockerClient
    public ExecutionService(DockerWorker dockerWorker) {
        this.dockerWorker = dockerWorker;
    }

    public ExecutionJob submitJob(ExecutionRequest request) {
        ExecutionJob job = new ExecutionJob(request);
        jobs.put(job.getId(), job); // Save it to our map

        // Trigger the background execution
        dockerWorker.executeInDocker(job);

        return job;
    }

    public ExecutionJob getJob(String id) {
        return jobs.get(id); // Returns the job, or null if the ID doesn't exist
    }
}
