# Week 3 — Day 2: Spring Boot Auto-Configuration + application.yml

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. How Auto-Configuration Works (10 min)

`@SpringBootApplication` is shorthand for three annotations:
- `@Configuration` — marks this as a Spring config class
- `@EnableAutoConfiguration` — Spring Boot magic: scans classpath and auto-configures beans
- `@ComponentScan` — scans for `@Component`, `@Service`, etc. in the current package

```java
@SpringBootApplication  // equivalent to the 3 annotations above
public class TaskApp {
    public static void main(String[] args) {
        SpringApplication.run(TaskApp.class, args);
    }
}
```

**How auto-config works:**
1. Spring Boot checks what's on the classpath (e.g., `spring-boot-starter-web` → includes Tomcat, Jackson)
2. Reads `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` inside starter JARs
3. Applies configuration **only if** conditions are met (`@ConditionalOnClass`, `@ConditionalOnMissingBean`, etc.)

Example: If you have `spring-boot-starter-data-jpa` on the classpath AND no custom `DataSource` bean, Spring auto-creates a `DataSource` for you.

---

### 2. application.yml / application.properties (20 min)

The primary configuration file. Spring Boot loads it automatically.

**application.yml** (preferred — more readable than .properties):
```yaml
# Server config
server:
  port: 8080
  servlet:
    context-path: /api     # all endpoints prefixed with /api

# Spring configuration
spring:
  application:
    name: task-manager

  # DataSource (for JPA — Week 3 Day 5)
  datasource:
    url: jdbc:postgresql://localhost:5432/taskdb
    username: postgres
    password: secret
    driver-class-name: org.postgresql.Driver

  # JPA settings
  jpa:
    hibernate:
      ddl-auto: update        # auto-create/update schema
    show-sql: true            # log SQL queries
    properties:
      hibernate:
        format_sql: true

# Custom properties
app:
  max-tasks: 100
  default-priority: MEDIUM
  admin-email: admin@example.com

# Logging
logging:
  level:
    root: INFO
    com.learn.taskmanager: DEBUG   # debug level for your package
```

---

### 3. Reading Configuration in Code (20 min)

**Way 1: `@Value` — inject a single property**
```java
@Service
public class TaskService {

    @Value("${app.max-tasks}")
    private int maxTasks;

    @Value("${app.default-priority:MEDIUM}")  // default value after :
    private String defaultPriority;

    @Value("${app.admin-email}")
    private String adminEmail;

    public void printConfig() {
        System.out.println("Max tasks: " + maxTasks);
        System.out.println("Default priority: " + defaultPriority);
    }
}
```

**Way 2: `@ConfigurationProperties` — bind a whole group (recommended)**
```java
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private int maxTasks;
    private String defaultPriority;
    private String adminEmail;

    // Getters and setters required (or use with Lombok @Data)
    public int getMaxTasks() { return maxTasks; }
    public void setMaxTasks(int maxTasks) { this.maxTasks = maxTasks; }

    public String getDefaultPriority() { return defaultPriority; }
    public void setDefaultPriority(String defaultPriority) { this.defaultPriority = defaultPriority; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
}
```

```java
@Service
public class TaskService {
    private final AppProperties appProperties;

    public TaskService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void doSomething() {
        int max = appProperties.getMaxTasks(); // type-safe access
    }
}
```

Add this to enable `@ConfigurationProperties`:
```java
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class TaskApp { ... }
```

---

### 4. Spring Boot Actuator + Info Endpoint (10 min)

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

Now visit:
- `GET /actuator/health` — app health status
- `GET /actuator/info` — app info
- `GET /actuator/metrics` — app metrics

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What three annotations does `@SpringBootApplication` combine?

**Q2.** What is the difference between using `@Value` and `@ConfigurationProperties` for reading configuration?

**Q3.** Where does Spring Boot look for the `application.yml` file by default?

**Q4.** What does `spring.jpa.hibernate.ddl-auto: update` do?

**Q5.** How do you set a default value in a `@Value` annotation?

---

### Part B — Coding Challenge (20 min)

1. Add an `application.yml` to your Spring Boot project with:
   - Server port: `9090`
   - App name: `task-api`
   - Custom property group `app:` with `welcome-message: "Welcome to Task API!"` and `version: "1.0"`

2. Create a `@ConfigurationProperties` class `AppConfig` bound to the `app` prefix.

3. Create a `WelcomeController` (just `@RestController`, no routing yet) with a method `welcome()` that injects `AppConfig` and returns the welcome message and version as a combined string.

4. Create a `CommandLineRunner` bean that prints the welcome message at startup using `AppConfig`.

5. Verify the app starts on port `9090` and the runner prints the message.

---

### Answers

**A1.** `@Configuration` (marks as config class), `@EnableAutoConfiguration` (triggers Spring Boot auto-config based on classpath), and `@ComponentScan` (scans current package for Spring-managed beans).

**A2.** `@Value` injects a single property into a field — simple but not type-safe and not grouped. `@ConfigurationProperties` binds an entire section of config to a class with proper types, validation support, and IDE auto-complete. Use `@ConfigurationProperties` for anything beyond a few simple values.

**A3.** `src/main/resources/application.yml` (or `application.properties`). Spring Boot also checks the classpath root and several other locations. Profile-specific files like `application-dev.yml` override the default.

**A4.** Hibernate will compare your entity classes to the existing DB schema and **automatically alter the schema** (add columns, create tables) to match. Options: `none` (no action), `validate` (validate only), `update` (alter), `create` (recreate on start), `create-drop` (drop on exit). Use `update` for dev, `validate` or `none` for production.

**A5.** Use the `:` syntax: `@Value("${property.name:defaultValue}")`. If `property.name` is not found in any config source, `defaultValue` is used. Example: `@Value("${app.timeout:5000}")` defaults to 5000 if not configured.

**Part B Solution:**
```yaml
# application.yml
server:
  port: 9090
spring:
  application:
    name: task-api
app:
  welcome-message: "Welcome to Task API!"
  version: "1.0"
```

```java
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private String welcomeMessage;
    private String version;

    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}

@RestController
public class WelcomeController {
    private final AppConfig appConfig;

    public WelcomeController(AppConfig appConfig) { this.appConfig = appConfig; }

    public String welcome() {
        return appConfig.getWelcomeMessage() + " v" + appConfig.getVersion();
    }
}

@Component
public class StartupRunner implements CommandLineRunner {
    private final AppConfig appConfig;
    public StartupRunner(AppConfig appConfig) { this.appConfig = appConfig; }

    @Override
    public void run(String... args) {
        System.out.println(appConfig.getWelcomeMessage());
    }
}
```
