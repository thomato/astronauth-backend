# Astronauth
This project uses **Spring Boot** and **Kotlin**.

## Frontend
Astronauth's own pages (Registration, Email verification) are a React SPA in `frontend/` (ADR 0004), built with Vite and Tailwind and bundled into the jar by Gradle, which downloads its own Node and pnpm. `./gradlew bootRun` serves them at http://localhost:8080/register with the `dev` profile (GraphiQL at `/graphiql`); verification emails land in Mailpit at http://localhost:8025.

For fast frontend work, run `pnpm dev` in `frontend/` next to `./gradlew bootRun` and open http://localhost:5173/register; Vite proxies `/graphql` to Spring.

- `pnpm test`: component tests (Vitest, Testing Library, MSW); also run by `./gradlew test`
- `pnpm lint`: ESLint, Prettier and the type check; also run by `./gradlew check`
- `pnpm e2e`: Playwright against the running application, from Registration through the emailed link to Email verification; starts `./gradlew bootRun` if nothing runs on port 8080

TypeScript types for GraphQL operations are generated from `src/main/resources/graphql/schema.graphqls` (`pnpm codegen`, run by the scripts above). All wording lives in `frontend/src/copy/en.ts`; a translation is another file of the same `Copy` type.

## Pre-commit Setup
This project uses pre-commit hooks to ensure code quality. To set up:

1. Install pre-commit: `pip3 install pre-commit`
2. Install hooks: `pre-commit install`

The hooks will automatically run:
- **ktlint** for Kotlin formatting and style checks
- **detekt** for static code analysis  
- **tests** to ensure all tests pass
- **conventional-pre-commit** to check that commit messages follow [Conventional Commits](https://www.conventionalcommits.org/)

### Commit messages
Commits follow [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<optional scope>): <description>`, for example `feat(registration): reject duplicate emails`.

Allowed types: `feat`, `fix`, `refactor`, `docs`, `style`, `test`, `chore`, `build`, `ci`, `perf`. Flag breaking changes with a `BREAKING CHANGE:` footer.

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