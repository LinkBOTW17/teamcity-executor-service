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
            long nanoCpus = (long) (job.getRequest().cpuCount() * 1_000_000_000L);

            CreateContainerResponse container = dockerClient.createContainerCmd("alpine:latest")
                    .withCmd("sh", "-c", job.getRequest().script())
                    .withHostConfig(HostConfig.newHostConfig().withNanoCPUs(nanoCpus))
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            dockerClient.waitContainerCmd(container.getId())
                    .start()
                    .awaitCompletion();

            dockerClient.removeContainerCmd(container.getId()).exec();

        } catch (Exception e) {
            System.err.println("Failed to execute job " + job.getId() + ": " + e.getMessage());
        } finally {
            job.setStatus(ExecutionStatus.FINISHED);
        }
    }
}
