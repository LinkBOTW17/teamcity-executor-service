package io.github.linkbotw17.remote_executor.service;

import com.github.dockerjava.api.DockerClient;
import io.github.linkbotw17.remote_executor.model.ExecutionJob;
import io.github.linkbotw17.remote_executor.model.ExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

class ExecutionServiceTest {

    private ExecutionService executionService;
    private DockerClient mockDockerClient;

    @BeforeEach
    void setUp() {
        // Mock the DockerClient so to avoid spinning up real containers during tests.
        // RETURNS_DEEP_STUBS prevents NullPointerExceptions when the service chains Docker methods.
        mockDockerClient = Mockito.mock(DockerClient.class, Mockito.RETURNS_DEEP_STUBS);
        executionService = new ExecutionService(mockDockerClient);
    }

    @Test
    void submitJob_ShouldCreateAndStoreJob() {
        ExecutionRequest request = new ExecutionRequest("echo 'Hello'", 1.0);

        ExecutionJob job = executionService.submitJob(request);

        assertThat(job).isNotNull();
        assertThat(job.getId()).isNotNull();
        // Verify we can retrieve it from the map using the returned ID
        assertThat(executionService.getJob(job.getId())).isEqualTo(job);
    }

    @Test
    void getJob_ShouldReturnNull_WhenIdDoesNotExist() {
        ExecutionJob job = executionService.getJob("non-existent-id");
        assertThat(job).isNull();
    }
}