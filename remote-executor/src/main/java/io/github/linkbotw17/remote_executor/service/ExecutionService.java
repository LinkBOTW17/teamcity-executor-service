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
    private final DockerClient dockerClient;

    // Inject the configured DockerClient
    public ExecutionService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public ExecutionJob submitJob(ExecutionRequest request) {
        ExecutionJob job = new ExecutionJob(request);
        jobs.put(job.getId(), job); // Save it to our map

        // Trigger the background execution
        executeInDocker(job);

        return job;
    }

    public ExecutionJob getJob(String id) {
        return jobs.get(id); // Returns the job, or null if the ID doesn't exist
    }

    @Async // Runs in a separate background thread
    protected void executeInDocker(ExecutionJob job) {
        try {
            // 1. Wait for initialization (Status: IN_PROGRESS)
            job.setStatus(ExecutionStatus.IN_PROGRESS);

            // Docker expects CPU limits in "NanoCPUs" (1 CPU = 1,000,000,000 NanoCPUs)
            long nanoCpus = (long) (job.getRequest().cpuCount() * 1_000_000_000L);

            // 2. Start a new executor (Create the container)
            CreateContainerResponse container = dockerClient.createContainerCmd("alpine:latest")
                    .withCmd("sh", "-c", job.getRequest().script())
                    .withHostConfig(HostConfig.newHostConfig().withNanoCPUs(nanoCpus))
                    .exec();

            // 3. Execute the command
            dockerClient.startContainerCmd(container.getId()).exec();

            // Wait for the container to finish running the script
            dockerClient.waitContainerCmd(container.getId())
                    .start()
                    .awaitCompletion();

            // Optional cleanup: remove the container after it's done so your hard drive doesn't fill up
            dockerClient.removeContainerCmd(container.getId()).exec();

        } catch (Exception e) {
            System.err.println("Failed to execute job " + job.getId() + ": " + e.getMessage());
        } finally {
            // 4. Update the execution status (Status: FINISHED)
            job.setStatus(ExecutionStatus.FINISHED);
        }
    }
}
