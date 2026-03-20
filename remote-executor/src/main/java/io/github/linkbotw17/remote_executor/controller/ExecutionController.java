package io.github.linkbotw17.remote_executor.controller;

import io.github.linkbotw17.remote_executor.model.ExecutionRequest;
import io.github.linkbotw17.remote_executor.model.ExecutionJob;
import io.github.linkbotw17.remote_executor.service.ExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/execute")
public class ExecutionController {
    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    // Endpoint 1: POST /api/execute (Submit a command)
    @PostMapping
    public ResponseEntity<ExecutionJob> submitCommand(@RequestBody ExecutionRequest request) {
        ExecutionJob job = executionService.submitJob(request);
        // Return a 202 Accepted status, along with the newly created job (which contains the ID)
        return ResponseEntity.accepted().body(job);
    }

    // Endpoint 2: GET /api/execute/{id} (Check the status)
    @GetMapping("/{id}")
    public ResponseEntity<ExecutionJob> getStatus(@PathVariable String id) {
        ExecutionJob job = executionService.getJob(id);

        if (job == null) {
            return ResponseEntity.notFound().build(); // Return 404 if ID doesn't exist
        }

        return ResponseEntity.ok(job); // Return 200 OK and the job's current status
    }
}
