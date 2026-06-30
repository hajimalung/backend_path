# Week 3 — Day 4: Spring Data JPA — Entities and Repositories

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. JPA Fundamentals (10 min)

JPA (Java Persistence API) is a specification for mapping Java objects to relational database tables. **Hibernate** is the most popular implementation.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>  <!-- in-memory DB for learning -->
</dependency>
```

```yaml
# application.yml — H2 in-memory for development
spring:
  datasource:
    url: jdbc:h2:mem:taskdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true        # visit http://localhost:8080/h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop  # create schema on start, drop on exit
    show-sql: true
```

---

### 2. Defining Entities (20 min)

```java
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity                           // marks this class as a JPA entity (maps to a table)
@Table(name = "tasks")            // optional — defaults to class name lowercase
public class Task {

    @Id                           // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto-increment
    private Long id;

    @Column(nullable = false, length = 100) // NOT NULL, max 100 chars in DB
    private String title;

    @Column(columnDefinition = "TEXT")       // TEXT type in DB
    private String description;

    @Enumerated(EnumType.STRING)   // store as "TODO", not 0, 1, 2
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // JPA requires a no-arg constructor
    protected Task() {}

    public Task(String title, String description, Priority priority) {
        this.title       = title;
        this.description = description;
        this.priority    = priority;
        this.status      = TaskStatus.TODO;
    }

    // Lifecycle callbacks
    @PrePersist     // called before INSERT
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate      // called before UPDATE
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId()              { return id; }
    public String getTitle()         { return title; }
    public String getDescription()   { return description; }
    public TaskStatus getStatus()    { return status; }
    public Priority getPriority()    { return priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setTitle(String title)           { this.title = title; }
    public void setDescription(String desc)      { this.description = desc; }
    public void setStatus(TaskStatus status)     { this.status = status; }
    public void setPriority(Priority priority)   { this.priority = priority; }
}
```

---

### 3. Spring Data JPA Repositories (20 min)

Spring Data auto-generates repository implementations from method names:

```java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Spring Data derives the query from the method name!
    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(Priority priority);

    List<Task> findByStatusAndPriority(TaskStatus status, Priority priority);

    List<Task> findByTitleContainingIgnoreCase(String keyword); // LIKE %keyword%

    List<Task> findByStatusOrderByCreatedAtDesc(TaskStatus status);

    long countByStatus(TaskStatus status);

    boolean existsByTitle(String title);

    // Delete by field
    void deleteByStatus(TaskStatus status);
}
```

**What `JpaRepository<Task, Long>` gives you for free:**
- `save(entity)` — INSERT or UPDATE
- `findById(id)` → `Optional<Task>`
- `findAll()` → `List<Task>`
- `findAll(Pageable)` → `Page<Task>`
- `deleteById(id)`
- `count()`
- `existsById(id)`

---

### 4. Service Layer with Repository (10 min)

```java
@Service
@Transactional          // all public methods run in a transaction by default
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<TaskResponse> findAll() {
        return taskRepository.findAll().stream()
            .map(TaskResponse::from)
            .collect(Collectors.toList());
    }

    public Optional<TaskResponse> findById(Long id) {
        return taskRepository.findById(id).map(TaskResponse::from);
    }

    public TaskResponse create(CreateTaskRequest request) {
        Task task = new Task(request.title(), request.description(), request.priority());
        Task saved = taskRepository.save(task);
        return TaskResponse.from(saved);
    }

    public TaskResponse markDone(Long id) {
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
        task.setStatus(TaskStatus.DONE);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)  // optimization for read-only queries
    public List<TaskResponse> findByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
            .map(TaskResponse::from)
            .toList();
    }

    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }
}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What does `@Entity` do? What does JPA require from every entity class?

**Q2.** What is the difference between `EnumType.STRING` and `EnumType.ORDINAL` in `@Enumerated`? Which should you use?

**Q3.** How does Spring Data JPA derive a query from the method name `findByStatusAndPriority`?

**Q4.** What does `@Transactional` do? What happens if a `RuntimeException` is thrown inside a transactional method?

**Q5.** What does `@PrePersist` do?

---

### Part B — Coding Challenge (20 min)

1. Create a `Task` entity with fields: `id`, `title`, `status` (enum), `priority` (enum), `createdAt`. Use `@PrePersist` for `createdAt`.

2. Create `TaskRepository extends JpaRepository<Task, Long>` with:
   - `findByStatus(TaskStatus status)`
   - `findByTitleContainingIgnoreCase(String keyword)`

3. Create `TaskService` with:
   - `create(String title, Priority priority): Task`
   - `findAll(): List<Task>`
   - `markComplete(Long id): Task` — throws `RuntimeException` if not found

4. Create a `CommandLineRunner` that adds 3 tasks, marks one as done, and prints all tasks to console.

5. Run the app and check the H2 console at `http://localhost:8080/h2-console` to verify the data.

---

### Answers

**A1.** `@Entity` tells JPA to map this class to a database table. Every entity must have: (1) a no-arg constructor (can be `protected`), (2) a field annotated with `@Id` (the primary key). The class should not be `final`.

**A2.** `EnumType.ORDINAL` stores the enum's index (0, 1, 2...). This is **fragile** — if you add an enum value in the middle, all existing data becomes wrong. `EnumType.STRING` stores the name (e.g., "TODO", "DONE") — safe to reorder or add values. Always use `STRING`.

**A3.** Spring Data JPA parses the method name: `findBy` = SELECT, `Status` = where `status =`, `And` = AND, `Priority` = where `priority =`. It generates: `SELECT * FROM tasks WHERE status = ? AND priority = ?`. This is called **derived query methods**.

**A4.** `@Transactional` wraps the method in a database transaction — either everything succeeds and commits, or any exception causes a **rollback**. By default, Spring rolls back on `RuntimeException` and `Error`, but NOT on checked exceptions. You can customize: `@Transactional(rollbackFor = Exception.class)`.

**A5.** `@PrePersist` is a JPA lifecycle callback — the annotated method is called by JPA just **before** the entity is first saved (INSERTed) to the database. Commonly used to set `createdAt` timestamps automatically.

**Part B Solution:**
```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private LocalDateTime createdAt;

    protected Task() {}

    public Task(String title, Priority priority) {
        this.title = title;
        this.priority = priority;
    }

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }

    // getters/setters ...
    public void setStatus(TaskStatus s) { this.status = s; }
    public Long getId() { return id; }
    @Override public String toString() {
        return "[" + id + "] " + title + " — " + status + " (" + priority + ")";
    }
}

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByTitleContainingIgnoreCase(String keyword);
}

@Service @Transactional
public class TaskService {
    private final TaskRepository repo;
    public TaskService(TaskRepository repo) { this.repo = repo; }

    public Task create(String title, Priority priority) {
        return repo.save(new Task(title, priority));
    }

    public List<Task> findAll() { return repo.findAll(); }

    public Task markComplete(Long id) {
        Task task = repo.findById(id).orElseThrow(() -> new RuntimeException("Not found: " + id));
        task.setStatus(TaskStatus.DONE);
        return repo.save(task);
    }
}

@Component
public class DataLoader implements CommandLineRunner {
    private final TaskService service;
    public DataLoader(TaskService service) { this.service = service; }

    @Override public void run(String... args) {
        Task t1 = service.create("Learn JPA", Priority.HIGH);
        Task t2 = service.create("Write tests", Priority.MEDIUM);
        Task t3 = service.create("Deploy app", Priority.LOW);
        service.markComplete(t1.getId());
        service.findAll().forEach(System.out::println);
    }
}
```
