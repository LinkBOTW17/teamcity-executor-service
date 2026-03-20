# Remote Executor Service

A Spring Boot REST API for asynchronously executing shell scripts inside isolated Docker containers. 

This project was developed as a technical task, focusing on asynchronous processing, container orchestration, and clean architectural design.

## Features

* **Asynchronous Execution:** Scripts are executed in background threads, immediately returning a job ID to the user.
* **Docker Isolation:** Every script runs inside a fresh `alpine:latest` Docker container, preventing host machine contamination.
* **Resource Limiting:** Maps user-defined CPU and memory limits directly to the container's HostConfig, mirroring how real CI/CD runners allocate resources.
* **Resilient Status Tracking:** Users can poll the API to see if their job is `QUEUED`, `IN_PROGRESS`, `FINISHED`, or `FAILED`.
* **Crash Detection:** Inspects container exit codes to catch out-of-memory (OOM) kills or script crashes, returning a descriptive `errorMessage` rather than failing silently.

## Architecture & Design Decisions

* **Spring AOP & Async:** The application leverages Spring's `@Async` for multithreading. To prevent bypassing the Spring AOP proxy, the background execution logic is extracted into a dedicated `DockerWorker` component, separating infrastructure concerns from business logic.
* **docker-java (Zerodep):** Uses the official Java Docker API client with the modern Zerodep transport to communicate directly with the local Linux Docker daemon via sockets, avoiding fragile `Runtime.exec()` shell commands.
* **Thread-Safe State:** Job states are stored in a `ConcurrentHashMap` to ensure thread safety between the web server threads and background worker threads.
* **Explicit FAILED Status:** Introduces a FAILED state to prevent silent failures. If the Docker container crashes, the API accurately reflects a failed execution and provides the underlying exception.

## 📋 Prerequisites

Before running this application, ensure you have the following installed:
1. **Java 21**
2. **Gradle**
3. **Docker** (Docker Engine or Docker Desktop must be running)

*Note: Ensure your user has permissions to access the Docker socket (`/var/run/docker.sock` on Linux).*

## How to Run

1. Clone the repository and navigate to the project root.
2. Build and run the application using Gradle wrapper:
   ```bash
   ./gradlew bootRun
    ```
3. The server will start on `http://localhost:8080`

## Running Tests

Unit tests are written using JUnit 5 and Mockito. The `docker-java` client is mocked using deep stubs to ensure tests run instantly without requiring a live Docker daemon.

```bash
./gradlew test
```

## API Documentation

**1. Submit a Script for Execution**

**Endpoint:** `POST /api/execute`

**Request Body:**
```json
{
  "script": "sleep 5 && echo 'Hello from Docker!'",
  "cpuCount": 1.0
  "memoryMb": 256
}
```

**Response (202 Accepted):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "request": {
    "script": "sleep 5 && echo 'Hello from Docker!'",
    "cpuCount": 1.0,
    "memoryMb": 256
  },
  "status": "QUEUED",
  "errorMessage": null
}
```

**2. Check Execution Status**

**Endpoint:** `GET /api/execute/{id}`

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "request": {
    "script": "sleep 5 && echo 'Hello from Docker!'",
    "cpuCount": 1.0,
    "memoryMb": 256
  },
  "status": "FINISHED",
  "errorMessage": null
}
```

**Failed Response Example (200 OK):**
```json
{
  "id": "11eee6f8-28d1-440f-bd85-7b380d2711ef",
  "request": {
    "script": "x=\"a\"; while true; do x=$x$x; done",
    "cpuCount": 1.0,
    "memoryMb": 32
  },
  "status": "FAILED",
  "errorMessage": "Container failed or exceeded resources. Exit code: 137"
}
```

## Testing Resource Limits (The "Crash" Test)

To verify that the application properly enforces Docker resource limits and catches kernel-level terminations, you can submit an intentional Out-Of-Memory (OOM) attack.

Send this request, which infinitely doubles a string in memory but restricts the container to 32MB of RAM:

```bash
curl -X POST http://localhost:8080/api/execute \
     -H "Content-Type: application/json" \
     -d '{"script": "x=\"a\"; while true; do x=$x$x; done", "cpuCount": 1.0, "memoryMb": 32}'
```

Querying the returned ID will reveal a `FAILED` status with an Exit Code `137` (SIGKILL), proving the Linux OOM Killer successfully terminated the container and the application caught the failure.
