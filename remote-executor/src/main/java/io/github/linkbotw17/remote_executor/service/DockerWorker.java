package io.github.linkbotw17.remote_executor.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import io.github.linkbotw17.remote_executor.model.ExecutionJob;
import io.github.linkbotw17.remote_executor.model.ExecutionStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class DockerWorker {
    private final DockerClient dockerClient;

    public DockerWorker(DockerClient dockerClient){
        this.dockerClient = dockerClient;
    }

    @Async
    public void executeInDocker(ExecutionJob job) {
        try {
            job.setStatus(ExecutionStatus.IN_PROGRESS);

            // Calculate CPU limits
            long nanoCpus = (long) (job.getRequest().cpuCount() * 1_000_000_000L);

            // Calculate Memory limits (convert MB to Bytes). Default to 512MB if null.
            long memoryBytes = job.getRequest().memoryMb() != null
                    ? job.getRequest().memoryMb() * 1024 * 1024
                    : 512L * 1024 * 1024;

            // Apply limits to the container
            CreateContainerResponse container = dockerClient.createContainerCmd("alpine:latest")
                    .withCmd("sh", "-c", job.getRequest().script())
                    .withHostConfig(HostConfig.newHostConfig().withNanoCPUs(nanoCpus))
                    .withMemory(memoryBytes)
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            // Wait for the script to finish (or crash)
            dockerClient.waitContainerCmd(container.getId())
                    .start()
                    .awaitCompletion();

            // Inspect the container to see if it crashed or was killed by limits
            Long exitCode = dockerClient.inspectContainerCmd(container.getId())
                    .exec()
                    .getState()
                    .getExitCodeLong();

            dockerClient.removeContainerCmd(container.getId()).exec();

            // If the exit code isn't 0 (success), throw an error so the FAILED status triggers
            if (exitCode != null && exitCode != 0) {
                throw new RuntimeException("Container failed or exceeded resources. Exit code: " + exitCode);
            }

        } catch (Exception e) {
            System.err.println("Failed to execute job " + job.getId() + ": " + e.getMessage());
            job.setStatus(ExecutionStatus.FAILED);
        } finally {
            // Only mark as FINISHED if the job didn't fail
            if (job.getStatus() != ExecutionStatus.FAILED) {
                job.setStatus(ExecutionStatus.FINISHED);
            }
        }
    }
}
