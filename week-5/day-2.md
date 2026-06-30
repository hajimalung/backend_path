# Week 5 — Day 2: Docker + Containerizing Spring Boot

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Why Docker? (5 min)

**Problem without Docker:** "It works on my machine" — different JDK versions, different OS, different environment variables.

**Docker solution:** Package your app + its runtime + its config into a **container** — a lightweight, isolated, reproducible unit. Run the same image anywhere.

Key concepts:
- **Image**: read-only blueprint (like a class)
- **Container**: running instance of an image (like an object)
- **Dockerfile**: instructions to build an image
- **Registry**: store for images (Docker Hub, GitHub Container Registry)
- **docker-compose**: tool to run multiple containers together

---

### 2. Essential Docker Commands (10 min)

```bash
# Images
docker pull openjdk:21-jre-slim          # download image
docker images                            # list local images
docker rmi image-name                    # remove image

# Containers
docker run -d -p 8080:8080 my-app        # run container, detached, port mapping
docker run -e DB_URL=jdbc:... my-app     # pass environment variable
docker ps                                # list running containers
docker ps -a                             # all containers (including stopped)
docker logs container-id                 # view logs
docker logs -f container-id              # follow logs
docker stop container-id                 # stop container
docker rm container-id                   # remove stopped container
docker exec -it container-id /bin/bash   # shell into container

# Build
docker build -t my-app:1.0 .            # build image from Dockerfile
docker build -t my-app:latest .          # tag as latest
```

---

### 3. Dockerfile for Spring Boot (20 min)

**Basic Dockerfile:**
```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline           # cache dependencies first
COPY src ./src
RUN mvn package -DskipTests             # build jar

# Stage 2: Run
FROM eclipse-temurin:21-jre-jammy       # much smaller — only JRE
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Run as non-root (security best practice)
RUN adduser --system --group appuser
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why multi-stage?** The builder stage (with Maven + full JDK) is ~500MB. The final image only needs the JRE + jar, which is ~150MB.

**Build and run:**
```bash
docker build -t task-api:latest .
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/taskdb \
  --name task-api \
  task-api:latest
```

Note: `host.docker.internal` resolves to your host machine from inside a container (Mac/Windows). On Linux, use the host's IP.

---

### 4. Docker Compose (20 min)

Run your API + PostgreSQL together:

```yaml
# docker-compose.yml
version: "3.9"

services:
  db:
    image: postgres:16-alpine
    container_name: task-db
    environment:
      POSTGRES_DB: taskdb
      POSTGRES_USER: taskuser
      POSTGRES_PASSWORD: taskpass
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data   # persist data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U taskuser -d taskdb"]
      interval: 10s
      timeout: 5s
      retries: 5

  api:
    build: .
    container_name: task-api
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/taskdb
      SPRING_DATASOURCE_USERNAME: taskuser
      SPRING_DATASOURCE_PASSWORD: taskpass
      APP_SECURITY_JWT_SECRET: my-256-bit-secret-for-production-change-this
    depends_on:
      db:
        condition: service_healthy          # wait until DB is ready
    restart: on-failure

volumes:
  postgres-data:
```

**Key compose commands:**
```bash
docker compose up -d           # start all services in background
docker compose up --build -d   # rebuild images first
docker compose logs -f api     # follow api logs
docker compose ps              # status of all services
docker compose down            # stop and remove containers
docker compose down -v         # also remove volumes (deletes DB data!)
```

**Networking in compose**: Services reference each other by **service name** (e.g., `db`, not `localhost`). Docker creates a private network for all services in the same compose file.

---

### 5. Application Properties for Container (5 min)

```yaml
# application-prod.yml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}         # read from env var
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate                  # never auto-create in production
    show-sql: false

app:
  security:
    jwt-secret: ${APP_SECURITY_JWT_SECRET}
    jwt-expiration-ms: 3600000

server:
  port: 8080
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between a Docker image and a Docker container?

**Q2.** What is a multi-stage Dockerfile and why do we use it?

**Q3.** In a docker-compose file, how do services communicate with each other? (e.g., how does `api` connect to `db`?)

**Q4.** What does `depends_on` with `condition: service_healthy` do?

**Q5.** Why should you run the app container as a non-root user?

---

### Part B — Hands-on (20 min)

1. Add a `Dockerfile` (multi-stage) to your Task Manager project.
2. Create a `docker-compose.yml` with `db` (PostgreSQL) and `api` services.
3. Add `application-prod.yml` that reads all credentials from environment variables.
4. Run `docker compose up --build -d` and verify:
   - `docker compose ps` shows both services as healthy/running
   - `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`
   - `curl -X POST http://localhost:8080/api/auth/register ...` works

---

### Answers

**A1.** An **image** is a static, read-only blueprint (layers of filesystem changes stacked together). A **container** is a running instance of an image with its own writable layer, process space, and network interface. Multiple containers can run from the same image simultaneously.

**A2.** A multi-stage Dockerfile uses multiple `FROM` statements. Early stages do heavy work (compile, build) and later stages copy only the artifacts needed to run. This keeps the final image small — you don't ship Maven, the full JDK, or build caches into production. Smaller images = faster pulls, smaller attack surface, less storage.

**A3.** Docker Compose creates a private network for all services in the file. Services communicate using the **service name as the hostname**. If the API service needs to connect to PostgreSQL, it uses `jdbc:postgresql://db:5432/...` — `db` resolves to the PostgreSQL container's IP on the private network.

**A4.** `depends_on` with `condition: service_healthy` makes the `api` service wait until `db` reports healthy (passes its `healthcheck` test) before starting. Without this, the API might start before PostgreSQL is ready to accept connections, causing connection errors on startup.

**A5.** Running as root inside a container means that if the container is compromised, the attacker has root privileges inside it, which may also give access to the host filesystem via volume mounts. Running as a non-root user follows the **principle of least privilege** — the app only has the permissions it needs to run.
