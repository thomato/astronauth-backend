# Astronauth
This project uses **Spring Boot** and **Kotlin**.

## Pre-commit Setup
This project uses pre-commit hooks to ensure code quality. To set up:

1. Install pre-commit: `pip3 install pre-commit`
2. Install hooks: `pre-commit install`

The hooks will automatically run:
- **ktlint** for Kotlin formatting and style checks
- **detekt** for static code analysis  
- **tests** to ensure all tests pass

### Docker Configuration for Tests
Tests use testcontainers and require Docker to be running. Configuration depends on your Docker setup:

**For Docker Desktop users:** No additional configuration needed.

**For Lima Docker users:** Set these environment variables in your shell:
```bash
export DOCKER_HOST=unix:///Users/$USER/.lima/docker/sock/docker.sock
export TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1
export TESTCONTAINERS_RYUK_DISABLED=true
```

**For other Docker setups:** Configure `DOCKER_HOST` to point to your Docker daemon socket.

## Setup IntelliJ
### Running Docker externally
If you run Docker externally (e.g. using Lima), you should add the following:
1. Go to **Run** → **Edit Configurations...**
2. Select your test configuration (or create a new JUnit configuration)
3. In the **Environment variables** section, add:
    - `DOCKER_HOST`: `unix:///Users/<your_home_folder>/.lima/docker/sock/docker.sock`
    - `TESTCONTAINERS_HOST_OVERRIDE`: `localhost`
    - `TESTCONTAINERS_RYUK_DISABLED`: `true`
    - `TESTCONTAINERS_HOST_OVERRIDE`: `127.0.0.1`

Now, configure the IntelliJ Docker integration:
1. Go to IntelliJ Settings → Build, Execution, Deployment → Docker
2. Add a new Docker configuration:
    - Click the "+" button
    - Choose "Docker for Mac" or "Docker"
    - Set the Engine API URL to: unix:///Users/YOUR_USERNAME/.lima/docker/sock/docker.sock
    - Replace YOUR_USERNAME with your actual username
    - Test the connection
3. Make this the default Docker configuration by moving it to the top of the list