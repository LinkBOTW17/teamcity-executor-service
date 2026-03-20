# Remote Executor Service

A Spring Boot REST API for asynchronously executing shell scripts inside isolated Docker containers. 

This project was developed as a technical task, focusing on asynchronous processing, container orchestration, and clean architectural design.

## Features

* **Asynchronous Execution:** Scripts are executed in background threads, immediately returning a job ID to the user.
* **Docker Isolation:** Every script runs inside a fresh `alpine:latest` Docker container, preventing host machine contamination.
* **Resource Limiting:** CPU limits specified in the request are translated to Docker NanoCPUs and enforced on the container.
* **Status Tracking:** Users can poll the API to see if their job is `QUEUED`, `IN_PROGRESS`, or `FINISHED`.

## Architecture & Design Decisions

* **Spring AOP & Async:** The application leverages Spring's `@Async` for multithreading. To prevent bypassing the Spring AOP proxy, the background execution logic is extracted into a dedicated `DockerWorker` component, separating infrastructure concerns from business logic.
* **docker-java:** Uses the official Java Docker API client to communicate directly with the local Docker daemon via sockets, avoiding fragile `Runtime.exec()` shell commands.
* **Thread-Safe State:** Job states are stored in a `ConcurrentHashMap` to ensure thread safety between the web server threads and background worker threads.

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
}
```

**Response (202 Accepted):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "request": {
    "script": "sleep 5 && echo 'Hello from Docker!'",
    "cpuCount": 1.0
  },
  "status": "QUEUED"
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
    "cpuCount": 1.0
  },
  "status": "FINISHED"
}
```
