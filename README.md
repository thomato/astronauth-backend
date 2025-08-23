# Astronauth
This project uses **Spring Boot** and **Kotlin**.

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