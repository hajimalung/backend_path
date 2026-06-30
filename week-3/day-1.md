# Week 3 — Day 1: Spring IoC and Dependency Injection

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. What is Spring IoC? (10 min)

**IoC = Inversion of Control**: Instead of your code creating its own dependencies, the framework creates and injects them for you.

Without IoC:
```java
// You create and wire everything manually
public class OrderService {
    private PaymentService paymentService = new PaymentService(); // tight coupling!
    private EmailService emailService = new EmailService();       // hard to test!
}
```

With IoC (Spring):
```java
@Service
public class OrderService {
    private final PaymentService paymentService; // Spring injects this
    private final EmailService emailService;

    public OrderService(PaymentService paymentService, EmailService emailService) {
        this.paymentService = paymentService;
        this.emailService   = emailService;
    }
}
```

The **Spring IoC Container** (ApplicationContext) manages the lifecycle of objects called **beans**.

---

### 2. Core Annotations (20 min)

Spring uses annotations to identify which classes it should manage:

```java
// @Component — generic Spring-managed bean
@Component
public class AuditLogger {
    public void log(String message) {
        System.out.println("[AUDIT] " + message);
    }
}

// @Service — for business logic layer (same as @Component, better semantics)
@Service
public class UserService {
    public String getUser(int id) { return "User " + id; }
}

// @Repository — for data access layer (same as @Component + exception translation)
@Repository
public class UserRepository {
    public String findById(int id) { return "Alice"; }
}

// @Controller / @RestController — for web layer
@RestController
public class UserController { ... }

// @Configuration — class that declares beans manually
@Configuration
public class AppConfig {

    @Bean  // Spring will call this method and manage the returned object
    public AuditLogger auditLogger() {
        return new AuditLogger();
    }
}
```

**Bean naming**: by default, Spring uses the class name with lowercase first letter as the bean name. `UserService` → `"userService"`.

---

### 3. Dependency Injection — 3 Ways (20 min)

#### Constructor Injection (Recommended)
```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    private final InventoryService inventoryService;

    // Spring sees one constructor — auto-injects (no @Autowired needed in Spring 4.3+)
    public OrderService(PaymentService paymentService, InventoryService inventoryService) {
        this.paymentService   = paymentService;
        this.inventoryService = inventoryService;
    }
}
```
**Why constructor injection is best:**
- Fields are `final` → immutable, thread-safe
- All dependencies are visible at construction time
- Easier to test (just call constructor in tests)
- Spring detects circular dependencies early

#### Field Injection (Not recommended — for learning only)
```java
@Service
public class OrderService {
    @Autowired                          // Spring injects directly into field via reflection
    private PaymentService paymentService; // can't be final, hard to test
}
```

#### Setter Injection (For optional dependencies)
```java
@Service
public class ReportService {
    private EmailService emailService;

    @Autowired(required = false)        // optional — won't fail if no bean found
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
}
```

---

### 4. The ApplicationContext (10 min)

```java
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

// Without Spring Boot (pure Spring — for understanding)
@Configuration
@ComponentScan("com.example")   // scan this package for @Component etc.
public class AppConfig { }

ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);

UserService userService = ctx.getBean(UserService.class);
userService.getUser(1);

// With Spring Boot — ApplicationContext is created automatically
// @SpringBootApplication scans the package it's in and all sub-packages
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

**Spring Boot setup — add to `pom.xml`:**
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

Or use https://start.spring.io to generate a project (recommended for Week 3).

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What does IoC stand for, and what problem does it solve?

**Q2.** What is the difference between `@Component`, `@Service`, and `@Repository`?

**Q3.** Why is constructor injection preferred over field injection?

**Q4.** What does `@ComponentScan("com.example")` do?

**Q5.** What will happen at startup if Spring cannot find a bean to inject into a required constructor parameter?

---

### Part B — Coding Challenge (20 min)

Create a small Spring Boot application (use https://start.spring.io with just the `Spring Web` dependency):

1. Create `GreetingService` with a method `String greet(String name)` that returns `"Hello, [name]! Welcome to Spring Boot."` Annotate it as a `@Service`.

2. Create `WelcomeRunner` that implements `CommandLineRunner` (Spring calls its `run()` method on startup). Inject `GreetingService` via constructor injection. In `run()`, call greet with 3 different names and print the results.

3. Run the application and confirm you see the greetings in the console.

4. Create a second service `UpperCaseGreetingService` that also returns a greeting but in UPPERCASE. Use `@Primary` on one of them, then inject using the interface.

---

### Answers

**A1.** Inversion of Control. Normally, code creates its own dependencies (`new Service()`). IoC inverts this — the framework creates and injects dependencies. This decouples classes, making them easier to test and change.

**A2.** Technically they are all aliases for `@Component` and work the same way. Semantically: `@Service` marks business logic, `@Repository` marks data access (and adds exception translation for DB exceptions), `@Component` is the generic default. Using the right annotation improves readability and allows framework features specific to each layer.

**A3.** Constructor injection: (1) allows fields to be `final` (immutable), (2) all dependencies are visible at construction time — making the class's requirements explicit, (3) easier unit testing (just `new MyService(mockDep)`), (4) Spring detects circular dependencies at startup instead of at runtime.

**A4.** Tells Spring to scan the `com.example` package (and all sub-packages) for classes annotated with `@Component`, `@Service`, `@Repository`, `@Controller`, etc., and register them as beans in the ApplicationContext.

**A5.** The application fails to start with a `NoSuchBeanDefinitionException` or `UnsatisfiedDependencyException`. Spring's startup fail-fast behavior catches missing dependencies before your app handles any requests.

**Part B Solution:**
```java
@Service
public class GreetingService {
    public String greet(String name) {
        return "Hello, " + name + "! Welcome to Spring Boot.";
    }
}

@Component
public class WelcomeRunner implements CommandLineRunner {
    private final GreetingService greetingService;

    public WelcomeRunner(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @Override
    public void run(String... args) {
        System.out.println(greetingService.greet("Alice"));
        System.out.println(greetingService.greet("Bob"));
        System.out.println(greetingService.greet("Charlie"));
    }
}
```
