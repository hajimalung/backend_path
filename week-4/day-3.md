# Week 4 — Day 3: Spring Profiles and Configuration Properties

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Why Profiles? (5 min)

Different environments (dev, test, prod) need different configuration:
- Dev: in-memory H2, debug logging, SQL visible
- Test: in-memory H2, no Flyway migration
- Prod: real PostgreSQL, minimal logging, connection pool tuned

Profiles let you define env-specific config without changing code.

---

### 2. Profile-Specific Configuration Files (15 min)

Spring Boot merges files in this order (later overrides earlier):
1. `application.yml` — base config (always loaded)
2. `application-{profile}.yml` — profile-specific overrides

```
src/main/resources/
├── application.yml          ← shared config
├── application-dev.yml      ← development overrides
├── application-prod.yml     ← production overrides
└── application-test.yml     ← test overrides
```

**application.yml (base — shared):**
```yaml
spring:
  application:
    name: task-manager

app:
  max-tasks: 100
  version: "1.0"

logging:
  level:
    root: WARN
```

**application-dev.yml (development):**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:devdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

logging:
  level:
    com.learn.taskmanager: DEBUG
    org.hibernate.SQL: DEBUG

server:
  port: 8080
```

**application-prod.yml (production):**
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}           # from environment variable
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: validate           # never auto-modify schema in prod!
    show-sql: false                # don't log SQL in prod

server:
  port: ${PORT:8080}               # from env var, default 8080

logging:
  level:
    root: ERROR
    com.learn.taskmanager: INFO
```

**application-test.yml (test):**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
```

---

### 3. Activating Profiles (10 min)

**In `application.yml`:**
```yaml
spring:
  profiles:
    active: dev    # set active profile
```

**Via JVM argument (CI/CD):**
```bash
java -jar app.jar --spring.profiles.active=prod
```

**Via environment variable:**
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```

**In tests with `@ActiveProfiles`:**
```java
@SpringBootTest
@ActiveProfiles("test")
class TaskServiceTest { ... }
```

---

### 4. @ConfigurationProperties — Validated (20 min)

`@ConfigurationProperties` binds a section of YAML to a Java class. Add validation with JSR-303 annotations:

```java
@Component
@ConfigurationProperties(prefix = "app")
@Validated                               // enables Bean Validation on properties
public class AppProperties {

    @NotBlank
    private String version;

    @Min(1) @Max(10000)
    private int maxTasks;

    @Email
    private String adminEmail;

    private Security security = new Security(); // nested config

    // Getters and setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public int getMaxTasks() { return maxTasks; }
    public void setMaxTasks(int maxTasks) { this.maxTasks = maxTasks; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public static class Security {
        private boolean enabled = false;
        private String jwtSecret;
        private long jwtExpirationMs = 3600000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
        public long getJwtExpirationMs() { return jwtExpirationMs; }
        public void setJwtExpirationMs(long jwtExpirationMs) { this.jwtExpirationMs = jwtExpirationMs; }
    }
}
```

**Corresponding YAML:**
```yaml
app:
  version: "1.0"
  max-tasks: 100
  admin-email: admin@example.com
  security:
    enabled: true
    jwt-secret: ${JWT_SECRET}        # from env var
    jwt-expiration-ms: 3600000
```

**Use in a service:**
```java
@Service
public class TaskService {
    private final AppProperties properties;
    private final TaskRepository repository;

    public TaskService(AppProperties properties, TaskRepository repository) {
        this.properties = properties;
        this.repository = repository;
    }

    public TaskResponse create(CreateTaskRequest request) {
        if (repository.count() >= properties.getMaxTasks()) {
            throw new IllegalStateException("Maximum task limit reached: " + properties.getMaxTasks());
        }
        // ... rest of logic
    }
}
```

---

### 5. Environment Variables and Secrets (10 min)

**Never store secrets in code or committed config files.**

```yaml
# application-prod.yml — use placeholders
spring:
  datasource:
    password: ${DB_PASSWORD}          # required — fails to start if not set
  
app:
  security:
    jwt-secret: ${JWT_SECRET:default-insecure-key}  # default if not set (bad for prod!)
```

**Set at runtime:**
```bash
DB_PASSWORD=secret123 JWT_SECRET=my-key java -jar app.jar
```

**In Docker:**
```yaml
# docker-compose.yml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - DB_PASSWORD=secret123
  - JWT_SECRET=super-secret-key
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the load order of Spring Boot config files? Which has highest priority?

**Q2.** How do you activate a profile at runtime without changing `application.yml`?

**Q3.** What does `@Validated` on a `@ConfigurationProperties` class enable?

**Q4.** What does `${DB_PASSWORD}` in a YAML value mean? What happens if that env var is not set?

**Q5.** Why should you use `ddl-auto: validate` or `none` in production instead of `update`?

---

### Part B — Coding Challenge (20 min)

1. Create three config files: `application.yml`, `application-dev.yml`, `application-prod.yml`

2. In base `application.yml`, set app name and a custom `app.welcome-message`

3. In `application-dev.yml`, use H2 in-memory DB, enable H2 console, show SQL

4. Create `AppProperties` class bound to `app` prefix with fields: `welcomeMessage` (String), `maxTasks` (int, min=1, max=500), `debugMode` (boolean)

5. Write a `CommandLineRunner` that:
   - Prints the active profile name: `System.getProperty("spring.profiles.active")` or inject `Environment`
   - Prints the welcome message from `AppProperties`
   - Prints whether debug mode is on

6. Test by running with profile `dev` and verify H2 console is available.

---

### Answers

**A1.** Spring Boot property sources are loaded in priority order (highest first): (1) command-line arguments, (2) environment variables, (3) `application-{profile}.yml`, (4) `application.yml`, (5) default values. Profile-specific files override the base file for matching keys.

**A2.** Via JVM argument: `--spring.profiles.active=prod` or environment variable: `SPRING_PROFILES_ACTIVE=prod`. This overrides whatever is in `application.yml` without changing any file.

**A3.** `@Validated` enables JSR-303 Bean Validation on the configuration class fields. If any `@NotBlank`, `@Min`, `@Email` etc. constraints fail when the app starts, Spring Boot throws a `BindValidationException` and **refuses to start** — fail-fast behavior that prevents running with invalid config.

**A4.** `${DB_PASSWORD}` is a Spring property placeholder that reads from environment variables, JVM system properties, or other property sources. If not set and no default is provided with `:`, the application **fails to start** with a `IllegalArgumentException: Could not resolve placeholder`.

**A5.** `update` uses Hibernate to auto-alter the schema — it can miss complex migrations, cannot drop columns, and may corrupt data. In production, use proper migration tools (Flyway or Liquibase) that are versioned, reversible, and team-reviewable. `validate` just checks the entity model matches the DB and throws an error if they don't — safe and fast.

**Part B Solution:**
```java
// AppProperties.java
@Component
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {
    @NotBlank private String welcomeMessage;
    @Min(1) @Max(500) private int maxTasks = 100;
    private boolean debugMode;

    // getters + setters
}

// application.yml
spring:
  profiles:
    active: dev
app:
  welcome-message: "Welcome to Task Manager"
  max-tasks: 100
  debug-mode: false

// application-dev.yml
spring:
  datasource:
    url: jdbc:h2:mem:devdb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
app:
  debug-mode: true

// Runner
@Component
public class StartupRunner implements CommandLineRunner {
    private final AppProperties props;
    private final Environment env;

    public StartupRunner(AppProperties props, Environment env) {
        this.props = props; this.env = env;
    }

    @Override
    public void run(String... args) {
        System.out.println("Active profiles: " + Arrays.toString(env.getActiveProfiles()));
        System.out.println(props.getWelcomeMessage());
        System.out.println("Debug mode: " + props.isDebugMode());
    }
}
```
