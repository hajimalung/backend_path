# Week 7 — Day 1: SOLID Principles in Java/Spring

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Why SOLID? (5 min)

SOLID principles are design guidelines that make code:
- Easier to change (one reason to change = one place to change)
- Easier to test (small, focused units)
- Easier to extend (open for new features, closed for modification)

They're not rules — they're trade-offs. Apply them where they add value; don't over-engineer trivial code.

---

### 2. S — Single Responsibility Principle (10 min)

> A class should have only one reason to change.

**Violation:**
```java
class TaskService {
    public void save(Task task) { /* DB logic */ }
    public void sendEmail(Task task) { /* email logic */ }  // ← wrong place
    public String exportToCsv(List<Task> tasks) { /* CSV logic */ }  // ← wrong place
}
```

If the email provider changes OR the CSV format changes OR the DB schema changes → all 3 touch `TaskService`.

**Fixed:**
```java
class TaskService {
    void save(Task task) { /* only DB */ }
}
class NotificationService {
    void sendTaskCreatedEmail(Task task) { /* only email */ }
}
class TaskExporter {
    String toCsv(List<Task> tasks) { /* only export */ }
}
```

In Spring: `@Service` for business logic, `@Repository` for data, `@Component` for utilities. This layering naturally enforces SRP.

---

### 3. O — Open/Closed Principle (10 min)

> Open for extension, closed for modification.

**Violation:**
```java
class PriorityCalculator {
    int calculate(Task task) {
        if (task.getType().equals("BUG")) return 10;
        else if (task.getType().equals("FEATURE")) return 5;
        else if (task.getType().equals("DEBT")) return 3;
        // Every new type requires modifying this class
        return 0;
    }
}
```

**Fixed — Strategy Pattern:**
```java
interface PriorityStrategy {
    int calculate(Task task);
}

class BugPriorityStrategy implements PriorityStrategy {
    public int calculate(Task task) { return 10; }
}

class FeaturePriorityStrategy implements PriorityStrategy {
    public int calculate(Task task) { return 5; }
}

// New type? Add a new class — don't touch existing ones.
class TechDebtPriorityStrategy implements PriorityStrategy {
    public int calculate(Task task) { return 3; }
}
```

Spring's `@Component` + injection makes this trivial — inject a `List<PriorityStrategy>` and Spring auto-discovers all implementations.

---

### 4. L — Liskov Substitution Principle (10 min)

> Subclasses must be substitutable for their base classes without breaking the program.

**Violation:**
```java
class Bird {
    void fly() { System.out.println("flying"); }
}
class Penguin extends Bird {
    @Override
    void fly() { throw new UnsupportedOperationException("Penguins can't fly!"); }
}

// Breaks LSP — code expecting a Bird crashes with Penguin
Bird bird = new Penguin();
bird.fly(); // ← throws!
```

**Fixed — redesign the hierarchy:**
```java
interface Bird { void eat(); }
interface FlyingBird extends Bird { void fly(); }
class Sparrow implements FlyingBird { ... }
class Penguin implements Bird { ... }  // can't fly, but that's fine
```

In Spring: if `TaskRepository.findById()` is expected to return `Optional<Task>`, every implementation must return `Optional` — not throw, not return null.

---

### 5. I — Interface Segregation Principle (10 min)

> Don't force clients to depend on interfaces they don't use.

**Violation:**
```java
interface UserOperations {
    UserDto getUser(Long id);
    void createUser(CreateUserRequest req);
    void deleteUser(Long id);
    void exportUsers(OutputStream stream);
    void sendBulkEmail(String subject, String body);
}
// TaskService only needs getUser, but depends on the whole interface
```

**Fixed — split into focused interfaces:**
```java
interface UserReader {
    UserDto getUser(Long id);
}
interface UserWriter {
    void createUser(CreateUserRequest req);
    void deleteUser(Long id);
}
interface UserNotifier {
    void sendBulkEmail(String subject, String body);
}
// TaskService only depends on UserReader
```

In Spring: Feign client interfaces are naturally segregated — each client interface covers one service's public API.

---

### 6. D — Dependency Inversion Principle (10 min)

> Depend on abstractions, not concrete implementations.

**Violation:**
```java
class TaskService {
    private PostgresTaskRepository repository = new PostgresTaskRepository();  // concrete!
}
// Can't swap to H2 for tests without changing TaskService
```

**Fixed — depend on the interface:**
```java
class TaskService {
    private final TaskRepository repository;  // interface — Spring's JpaRepository

    TaskService(TaskRepository repository) {  // injected — Spring resolves at runtime
        this.repository = repository;
    }
}
// In tests: inject an H2-backed or mock repository without touching TaskService
```

Spring DI is literally dependency inversion implemented — you've been doing this all along! `@Service` classes depend on `@Repository` interfaces, not PostgresQL implementations.

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** How does Spring Boot's layered architecture (Controller → Service → Repository) relate to SRP?

**Q2.** Which SOLID principle does the Strategy Pattern implement? Why?

**Q3.** Give an example from your own Task Manager project that violates LSP.

**Q4.** What is the problem with a "fat interface"? How does ISP fix it?

**Q5.** Why is constructor injection in Spring considered better than field injection (`@Autowired` on a field) from a SOLID standpoint?

---

### Part B — Refactoring Challenge (20 min)

Identify and fix SOLID violations in this code:

```java
@Service
public class TaskManager {
    @Autowired
    private EmailSender emailSender;

    public void createTask(String title, String description, String priority, String userEmail) {
        // Validation
        if (title == null || title.isBlank()) throw new IllegalArgumentException("blank title");

        // Persistence
        Task task = new Task(title, description, Priority.valueOf(priority));
        new JdbcTemplate(DataSourceBuilder.create().build()).update(
            "INSERT INTO tasks (title, description, priority) VALUES (?, ?, ?)",
            title, description, priority);

        // Notification
        emailSender.send(userEmail, "Task created: " + title, "Your task is ready.");

        // Analytics
        System.out.println("Task count: " + countTasks());
    }

    private int countTasks() { return 42; }
}
```

1. Identify which SOLID principles are violated and why.
2. Sketch a refactored version with proper separation.

---

### Answers

**A1.** Each layer has one reason to change: Controllers change when the HTTP API contract changes; Services change when business rules change; Repositories change when data storage changes. This natural separation enforces SRP — a business rule change doesn't touch the controller; a DB schema change doesn't touch the service.

**A2.** Open/Closed Principle. By defining a `PriorityStrategy` interface, you open the system for extension (add `NewTaskTypeStrategy`) without modifying existing code (closed for modification). The algorithm is a "plug" — swap implementations without changing the code that uses the strategy.

**A3.** Example: If you had `TaskRepository extends JpaRepository` and added a method `findOrThrow(Long id)` that throws `RuntimeException` instead of returning `Optional`, and a subclass returns `null` instead — any code expecting an `Optional` would get a `NullPointerException`. Proper LSP: all implementations of `findById` return `Optional<Task>` consistently.

**A4.** A fat interface forces every implementor to provide implementations for methods they don't need. In test mocks, you'd have to stub 10 methods when you only use 2. ISP splits fat interfaces into role-specific ones — each class implements only the interface it actually needs. This reduces coupling and makes mocking/testing simpler.

**A5.** Constructor injection makes dependencies explicit — you can see exactly what a class needs without reading its body. It enables `final` fields (immutability). Most importantly: it enables **testing without Spring** — you can `new TaskService(mockRepository)` in a unit test. Field injection with `@Autowired` hides dependencies and forces reflection-based injection (Spring magic) even in tests, making `TaskService` untestable in isolation.

**Part B — Violations:**
1. **SRP**: `TaskManager` does validation, persistence, notification, analytics — 4 reasons to change
2. **DIP**: creates `JdbcTemplate` inline (concrete implementation, not injected)
3. **OCP**: hard-coded `Priority.valueOf(priority)` — adding a new priority requires changing this class

**Refactored:**
```java
@Service
public class TaskService {           // only business logic
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public TaskResponse create(CreateTaskRequest request, String userEmail) {
        Task task = taskRepository.save(new Task(request.title(), request.description(),
            Priority.valueOf(request.priority())));
        notificationService.notifyCreated(task, userEmail);  // SRP — delegated
        return TaskResponse.from(task);
    }
}

@Service class NotificationService { ... }   // SRP
@Repository interface TaskRepository { ... } // DIP — interface, not JdbcTemplate
// Validation moved to @Valid annotations on request DTO
```
