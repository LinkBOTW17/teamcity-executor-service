package io.github.linkbotw17.remote_executor.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionJobTest {

    @Test
    void newJobShouldBeQueuedWithGeneratedId() {
        // Arrange
        ExecutionRequest request = new ExecutionRequest("echo 'Hello World'", 1.0, 256L);

        // Act
        ExecutionJob job = new ExecutionJob(request);

        // Assert
        assertThat(job.getId()).isNotNull().isNotEmpty();
        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
        assertThat(job.getRequest()).isEqualTo(request);
    }

    @Test
    void statusShouldUpdateCorrectly() {
        // Arrange
        ExecutionRequest request = new ExecutionRequest("ls -la", 0.5, 32L);
        ExecutionJob job = new ExecutionJob(request);

        // Act
        job.setStatus(ExecutionStatus.IN_PROGRESS);

        // Assert
        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.IN_PROGRESS);
    }
}