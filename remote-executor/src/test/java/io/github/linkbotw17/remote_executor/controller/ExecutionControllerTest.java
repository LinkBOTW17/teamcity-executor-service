package io.github.linkbotw17.remote_executor.controller;

import io.github.linkbotw17.remote_executor.model.ExecutionJob;
import io.github.linkbotw17.remote_executor.model.ExecutionRequest;
import io.github.linkbotw17.remote_executor.service.ExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExecutionController.class)
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExecutionService executionService;

    @Test
    void submitCommand_ShouldReturn202Accepted() throws Exception {
        ExecutionRequest request = new ExecutionRequest("echo 'Test'", 1.0, 256L);
        ExecutionJob mockJob = new ExecutionJob(request);

        Mockito.when(executionService.submitJob(Mockito.any(ExecutionRequest.class))).thenReturn(mockJob);

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"script\": \"echo 'Test'\", \"cpuCount\": 1.0, \"memoryMb\": 256}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(mockJob.getId()))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void getStatus_ShouldReturn200_WhenJobExists() throws Exception {
        ExecutionJob mockJob = new ExecutionJob(new ExecutionRequest("ls", 0.5, 32L));

        Mockito.when(executionService.getJob(mockJob.getId())).thenReturn(mockJob);

        mockMvc.perform(get("/api/execute/" + mockJob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mockJob.getId()));
    }

    @Test
    void getStatus_ShouldReturn404_WhenJobDoesNotExist() throws Exception {
        Mockito.when(executionService.getJob("fake-id")).thenReturn(null);

        mockMvc.perform(get("/api/execute/fake-id"))
                .andExpect(status().isNotFound());
    }
}