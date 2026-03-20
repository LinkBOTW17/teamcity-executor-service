package io.github.linkbotw17.remote_executor.service;

import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import io.github.linkbotw17.remote_executor.model.ExecutionJob;
import io.github.linkbotw17.remote_executor.model.ExecutionRequest;
import io.github.linkbotw17.remote_executor.model.ExecutionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DockerWorkerTest {

    // Removed deep stubs from the client!
    @Mock
    private DockerClient mockDockerClient;

    @InjectMocks
    private DockerWorker dockerWorker;

    @Test
    void executeInDocker_ShouldRunContainerAndUpdateStatusToFinished() {
        ExecutionJob job = new ExecutionJob(new ExecutionRequest("echo 'test'", 1.0, 256L));

        // Mock Create Command (RETURNS_SELF handles all .withX() chains automatically!)
        CreateContainerCmd mockCreateCmd = Mockito.mock(CreateContainerCmd.class, Answers.RETURNS_SELF);
        CreateContainerResponse mockResponse = new CreateContainerResponse();
        mockResponse.setId("mock-id"); // The ID will no longer be null!
        when(mockDockerClient.createContainerCmd(anyString())).thenReturn(mockCreateCmd);
        when(mockCreateCmd.exec()).thenReturn(mockResponse);

        // Mock Start Command
        StartContainerCmd mockStartCmd = Mockito.mock(StartContainerCmd.class, Answers.RETURNS_SELF);
        when(mockDockerClient.startContainerCmd(anyString())).thenReturn(mockStartCmd);

        // Mock Log Command & Unblock the CountDownLatch
        LogContainerCmd mockLogCmd = Mockito.mock(LogContainerCmd.class, Answers.RETURNS_SELF);
        when(mockDockerClient.logContainerCmd(anyString())).thenReturn(mockLogCmd);
        when(mockLogCmd.exec(any())).thenAnswer(invocation -> {
            com.github.dockerjava.api.async.ResultCallback.Adapter<?> callback = invocation.getArgument(0);
            callback.onComplete(); // Instantly unblocks awaitCompletion()
            return callback;
        });

        // Mock Wait Command
        WaitContainerCmd mockWaitCmd = Mockito.mock(WaitContainerCmd.class, Answers.RETURNS_SELF);
        WaitContainerResultCallback mockWaitCallback = Mockito.mock(WaitContainerResultCallback.class);
        when(mockDockerClient.waitContainerCmd(anyString())).thenReturn(mockWaitCmd);
        when(mockWaitCmd.start()).thenReturn(mockWaitCallback);

        // Mock Inspect Command (Returns Exit Code 0)
        InspectContainerCmd mockInspectCmd = Mockito.mock(InspectContainerCmd.class, Answers.RETURNS_SELF);
        InspectContainerResponse mockInspectResponse = Mockito.mock(InspectContainerResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(mockDockerClient.inspectContainerCmd(anyString())).thenReturn(mockInspectCmd);
        when(mockInspectCmd.exec()).thenReturn(mockInspectResponse);
        when(mockInspectResponse.getState().getExitCodeLong()).thenReturn(0L);

        // Mock Remove Command
        RemoveContainerCmd mockRemoveCmd = Mockito.mock(RemoveContainerCmd.class, Answers.RETURNS_SELF);
        when(mockDockerClient.removeContainerCmd(anyString())).thenReturn(mockRemoveCmd);

        // ACT
        dockerWorker.executeInDocker(job);

        // ASSERT
        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.FINISHED);
    }

    @Test
    void executeInDocker_ShouldSetStatusToFailed_WhenExitCodeIsNotZero() {
        ExecutionJob job = new ExecutionJob(new ExecutionRequest("bad_cmd", 1.0, 256L));

        // Create
        CreateContainerCmd mockCreateCmd = Mockito.mock(CreateContainerCmd.class, Answers.RETURNS_SELF);
        CreateContainerResponse mockResponse = new CreateContainerResponse();
        mockResponse.setId("mock-id");
        when(mockDockerClient.createContainerCmd(anyString())).thenReturn(mockCreateCmd);
        when(mockCreateCmd.exec()).thenReturn(mockResponse);

        // Start, Log, Wait
        when(mockDockerClient.startContainerCmd(anyString())).thenReturn(Mockito.mock(StartContainerCmd.class, Answers.RETURNS_SELF));

        LogContainerCmd mockLogCmd = Mockito.mock(LogContainerCmd.class, Answers.RETURNS_SELF);
        when(mockDockerClient.logContainerCmd(anyString())).thenReturn(mockLogCmd);
        when(mockLogCmd.exec(any())).thenAnswer(invocation -> {
            com.github.dockerjava.api.async.ResultCallback.Adapter<?> callback = invocation.getArgument(0);
            callback.onComplete();
            return callback;
        });

        // Wait
        WaitContainerCmd mockWaitCmd = Mockito.mock(WaitContainerCmd.class, Answers.RETURNS_SELF);
        WaitContainerResultCallback mockWaitCallback = Mockito.mock(WaitContainerResultCallback.class);
        when(mockDockerClient.waitContainerCmd(anyString())).thenReturn(mockWaitCmd);
        when(mockWaitCmd.start()).thenReturn(mockWaitCallback);

        // Inspect (Mocking exit code 137 for OOM)
        InspectContainerCmd mockInspectCmd = Mockito.mock(InspectContainerCmd.class, Answers.RETURNS_SELF);
        InspectContainerResponse mockInspectResponse = Mockito.mock(InspectContainerResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(mockDockerClient.inspectContainerCmd(anyString())).thenReturn(mockInspectCmd);
        when(mockInspectCmd.exec()).thenReturn(mockInspectResponse);
        when(mockInspectResponse.getState().getExitCodeLong()).thenReturn(137L);

        // Remove
        when(mockDockerClient.removeContainerCmd(anyString())).thenReturn(Mockito.mock(RemoveContainerCmd.class, Answers.RETURNS_SELF));

        // ACT
        dockerWorker.executeInDocker(job);

        // ASSERT
        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("Exit code: 137");
    }

    @Test
    void executeInDocker_ShouldSetStatusToFailed_WhenDockerThrowsException() {
        ExecutionJob job = new ExecutionJob(new ExecutionRequest("echo 'test'", 1.0, 256L));

        // Simulate the Docker socket being completely unavailable
        when(mockDockerClient.createContainerCmd(anyString())).thenThrow(new RuntimeException("Connection refused"));

        dockerWorker.executeInDocker(job);

        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("Connection refused");
    }
}