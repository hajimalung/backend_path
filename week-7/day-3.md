# Week 7 — Day 3: Behavioral Design Patterns

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Strategy Pattern (20 min)

**Intent**: define a family of algorithms, put each in a separate class, and make them interchangeable. The context uses the strategy without knowing which one.

**Problem**: multiple sorting/calculation approaches that switch based on context:
```java
// Violation — changes every time a new notification type is added
class NotificationService {
    void send(User user, String message, String type) {
        if (type.equals("email"))     sendEmail(user, message);
        else if (type.equals("sms"))  sendSms(user, message);
        else if (type.equals("push")) sendPush(user, message);
    }
}
```

**Strategy Pattern:**
```java
// Strategy interface
public interface NotificationStrategy {
    void send(String recipient, String message);
    NotificationType getType();
}

// Concrete strategies
@Component
public class EmailNotificationStrategy implements NotificationStrategy {
    public void send(String recipient, String message) {
        System.out.printf("[EMAIL] To: %s | Message: %s%n", recipient, message);
    }
    public NotificationType getType() { return NotificationType.EMAIL; }
}

@Component
public class SmsNotificationStrategy implements NotificationStrategy {
    public void send(String recipient, String message) {
        System.out.printf("[SMS] To: %s | Message: %s%n", recipient, message);
    }
    public NotificationType getType() { return NotificationType.SMS; }
}

// Context — uses a strategy, doesn't know which one
@Service
public class NotificationService {
    private final Map<NotificationType, NotificationStrategy> strategies;

    // Spring injects ALL NotificationStrategy beans — zero config
    public NotificationService(List<NotificationStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(NotificationStrategy::getType, Function.identity()));
    }

    public void notify(User user, String message, NotificationType type) {
        NotificationStrategy strategy = strategies.get(type);
        if (strategy == null) throw new IllegalArgumentException("Unknown type: " + type);
        strategy.send(user.getEmail(), message);
    }
}
// Add a new notification type? Just add a new @Component — zero changes to existing code!
```

---

### 2. Observer Pattern (15 min)

**Intent**: when one object changes state, all dependents are notified automatically. Also called publish-subscribe.

**Spring Events (built-in Observer):**
```java
// Event (what happened)
public class TaskCompletedEvent {
    private final Task task;
    private final String ownerUsername;

    public TaskCompletedEvent(Task task, String ownerUsername) {
        this.task = task;
        this.ownerUsername = ownerUsername;
    }
    public Task getTask() { return task; }
    public String getOwnerUsername() { return ownerUsername; }
}

// Publisher (the observable)
@Service
public class TaskService {
    private final ApplicationEventPublisher eventPublisher;

    public TaskResponse markComplete(Long id, String username) {
        Task task = findTaskByIdAndOwner(id, username);
        task.setStatus(TaskStatus.DONE);
        Task saved = taskRepository.save(task);

        // Publish event — TaskService doesn't know who listens
        eventPublisher.publishEvent(new TaskCompletedEvent(saved, username));
        return TaskResponse.from(saved);
    }
}

// Observers — any number can listen without TaskService knowing
@Component
public class NotificationEventListener {
    @EventListener
    public void onTaskCompleted(TaskCompletedEvent event) {
        log.info("[EMAIL] Congrats! Task '{}' completed!", event.getTask().getTitle());
    }
}

@Component
public class AnalyticsEventListener {
    @EventListener
    public void onTaskCompleted(TaskCompletedEvent event) {
        log.info("[ANALYTICS] User {} completed task {}", event.getOwnerUsername(), event.getTask().getId());
    }
}
```

**Async events** (non-blocking — listener runs in a different thread):
```java
@Async
@EventListener
public void onTaskCompleted(TaskCompletedEvent event) { ... }
// Requires @EnableAsync on a @Configuration class
```

**Difference from Kafka**: Spring events are in-process (same JVM). Kafka events cross service boundaries. Use Spring events within a service; use Kafka across services.

---

### 3. Decorator Pattern (15 min)

**Intent**: add behavior to an object dynamically without modifying its class.

```java
// Core interface
public interface TaskRepository extends JpaRepository<Task, Long> { ... }

// Caching decorator (wraps the real repo)
@Primary  // Spring injects this one instead of the real repo
@Component
public class CachingTaskRepository implements TaskRepository {

    private final TaskRepository delegate;     // the real implementation
    private final Map<Long, Task> cache = new ConcurrentHashMap<>();

    public CachingTaskRepository(@Qualifier("simpleJpaRepository") TaskRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<Task> findById(Long id) {
        return Optional.ofNullable(
            cache.computeIfAbsent(id, k -> delegate.findById(k).orElse(null))
        );
    }

    @Override
    public Task save(Task task) {
        Task saved = delegate.save(task);
        cache.put(saved.getId(), saved);
        return saved;
    }
}
```

**In Spring**: `@Transactional`, `@Cacheable`, `@Async` are all decorators implemented via AOP (Spring wraps your bean in a proxy that adds behavior).

```java
@Service
public class TaskService {
    @Cacheable("tasks")           // Spring wraps this method in a caching decorator
    public TaskResponse findById(Long id) {
        return TaskResponse.from(taskRepository.findById(id).orElseThrow());
    }
}
```

---

### 4. Command Pattern (10 min)

**Intent**: encapsulate a request as an object — supports queuing, undo, logging.

```java
// Command interface
public interface TaskCommand {
    void execute();
    void undo();
}

// Concrete commands
public class CreateTaskCommand implements TaskCommand {
    private final TaskRepository taskRepository;
    private final Task task;
    private Long savedId;

    public void execute() {
        Task saved = taskRepository.save(task);
        this.savedId = saved.getId();
    }

    public void undo() {
        if (savedId != null) taskRepository.deleteById(savedId);
    }
}

// Invoker — executes and tracks commands for undo
public class TaskCommandInvoker {
    private final Deque<TaskCommand> history = new ArrayDeque<>();

    public void execute(TaskCommand command) {
        command.execute();
        history.push(command);
    }

    public void undo() {
        if (!history.isEmpty()) history.pop().undo();
    }
}
```

In Spring: Spring's `@Transactional` rollback is conceptually a command pattern — each transaction can be rolled back (undone).

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** How does Spring's DI mechanism make the Strategy Pattern trivially easy to implement?

**Q2.** What is the difference between Spring application events and Kafka events? When do you use each?

**Q3.** What does `@Primary` do in the Decorator pattern example?

**Q4.** How are `@Transactional`, `@Cacheable`, and `@Async` related to the Decorator pattern?

**Q5.** What is the benefit of the Command pattern over calling methods directly?

---

### Part B — Coding Challenge (20 min)

Implement the Strategy Pattern for task priority scoring in your project:

```java
public interface PriorityScoreStrategy {
    int score(Task task);
    Priority applicableTo();
}
```

1. Create `HighPriorityScoreStrategy` (returns 100), `MediumPriorityScoreStrategy` (50), `LowPriorityScoreStrategy` (10).
2. Create `PriorityScoreService` that takes `List<PriorityScoreStrategy>` and builds a map.
3. Add a method `int getScore(Task task)` that delegates to the right strategy.
4. Write a unit test: inject all 3 strategies, verify `getScore` returns correct values.

---

### Answers

**A1.** Spring automatically discovers all `@Component` beans that implement a given interface and injects them as a `List<InterfaceName>`. You can then collect them into a `Map<Type, Strategy>` in the constructor. Adding a new strategy means adding one `@Component` class — no changes to the context, no factory switches, no config updates. Open/Closed Principle + Strategy Pattern powered by Spring DI.

**A2.** **Spring application events** are in-process (same JVM, same service) — synchronous by default, fast, no infrastructure needed. Use within a single service to decouple layers (service → listeners). **Kafka events** cross service boundaries — persistent, reliable, consumed by different microservices independently, require Kafka infrastructure. Use when other services need to react. Rule of thumb: application events within a service, Kafka between services.

**A3.** `@Primary` tells Spring to use this bean when there are multiple implementations of the same interface. Without `@Primary`, Spring wouldn't know whether to inject the `CachingTaskRepository` or the real JPA repository when someone injects `TaskRepository`. With `@Primary`, the decorator (caching wrapper) is injected everywhere, which internally delegates to the real repository.

**A4.** All three are **decorator patterns implemented via AOP proxy**. When you annotate a method with `@Transactional`, Spring generates a proxy class that wraps your bean. The proxy adds transaction management logic before/after your method call — without you modifying your class. Same for `@Cacheable` (adds caching wrapper) and `@Async` (executes in a thread pool wrapper). This is the Decorator pattern applied transparently.

**A5.** Command pattern advantages: (1) **Undo/redo**: commands can be reversed by implementing `undo()`; (2) **Queuing**: commands are objects — store them in a queue, execute later; (3) **Logging/auditing**: log what command was executed, with what parameters; (4) **Compositing**: combine commands into macros; (5) **Decoupling**: the invoker doesn't need to know how the command works, just `execute()`.
