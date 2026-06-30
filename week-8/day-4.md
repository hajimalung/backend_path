# Week 8 — Day 4: OWASP Top 10 in Spring Boot

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. OWASP Top 10 Overview (5 min)

The OWASP Top 10 is a list of the most critical web application security risks. As a backend engineer, you must know how to prevent each.

| # | Risk | Spring Boot mitigation |
|---|------|----------------------|
| A01 | Broken Access Control | `@PreAuthorize`, method security |
| A02 | Cryptographic Failures | BCrypt, HTTPS, secrets in env vars |
| A03 | Injection | JPA parameterized queries, validation |
| A04 | Insecure Design | Threat modeling, defense in depth |
| A05 | Security Misconfiguration | Actuator security, CORS config |
| A06 | Vulnerable Components | Dependency scanning (OWASP plugin) |
| A07 | Auth Failures | JWT + short expiry, rate limiting |
| A08 | Software Integrity Failures | Maven checksums, signed artifacts |
| A09 | Logging Failures | Don't log PII, log security events |
| A10 | SSRF | Validate URLs, block internal IPs |

---

### 2. A01 — Broken Access Control (15 min)

Most common vulnerability. Users access data or actions they shouldn't.

**Horizontal privilege escalation** (accessing another user's data):
```java
// VULNERABLE — user can access any task by ID
@GetMapping("/{id}")
public TaskResponse getById(@PathVariable Long id) {
    return taskService.findById(id);   // no ownership check!
}

// SECURE — enforce ownership
@GetMapping("/{id}")
public TaskResponse getById(@PathVariable Long id,
        @AuthenticationPrincipal UserDetails user) {
    Task task = taskService.findById(id);
    if (!task.getOwnerUsername().equals(user.getUsername())) {
        throw new AccessDeniedException("You don't own this task");
    }
    return TaskResponse.from(task);
}

// BETTER — check ownership in the query
@Transactional(readOnly = true)
public TaskResponse findByIdAndOwner(Long id, String username) {
    return taskRepository.findByIdAndOwnerUsername(id, username)
        .map(TaskResponse::from)
        .orElseThrow(() -> new TaskNotFoundException(id));
    // 404 instead of 403 — doesn't reveal existence of the resource
}
```

**Vertical privilege escalation** (accessing admin endpoints as a regular user):
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/tasks/**").hasRole("USER")
            .anyRequest().authenticated()
        ).build();
    }
}

// Method-level security
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/admin/users/{id}")
public void deleteUser(@PathVariable Long id) { ... }

@PreAuthorize("@taskSecurity.isOwner(#id, authentication.name)")
@DeleteMapping("/{id}")
public void delete(@PathVariable Long id) { ... }
```

---

### 3. A02 — Cryptographic Failures (10 min)

**Never store passwords in plain text:**
```java
// VULNERABLE
user.setPassword(request.password());

// SECURE — BCrypt (salt + slow hash)
user.setPassword(passwordEncoder.encode(request.password()));
```

**Never hardcode secrets:**
```java
// VULNERABLE
private static final String JWT_SECRET = "my-secret-key";

// SECURE — environment variable
@Value("${app.security.jwt-secret}")
private String jwtSecret;
```

**Enforce HTTPS:**
```yaml
server:
  ssl:
    enabled: true                 # in production (or handle at load balancer/ingress)

# Redirect HTTP to HTTPS
  port: 8443
```

**Sensitive data in responses — never expose:**
```java
// VULNERABLE — entity returned directly
return userRepository.findById(id);   // includes password hash!

// SECURE — DTO strips sensitive fields
public record UserResponse(Long id, String username) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername());
        // password NOT included
    }
}
```

---

### 4. A03 — Injection (10 min)

**SQL Injection — use JPA / parameterized queries:**
```java
// VULNERABLE — raw string concatenation
@Query(value = "SELECT * FROM tasks WHERE title = '" + title + "'", nativeQuery = true)
List<Task> findByTitle(String title);   // attacker sends: ' OR '1'='1

// SECURE — named parameter binding
@Query("SELECT t FROM Task t WHERE t.title = :title")
List<Task> findByTitle(@Param("title") String title);  // parameter is escaped

// Even better — derived query (no query string)
List<Task> findByTitleContainingIgnoreCase(String title);
```

**Input validation at the boundary:**
```java
public record CreateTaskRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title too long")
    @Pattern(regexp = "^[\\w\\s\\-.,!?]+$", message = "Invalid characters in title")
    String title,

    @Size(max = 2000) String description,

    @NotNull Priority priority
) {}
```

---

### 5. A05 — Security Misconfiguration (10 min)

**Actuator security — never expose everything in production:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info   # NEVER: "*" in production
  endpoint:
    health:
      show-details: when-authorized   # hide details from public
```

**CORS — be specific, not permissive:**
```java
// VULNERABLE
corsConfig.addAllowedOrigin("*");   // allows any website to make requests

// SECURE
corsConfig.addAllowedOrigin("https://app.yourdomain.com");
corsConfig.addAllowedMethod("GET");
corsConfig.addAllowedMethod("POST");
```

**Remove error details from responses:**
```yaml
server:
  error:
    include-message: never          # don't expose exception messages to clients
    include-stacktrace: never
    include-binding-errors: never
```

---

### 6. A09 — Logging Failures (10 min)

**Never log sensitive data:**
```java
// VULNERABLE
log.info("User login: username={}, password={}", username, password);
log.info("JWT token: {}", token);
log.info("Credit card: {}", cardNumber);

// SECURE
log.info("User login attempt for username={}", username);
log.info("User authenticated successfully: username={}", username);
// Log the attempt, not the credentials
```

**Do log security events:**
```java
@Component
public class SecurityEventLogger implements ApplicationEventPublisher {

    @EventListener
    public void onAuthFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("SECURITY: Authentication failed for user={} from IP={} reason={}",
            event.getAuthentication().getName(),
            ((WebAuthenticationDetails) event.getAuthentication().getDetails()).getRemoteAddress(),
            event.getException().getMessage());
    }

    @EventListener
    public void onAuthSuccess(InteractiveAuthenticationSuccessEvent event) {
        log.info("SECURITY: User authenticated: username={}", event.getAuthentication().getName());
    }
}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is horizontal vs. vertical privilege escalation? Give an example of each.

**Q2.** Why return 404 instead of 403 when a user tries to access a resource they don't own?

**Q3.** How does JPA prevent SQL injection?

**Q4.** Why should you never use `addAllowedOrigin("*")` in a production CORS configuration?

**Q5.** Name 3 things you should ALWAYS log and 3 things you should NEVER log.

---

### Part B — Security Audit (20 min)

Review your Task Manager API for these specific vulnerabilities:

1. **Access control**: Does `GET /api/tasks/{id}` check that the task belongs to the authenticated user? If not, fix it.
2. **Password storage**: Is the password hashed with BCrypt before storing? Verify in `AuthService`.
3. **Secrets**: Are JWT secret and DB password read from environment variables (not hardcoded in code)?
4. **Actuator**: Is `/actuator` restricted in `SecurityConfig`? If not, add `permitAll()` only for `/actuator/health`.
5. **Error responses**: Do your error responses include Java stack traces? Add `server.error.include-stacktrace: never`.

---

### Answers

**A1.** **Horizontal escalation**: accessing another user's data at the same privilege level. Example: Alice accesses Bob's task at `GET /api/tasks/42` even though Alice doesn't own task 42. **Vertical escalation**: accessing functionality above your privilege level. Example: a regular USER role accesses `DELETE /api/admin/users/{id}` which requires ADMIN role. Both are broken access control — horizontal is more common and often overlooked.

**A2.** Returning 403 (Forbidden) reveals that the resource exists — the attacker learns "Task #42 exists, but I can't access it." They can then target the account that owns it. Returning 404 (Not Found) gives no information — the resource may not exist or you may not have access. This is the principle of **security through obscurity** (as a supplementary defense) and is recommended by OWASP for sensitive resource IDs.

**A3.** JPA uses **parameterized queries** (JDBC PreparedStatement under the hood). When you write `@Query("SELECT t FROM Task t WHERE t.title = :title")` and pass `@Param("title")`, the database driver separates the SQL structure from the data. Even if `title` contains `'; DROP TABLE tasks; --`, the driver treats it as a literal string value, not SQL to execute. The SQL structure is fixed — only safe data substitution is allowed.

**A4.** `addAllowedOrigin("*")` allows any website to make cross-origin requests to your API using the browser's credentials (cookies, auth headers). A malicious website at `evil.com` could make requests to your API on behalf of a logged-in user. Specific origins (`https://app.yourdomain.com`) restrict this to only your trusted frontend. Exception: public APIs with no authentication can use `*`.

**A5.** Always log: (1) Security events — login success/failure, permission denials; (2) Significant business events — task created, user registered; (3) System health events — service startup, connection failures. Never log: (1) Passwords or password hashes; (2) JWT tokens or API keys; (3) PII like email addresses, phone numbers, SSNs in detail logs (hash or mask them); (4) Credit card numbers or financial data.
