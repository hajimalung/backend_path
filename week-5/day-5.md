# Week 5 — Day 5: OpenFeign — Declarative HTTP Clients

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. The Problem with RestTemplate (5 min)

Calling other services with `RestTemplate` is verbose:
```java
// Lots of boilerplate
ResponseEntity<UserDto> response = restTemplate.getForEntity(
    "http://user-service/api/users/" + userId, UserDto.class);
UserDto user = response.getBody();
```

You have to manually handle URL building, error mapping, serialization — and repeat it everywhere.

---

### 2. OpenFeign — Declarative HTTP Client (15 min)

Feign generates an implementation from an **interface**. You declare what you want to call — Feign handles the HTTP details.

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableFeignClients                         // scan for @FeignClient interfaces
public class TaskServiceApplication { ... }
```

```java
@FeignClient(
    name = "user-service",                  // service name in Eureka
    path = "/api/users"                     // base path
)
public interface UserServiceClient {

    @GetMapping("/{id}")
    UserDto getUser(@PathVariable Long id);

    @GetMapping
    List<UserDto> getAllUsers();

    @PostMapping
    UserDto createUser(@RequestBody CreateUserRequest request);

    @PutMapping("/{id}/deactivate")
    void deactivateUser(@PathVariable Long id);
}
```

```java
@Service
public class TaskService {
    private final UserServiceClient userClient;    // injected by Spring

    public TaskService(UserServiceClient userClient) {
        this.userClient = userClient;
    }

    public TaskResponse createForUser(Long userId, CreateTaskRequest request) {
        UserDto user = userClient.getUser(userId);  // single line — no boilerplate!
        // ... create task
    }
}
```

---

### 3. Error Handling (15 min)

By default, Feign throws `FeignException` on non-2xx responses. You can customize:

**Custom Error Decoder:**
```java
@Component
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 404 -> new ResourceNotFoundException("Resource not found: " + methodKey);
            case 400 -> new IllegalArgumentException("Bad request: " + methodKey);
            case 503 -> new ServiceUnavailableException("Service down: " + methodKey);
            default  -> new FeignException.FeignClientException(
                response.status(), "Feign error", response.request(), null, null);
        };
    }
}
```

**Fallback (with Resilience4j Circuit Breaker — Week 6):**
```java
@FeignClient(
    name = "user-service",
    fallback = UserServiceFallback.class      // used when service is down
)
public interface UserServiceClient { ... }

@Component
class UserServiceFallback implements UserServiceClient {
    @Override
    public UserDto getUser(Long id) {
        return UserDto.unknown(id);           // default response when service unavailable
    }

    @Override
    public List<UserDto> getAllUsers() {
        return List.of();
    }
}
```

---

### 4. Feign Configuration (10 min)

```yaml
# application.yml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:                           # applies to all feign clients
            connect-timeout: 2000           # 2s to establish connection
            read-timeout: 5000             # 5s to read response
            logger-level: FULL             # log request + response (dev only)
          user-service:                     # override for specific client
            connect-timeout: 1000
            read-timeout: 3000
```

**Logging:**
```java
@Configuration
public class FeignLoggingConfig {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;    // NONE, BASIC, HEADERS, FULL
    }
}
```

Enable in `application.yml`:
```yaml
logging:
  level:
    com.example.client.UserServiceClient: DEBUG
```

---

### 5. Passing Headers (e.g., JWT) (10 min)

When calling downstream services, you need to forward the auth token:

**Request Interceptor** — automatically adds headers to every Feign call:
```java
@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // Get token from current request's SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof String token) {
            template.header("Authorization", "Bearer " + token);
        }
    }
}
```

Or forward a request header:
```java
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    private final HttpServletRequest request;

    public FeignRequestInterceptor(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public void apply(RequestTemplate template) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            template.header("Authorization", authHeader);
        }
    }
}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What annotation enables Feign client scanning in a Spring Boot app?

**Q2.** In `@FeignClient(name = "user-service")`, what does `name = "user-service"` refer to?

**Q3.** What happens when a Feign client receives a 404 response by default? How can you customize this?

**Q4.** What is a `RequestInterceptor` used for?

**Q5.** Compare OpenFeign with `RestTemplate` — what are the advantages of Feign?

---

### Part B — Hands-on (20 min)

Add a `UserServiceClient` Feign client to your `task-service`:

1. Add `spring-cloud-starter-openfeign` dependency.
2. Create `UserServiceClient` interface with `@FeignClient(name = "user-service")`.
3. Add method: `@GetMapping("/{id}") UserDto getUser(@PathVariable Long id)`.
4. Create a `UserDto` record in `task-service` (copy only needed fields — don't reuse the entity).
5. In `TaskController`, add `GET /api/tasks/{id}/owner` that:
   - Fetches the task
   - Calls `userClient.getUser(task.getOwnerId())`
   - Returns combined response
6. Test the endpoint end-to-end with both services running.

---

### Answers

**A1.** `@EnableFeignClients` on the `@SpringBootApplication` class. You can also scope it: `@EnableFeignClients(basePackages = "com.example.clients")`.

**A2.** The `name` maps to `spring.application.name` of the target service registered in **Eureka**. Spring Cloud uses this name to look up available instances. With `lb://user-service` the load balancer picks one instance. Without Eureka, you'd use `url = "http://localhost:8081"` instead.

**A3.** By default, Feign throws a `FeignException` (specifically `FeignException.NotFound`) wrapping the HTTP 404. You can customize by implementing `ErrorDecoder` — a `@Component` that maps specific HTTP status codes to custom exceptions (e.g., `404 → ResourceNotFoundException`).

**A4.** A `RequestInterceptor` is called before every Feign request. It lets you mutate the `RequestTemplate` — add headers (like `Authorization`), set query parameters, or modify the URL. Useful for propagating auth tokens from the incoming request to outgoing Feign calls.

**A5.** Feign advantages over RestTemplate: (1) **Less code** — interface declaration vs verbose builder calls; (2) **Type safety** — return types and exceptions are declared explicitly; (3) **Automatic serialization** — Jackson handles request/response without manual mapping; (4) **Integrated with Eureka** — service name resolves automatically; (5) **Easy testing** — mock the interface, not HTTP; (6) **Interceptors** — centralize cross-cutting concerns like auth headers.
