# Week 5 — Day 4: Spring Cloud Gateway

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. What Is an API Gateway? (10 min)

Without a gateway, every external client needs to know about every service:

```
Client ──→ user-service:8081
Client ──→ task-service:8082
Client ──→ notification-service:8083
```

Problems: clients manage multiple URLs, CORS and auth logic duplicated in each service, no central rate limiting.

**With a Gateway:**
```
Client ──→ gateway:8080
               │
               ├──→ user-service
               ├──→ task-service
               └──→ notification-service
```

The gateway is the **single entry point** for all clients. It handles:
- Request routing (path-based, header-based)
- Authentication/authorization
- Rate limiting
- SSL termination
- Request/response transformation
- Load balancing

---

### 2. Spring Cloud Gateway Setup (15 min)

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

```yaml
# gateway/application.yml
server:
  port: 8080

spring:
  application:
    name: api-gateway

  cloud:
    gateway:
      discovery:
        locator:
          enabled: true             # auto-create routes from Eureka services
          lower-case-service-id: true

      routes:
        - id: user-service
          uri: lb://user-service    # lb:// = load-balanced via Eureka
          predicates:
            - Path=/api/users/**    # route if path matches
          filters:
            - StripPrefix=0         # don't strip path prefix

        - id: task-service
          uri: lb://task-service
          predicates:
            - Path=/api/tasks/**
          filters:
            - AddRequestHeader=X-Gateway-Source, api-gateway  # add header

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

Now all traffic goes through port 8080:
- `GET http://localhost:8080/api/users/1` → routed to `user-service`
- `GET http://localhost:8080/api/tasks` → routed to `task-service`

---

### 3. Predicates (10 min)

Predicates define **when** a route matches a request:

```yaml
routes:
  - id: v1-route
    uri: lb://task-service
    predicates:
      - Path=/api/tasks/**           # path matches

  - id: header-route
    uri: lb://task-service
    predicates:
      - Header=X-Version, v2         # header name+value

  - id: method-route
    uri: lb://task-service
    predicates:
      - Method=GET, POST             # HTTP method

  - id: combined-route
    uri: lb://task-service
    predicates:
      - Path=/api/tasks/**
      - Method=GET                   # both must match (AND logic)
```

---

### 4. Filters (15 min)

Filters transform requests and/or responses:

**Built-in filters:**
```yaml
filters:
  - StripPrefix=1                   # remove first path segment
  - AddRequestHeader=X-User-ID, user123
  - AddResponseHeader=X-Response-Time, 2024
  - RewritePath=/old/(?<segment>.*), /new/${segment}
  - CircuitBreaker=name=myCircuit,fallbackUri=forward:/fallback
  - RequestRateLimiter=redis-rate-limiter.replenishRate=10,redis-rate-limiter.burstCapacity=20
```

**Custom Global Filter** (runs on every route):
```java
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        log.info("Gateway: {} {}", request.getMethod(), request.getURI());

        long start = System.currentTimeMillis();
        return chain.filter(exchange)
            .doFinally(signalType -> {
                long duration = System.currentTimeMillis() - start;
                log.info("Completed in {}ms", duration);
            });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
```

**JWT validation filter** (validate token at gateway — services trust the gateway):
```java
@Component
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;

    public JwtValidationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Skip auth for public endpoints
        if (path.startsWith("/api/auth/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        if (!jwtUtils.isTokenValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Add user info to header for downstream services
        String username = jwtUtils.extractUsername(token);
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(r -> r.header("X-User-Name", username))
            .build();
        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() { return -1; }  // run early
}
```

---

### 5. CORS at Gateway (5 min)

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "http://localhost:3000"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** Name three responsibilities of an API Gateway (beyond just routing).

**Q2.** What does `lb://task-service` mean in a gateway route URI?

**Q3.** What is a predicate in Spring Cloud Gateway? Give two examples.

**Q4.** What is the difference between a Global Filter and a route-specific filter?

**Q5.** Why is it useful to validate JWT at the gateway instead of in each microservice?

---

### Part B — Hands-on (20 min)

1. Create a `api-gateway` Spring Boot project with `spring-cloud-starter-gateway` + Eureka client.
2. Configure routes: `Path=/api/tasks/**` → `lb://task-service`.
3. Add a `LoggingFilter` that logs method + path + response time.
4. Test: start eureka-server + task-service + api-gateway.
5. Verify: `curl http://localhost:8080/api/tasks` returns task data (routed through gateway).

---

### Answers

**A1.** Any three: Authentication/JWT validation, Rate limiting, SSL termination, Load balancing, CORS handling, Request/response transformation (add/remove headers), Circuit breaking, Logging/monitoring, Path rewriting.

**A2.** `lb://` prefix tells Spring Cloud Gateway to use **load balancing** for this route. The service name `task-service` is resolved via **Eureka** — the gateway looks up all instances registered under that name and distributes requests. Without `lb://`, you'd use a static URL like `http://localhost:8082`.

**A3.** A predicate is a condition that determines if a request should be matched to this route. Examples: `Path=/api/tasks/**` matches requests whose path starts with `/api/tasks/`; `Method=GET` matches only HTTP GET requests; `Header=X-Version, v2` matches requests with a specific header value.

**A4.** A **Global Filter** applies to **every route** — it wraps all traffic through the gateway (logging, auth, metrics). A **route-specific filter** (defined under `filters:` in a route) only applies to traffic matching that specific route — useful for things like `StripPrefix` or service-specific header manipulation.

**A5.** Validating JWT at the gateway centralizes auth logic — you write it once instead of duplicating it in every service. Internal services can trust that any request that passes the gateway is already authenticated. This simplifies services and reduces the risk of a service forgetting to validate tokens. Services just read the `X-User-Name` header the gateway adds.
