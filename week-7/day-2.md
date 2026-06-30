# Week 7 — Day 2: Creational Design Patterns

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. What Are Design Patterns? (5 min)

Design patterns are proven solutions to recurring design problems. They're a shared vocabulary — when you say "use a Factory Method here", every experienced engineer knows what you mean.

**Three categories:**
- **Creational**: how to create objects (Builder, Factory, Singleton, Prototype, Abstract Factory)
- **Structural**: how objects are composed (Decorator, Adapter, Facade, Proxy)
- **Behavioral**: how objects interact (Strategy, Observer, Command, Template Method)

---

### 2. Builder Pattern (20 min)

**Problem**: a class has many optional fields — its constructor becomes unwieldy.

```java
// Without Builder — ugly, error-prone (which arg is which?)
Task task = new Task("title", null, null, Priority.HIGH, null, null, null, false, owner, null);
```

**Builder Pattern:**
```java
public class TaskBuilder {
    private String title;
    private String description;
    private Priority priority = Priority.MEDIUM;   // sensible default
    private TaskStatus status = TaskStatus.TODO;
    private AppUser owner;
    private LocalDate dueDate;

    public TaskBuilder title(String title) {
        this.title = Objects.requireNonNull(title, "title is required");
        return this;   // fluent API — enable chaining
    }
    public TaskBuilder description(String description) { this.description = description; return this; }
    public TaskBuilder priority(Priority priority) { this.priority = priority; return this; }
    public TaskBuilder owner(AppUser owner) { this.owner = owner; return this; }
    public TaskBuilder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }

    public Task build() {
        if (owner == null) throw new IllegalStateException("owner is required");
        return new Task(title, description, priority, status, owner, dueDate);
    }
}

// Usage — readable, no magic positional arguments
Task task = new TaskBuilder()
    .title("Buy groceries")
    .priority(Priority.HIGH)
    .owner(currentUser)
    .dueDate(LocalDate.now().plusDays(1))
    .build();
```

**Lombok `@Builder`** generates this automatically:
```java
@Builder
@Entity
public class Task {
    @Builder.Default
    private Priority priority = Priority.MEDIUM;
    // ... fields
}

// Usage — same fluent API, zero boilerplate
Task task = Task.builder()
    .title("Buy groceries")
    .priority(Priority.HIGH)
    .owner(user)
    .build();
```

In Spring: `ResponseEntity.ok(body)`, `MockMvc.perform(get("/"))` — both use the Builder pattern.

---

### 3. Factory Method Pattern (15 min)

**Problem**: the creation logic is complex or depends on runtime data, and you don't want the caller to know which concrete class to instantiate.

**Simple Factory (not a GoF pattern, but common):**
```java
public class NotificationFactory {
    public static Notification create(NotificationType type) {
        return switch (type) {
            case EMAIL -> new EmailNotification();
            case SMS   -> new SmsNotification();
            case PUSH  -> new PushNotification();
        };
    }
}

// Caller doesn't need to know which class to use
Notification n = NotificationFactory.create(NotificationType.EMAIL);
n.send(user, message);
```

**Factory Method (proper GoF — subclass decides):**
```java
abstract class NotificationService {
    // Template method that uses the factory method
    public final void notify(User user, String message) {
        Notification notification = createNotification();   // factory method
        notification.send(user, message);
        log(notification, user);
    }

    protected abstract Notification createNotification();   // subclass decides
}

class EmailNotificationService extends NotificationService {
    @Override
    protected Notification createNotification() {
        return new EmailNotification(smtpConfig);
    }
}

class SmsNotificationService extends NotificationService {
    @Override
    protected Notification createNotification() {
        return new SmsNotification(twilioClient);
    }
}
```

**In Spring**: `@Bean` methods are factory methods — they decide how to create beans:
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // factory method
}
```

---

### 4. Singleton Pattern (10 min)

**Intent**: ensure only one instance of a class exists in the application.

**Java Singleton (thread-safe):**
```java
public class AppConfig {
    // Volatile ensures visibility across threads
    private static volatile AppConfig instance;
    private final Map<String, String> settings;

    private AppConfig() {
        settings = loadSettings();
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {   // double-checked locking
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }
}
```

**Best Java Singleton — Enum:**
```java
public enum AppConfig {
    INSTANCE;

    private final Map<String, String> settings = loadSettings();

    public String get(String key) { return settings.get(key); }
}
// Thread-safe, lazy, serialization-safe — by Java spec
```

**In Spring**: every `@Bean` is a singleton by default — Spring manages the single instance for you. You almost never write a Singleton pattern manually in Spring apps.

```java
@Service  // by default: singleton scope
class TaskService { ... }

@Bean     // singleton — Spring returns same instance for every injection
public TaskService taskService() { return new TaskService(...); }
```

---

### 5. Prototype Pattern (10 min)

**Intent**: create new objects by copying (cloning) an existing instance.

```java
public class TaskTemplate implements Cloneable {
    private String title;
    private Priority priority;
    private List<String> tags = new ArrayList<>();

    @Override
    public TaskTemplate clone() {
        try {
            TaskTemplate clone = (TaskTemplate) super.clone();
            clone.tags = new ArrayList<>(this.tags);  // deep copy mutable fields!
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}

// Usage
TaskTemplate bugTemplate = new TaskTemplate("BUG-", Priority.HIGH, List.of("bug", "urgent"));
TaskTemplate newBug = bugTemplate.clone();
newBug.setTitle("BUG-" + bugId);  // customize the copy
```

**In Spring**: `@Scope("prototype")` creates a new bean instance per injection (opposite of singleton):
```java
@Component
@Scope("prototype")
class RequestContext { ... }    // new instance each time it's injected
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What problem does the Builder pattern solve? When would you prefer `@Builder` over a constructor?

**Q2.** What is the difference between a Factory Method and a constructor?

**Q3.** Why is the Enum-based Singleton considered the best approach in Java?

**Q4.** Every `@Bean` in Spring is already which pattern?

**Q5.** When would you use `@Scope("prototype")` instead of the default singleton in Spring?

---

### Part B — Coding Challenge (20 min)

1. Add a `TaskBuilder` to your Task Manager project (or use Lombok's `@Builder`).
2. Create a `NotificationFactory` that takes a `NotificationType` enum (`EMAIL`, `PUSH`, `IN_APP`) and returns the appropriate `Notification` implementation.
3. Each `Notification` should have `send(String recipient, String message)` method.
4. Write a unit test for the factory: verify `NotificationFactory.create(EMAIL)` returns an `EmailNotification` instance.

---

### Answers

**A1.** Builder solves the "telescoping constructor" problem — when a class has many fields (especially optional ones), constructors become unreadable (which position is `null`? which is the description vs title?). Builder makes the code self-documenting and only requires you to set what you need. Use `@Builder` when you have 4+ fields and want Lombok to generate the boilerplate.

**A2.** A constructor is always tied to one specific class — you know exactly what you're creating. A Factory Method hides the concrete type behind an abstraction — the caller asks for "a Notification" and gets whatever the factory decides to create (Email, SMS, Push). This decouples creation logic from usage, enabling the concrete type to vary at runtime based on config or input.

**A3.** Java guarantees that enum values are instantiated exactly once by the class loader — it's inherently thread-safe. It handles serialization automatically (unlike a manual singleton which needs `readResolve()`). It's immune to reflection-based instantiation attacks (unlike `new AppConfig()` via reflection). It's also the most concise implementation.

**A4.** Singleton pattern. Spring's `ApplicationContext` manages the lifecycle of all beans and returns the same instance for every injection point that depends on a given bean type (the default `singleton` scope).

**A5.** Use `@Scope("prototype")` when: (1) the bean holds **mutable request-specific state** that should not be shared; (2) the bean represents a **non-thread-safe** resource that can't be reused; (3) each consumer needs its **own independent copy** (e.g., a builder object, a stateful parser). Most Spring beans should be stateless singletons — prototype scope is the exception, not the rule.
