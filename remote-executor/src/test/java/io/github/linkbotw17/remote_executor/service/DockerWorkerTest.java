package io.github.linkbotw17.remote_executor.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.model.HostConfig;
import io.github.linkbotw17.remote_executor.model.ExecutionJob;
import io.github.linkbotw17.remote_executor.model.ExecutionRequest;
import io.github.linkbotw17.remote_executor.model.ExecutionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class DockerWorkerTest {

    private DockerWorker dockerWorker;
    private DockerClient mockDockerClient;

    @BeforeEach
    void setUp() {
        // Deep stubs allow us to mock chained methods like .withCmd().exec()
        mockDockerClient = Mockito.mock(DockerClient.class, Mockito.RETURNS_DEEP_STUBS);
        dockerWorker = new DockerWorker(mockDockerClient);
    }

    @Test
    void executeInDocker_ShouldRunContainerAndUpdateStatusToFinished() {
        // Arrange
        ExecutionRequest request = new ExecutionRequest("echo 'test'", 1.0);
        ExecutionJob job = new ExecutionJob(request);

        // Mock the DockerClient chain responses
        CreateContainerResponse mockResponse = new CreateContainerResponse();
        mockResponse.setId("mock-container-id");

        Mockito.when(mockDockerClient.createContainerCmd(anyString())
                .withCmd(anyString(), anyString(), anyString())
                .withHostConfig(any(HostConfig.class))
                .exec()).thenReturn(mockResponse);

        // Act
        dockerWorker.executeInDocker(job);

        // Assert
        // Since it's synchronous in the test, it should end up as FINISHED
        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.FINISHED);

        // Verify Docker commands were actually called
        Mockito.verify(mockDockerClient).createContainerCmd("alpine:latest");
        Mockito.verify(mockDockerClient).startContainerCmd("mock-container-id");
        Mockito.verify(mockDockerClient).waitContainerCmd("mock-container-id");
        Mockito.verify(mockDockerClient).removeContainerCmd("mock-container-id");
    }
}