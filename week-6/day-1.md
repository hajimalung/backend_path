# Week 6 — Day 1: Resilience4j — Circuit Breakers, Retries, and More

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Why Resilience? (5 min)

In distributed systems, failures are inevitable. A service may:
- Be temporarily down (restart, deploy)
- Respond slowly (high load, GC pause)
- Return errors for a percentage of requests

Without resilience patterns, one slow service can cascade and bring down the whole system.

**Resilience patterns:**
| Pattern | Problem it solves |
|---------|------------------|
| Retry | Transient failures (brief network blip) |
| Circuit Breaker | Cascading failures (stop calling a broken service) |
| Bulkhead | Resource exhaustion (isolate thread pools per dependency) |
| Rate Limiter | Overloading a service |
| Timeout | Slow responses blocking your threads |

---

### 2. Dependency Setup (5 min)

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
<!-- AOP is required -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

---

### 3. Circuit Breaker (20 min)

A circuit breaker has three states:
```
CLOSED ──[failure rate exceeds threshold]──> OPEN
OPEN   ──[wait duration passes]──────────> HALF-OPEN
HALF-OPEN ──[test calls succeed]──────────> CLOSED
HALF-OPEN ──[test calls fail]─────────────> OPEN
```

- **CLOSED**: calls pass through normally
- **OPEN**: all calls fail immediately (fast fail) — no calls reach the service
- **HALF-OPEN**: allow a few test calls through to check if service recovered

```yaml
# application.yml
resilience4j:
  circuit-breaker:
    instances:
      user-service:
        register-health-indicator: true
        sliding-window-size: 10            # evaluate last 10 calls
        failure-rate-threshold: 50         # open if 50%+ fail
        wait-duration-in-open-state: 10s   # wait 10s before trying HALF-OPEN
        permitted-number-of-calls-in-half-open-state: 3
        slow-call-duration-threshold: 3s   # calls > 3s are counted as slow
        slow-call-rate-threshold: 80       # open if 80%+ are slow
```

```java
@Service
public class TaskService {

    private final UserServiceClient userClient;

    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserFallback")
    public UserDto getUser(String username) {
        return userClient.getByUsername(username);
    }

    // Fallback must match the return type + add Throwable parameter
    private UserDto getUserFallback(String username, Throwable throwable) {
        log.warn("Circuit open for user-service, using fallback. Error: {}", throwable.getMessage());
        return new UserDto(null, username);   // degraded response
    }
}
```

---

### 4. Retry (15 min)

```yaml
resilience4j:
  retry:
    instances:
      user-service:
        max-attempts: 3                          # try 3 times total
        wait-duration: 500ms                     # wait 500ms between retries
        retry-exceptions:
          - java.io.IOException
          - feign.FeignException$ServiceUnavailable
        ignore-exceptions:
          - com.example.exception.TaskNotFoundException  # don't retry on business errors
```

```java
@Retry(name = "user-service", fallbackMethod = "getUserFallback")
@CircuitBreaker(name = "user-service", fallbackMethod = "getUserFallback")
public UserDto getUser(String username) {
    return userClient.getByUsername(username);
}
```

**Important**: Apply retry BEFORE circuit breaker (retry tries multiple times, circuit breaker counts each attempt as a call).

---

### 5. Bulkhead (10 min)

Limits concurrent calls to a dependency — prevents one service from consuming all threads:

```yaml
resilience4j:
  bulkhead:
    instances:
      user-service:
        max-concurrent-calls: 10       # only 10 concurrent calls allowed
        max-wait-duration: 0ms         # fail immediately if limit reached (0ms wait)
```

```java
@Bulkhead(name = "user-service", fallbackMethod = "getUserFallback")
public UserDto getUser(String username) {
    return userClient.getByUsername(username);
}
```

Use bulkhead when one slow dependency shouldn't block all other requests.

---

### 6. Rate Limiter (5 min)

```yaml
resilience4j:
  rate-limiter:
    instances:
      user-service:
        limit-for-period: 10           # max 10 calls per period
        limit-refresh-period: 1s       # period = 1 second
        timeout-duration: 0            # fail immediately if over limit
```

```java
@RateLimiter(name = "user-service", fallbackMethod = "getUserFallback")
public UserDto getUser(String username) { ... }
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** Describe the three states of a circuit breaker and what triggers each transition.

**Q2.** What is the difference between a retry and a circuit breaker? When would you use both together?

**Q3.** What is a bulkhead and what problem does it solve?

**Q4.** What is a fallback method? What must its signature match?

**Q5.** If a business exception like `TaskNotFoundException` is thrown, should you retry? Why or why not?

---

### Part B — Hands-on (20 min)

Add resilience to your `task-service`'s call to `user-service`:

1. Add Resilience4j + AOP dependencies to `task-service`.
2. Configure a circuit breaker named `user-service` in `application.yml`:
   - Sliding window: 5 calls
   - Failure threshold: 50%
   - Wait duration: 10s
3. Annotate the Feign call with `@CircuitBreaker(name = "user-service", fallbackMethod = "userFallback")`.
4. Implement the fallback that returns a placeholder `UserDto`.
5. Test it: stop `user-service` and verify `task-service` uses the fallback instead of crashing.

---

### Answers

**A1.** **CLOSED**: Normal operation — calls pass through, failure rate is tracked. If failure rate exceeds threshold → transitions to OPEN. **OPEN**: All calls fail immediately with a `CallNotPermittedException` — no calls reach the service. After `wait-duration-in-open-state` → transitions to HALF-OPEN. **HALF-OPEN**: A limited number of test calls are permitted. If they succeed → CLOSED. If they fail → OPEN again.

**A2.** Retry handles **transient** failures — brief network glitches where the service is momentarily unavailable and will likely succeed on the 2nd or 3rd attempt. Circuit breaker handles **sustained** failures — the service is down and won't recover quickly. Together: retry first (handles brief blips), then circuit breaker tracks overall health. If retries consistently fail, the circuit opens and stops trying altogether, preventing cascade failures.

**A3.** A bulkhead limits the **number of concurrent calls** to a dependency. Like bulkheads in a ship (watertight compartments), it isolates failures — if a slow dependency fills its limit of 10 threads, the other 190 threads remain available for other operations. Without bulkheads, one slow service can fill your thread pool and make the whole application unresponsive.

**A4.** A fallback method is an alternative implementation called when the primary method fails (circuit open, exception, timeout). It must: (1) be in the same class, (2) have the same return type, (3) have the same parameters as the original method, plus (4) an additional `Throwable` parameter as the last argument.

**A5.** No. Business exceptions (`TaskNotFoundException`, `InvalidInputException`) indicate that the request itself is wrong — retrying won't change the outcome and wastes resources. Only retry on **infrastructure** failures (network errors, timeouts, 503 Service Unavailable). Configure `ignore-exceptions` in Resilience4j to exclude business exceptions from retry logic.
