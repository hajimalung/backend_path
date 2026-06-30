# Week 5 — Day 6 & 7: Mini Project 3 — Split Task Manager into Microservices

> Build time: 2 days (~1 hour each) | No separate test — this IS the test

---

## Goal

Split your Task Manager into two independent microservices connected through Eureka and Gateway:
- **user-service** (port 8081): manages users, handles auth
- **task-service** (port 8082): manages tasks, calls user-service via Feign

All traffic enters through the **api-gateway** (port 8080).

---

## Architecture

```
External Client
      ↓
 api-gateway:8080
      │
      ├──→ /api/auth/**   → user-service:8081
      ├──→ /api/users/**  → user-service:8081
      └──→ /api/tasks/**  → task-service:8082

 eureka-server:8761
      ↑
      ├── user-service registers as "user-service"
      ├── task-service registers as "task-service"
      └── api-gateway registers as "api-gateway"
```

---

## Day 6 — Extract user-service

### Project Structure

```
microservices/
├── eureka-server/          (already built — Week 5 Day 3)
├── api-gateway/            (already built — Week 5 Day 4)
├── user-service/           ← NEW
│   ├── src/main/java/com/example/user/
│   │   ├── UserServiceApplication.java
│   │   ├── entity/AppUser.java
│   │   ├── dto/UserDto.java
│   │   ├── dto/RegisterRequest.java
│   │   ├── dto/LoginRequest.java
│   │   ├── dto/LoginResponse.java
│   │   ├── repository/UserRepository.java
│   │   ├── service/AuthService.java
│   │   ├── service/CustomUserDetailsService.java
│   │   ├── controller/AuthController.java
│   │   ├── controller/UserController.java
│   │   └── security/JwtUtils.java
│   └── src/main/resources/application.yml
└── task-service/           ← Update existing
```

### user-service: Key Classes

**AppUser entity** (same as before, but no tasks list — tasks live in task-service):
```java
@Entity @Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String username;
    @Column(nullable = false) private String password;
    // getters, constructor
}
```

**UserDto (public contract):**
```java
public record UserDto(Long id, String username) {
    public static UserDto from(AppUser user) {
        return new UserDto(user.getId(), user.getUsername());
    }
}
```

**UserController:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(u -> ResponseEntity.ok(UserDto.from(u)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserDto> getByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
            .map(u -> ResponseEntity.ok(UserDto.from(u)))
            .orElse(ResponseEntity.notFound().build());
    }
}
```

**user-service/application.yml:**
```yaml
spring:
  application:
    name: user-service
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: taskuser
    password: taskpass
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: 8081

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

app:
  security:
    jwt-secret: my-256-bit-secret-at-least-32-chars-long
    jwt-expiration-ms: 3600000
```

---

## Day 7 — Update task-service + Wire Everything Together

### task-service Changes

**Remove** User entity, UserRepository, AuthService from task-service.

**Add** UserServiceClient (Feign):
```java
@FeignClient(name = "user-service", path = "/api/users")
public interface UserServiceClient {
    @GetMapping("/by-username/{username}")
    UserDto getByUsername(@PathVariable String username);
}
```

**Update** Task entity — store `ownerUsername` (String) instead of a JPA relation:
```java
@Column(nullable = false)
private String ownerUsername;
```

**Update** TaskService — use Feign to resolve user data when needed:
```java
public TaskResponse create(CreateTaskRequest request, String ownerUsername) {
    Task task = new Task(request.title(), request.description(), request.priority(), ownerUsername);
    return TaskResponse.from(taskRepository.save(task));
}
```

**task-service/application.yml:**
```yaml
spring:
  application:
    name: task-service
  datasource:
    url: jdbc:postgresql://localhost:5432/taskdb
    username: taskuser
    password: taskpass
  jpa:
    hibernate:
      ddl-auto: update
  cloud:
    openfeign:
      client:
        config:
          user-service:
            connect-timeout: 2000
            read-timeout: 5000

server:
  port: 8082

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### API Gateway — Add Routes

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/auth/**, /api/users/**

        - id: task-service
          uri: lb://task-service
          predicates:
            - Path=/api/tasks/**
```

### docker-compose.yml for All Services

```yaml
version: "3.9"

services:
  userdb:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: userdb
      POSTGRES_USER: taskuser
      POSTGRES_PASSWORD: taskpass
    ports:
      - "5433:5432"

  taskdb:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: taskdb
      POSTGRES_USER: taskuser
      POSTGRES_PASSWORD: taskpass
    ports:
      - "5432:5432"

  eureka-server:
    build: ./eureka-server
    ports:
      - "8761:8761"

  user-service:
    build: ./user-service
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://userdb:5432/userdb
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      - userdb
      - eureka-server

  task-service:
    build: ./task-service
    ports:
      - "8082:8082"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://taskdb:5432/taskdb
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      - taskdb
      - eureka-server
      - user-service

  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    environment:
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      - eureka-server
```

---

## End-to-End Test Flow

```bash
# 1. Register a user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123"}'
# Expected: 201 Created

# 2. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123"}'
# Expected: {"token":"eyJ..."}

# 3. Create a task (replace TOKEN)
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy Groceries","priority":"HIGH"}'
# Expected: 201 Created with task JSON

# 4. Get tasks
curl http://localhost:8080/api/tasks \
  -H "Authorization: Bearer TOKEN"
# Expected: 200 with task list

# 5. Access without token
curl http://localhost:8080/api/tasks
# Expected: 401/403

# 6. Check Eureka dashboard
open http://localhost:8761
# Expected: TASK-SERVICE, USER-SERVICE, API-GATEWAY listed
```

---

## Completion Checklist

- [ ] `eureka-server` running on 8761
- [ ] `user-service` registered in Eureka as `USER-SERVICE`
- [ ] `task-service` registered in Eureka as `TASK-SERVICE`
- [ ] `api-gateway` routing to both services
- [ ] `POST /api/auth/register` creates user in `userdb`
- [ ] `POST /api/auth/login` returns JWT
- [ ] `POST /api/tasks` (with JWT) creates task in `taskdb`
- [ ] task-service calls user-service via Feign client (no hardcoded URL)
- [ ] `docker compose up` starts the full stack
- [ ] `http://localhost:8761` shows all 3 services registered

## What You Practiced

| Concept | Where used |
|---------|-----------|
| Microservice boundaries | user-service / task-service split |
| Service discovery | Eureka server + `@EnableDiscoveryClient` |
| API Gateway routing | Spring Cloud Gateway with `lb://` URIs |
| Feign client | task-service calling user-service |
| Inter-service contract | `UserDto` as shared DTO (not entity) |
| docker-compose | Multi-service orchestration |
| Separate databases | userdb + taskdb — services own their data |
