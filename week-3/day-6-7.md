# Week 3 — Day 6 & 7: Mini Project 2 — Task Manager REST API

> Build time: 2 days (~1 hour each) | No separate test — this IS the test

---

## Project Goal

Build a **production-grade Task Manager REST API** using Spring Boot + PostgreSQL. This project consolidates everything from Weeks 1-3.

**Final API contract:**
```
POST   /api/tasks              — create a task
GET    /api/tasks              — list all tasks (optional ?status=TODO filter)
GET    /api/tasks/{id}         — get a task by ID
PUT    /api/tasks/{id}         — update a task
DELETE /api/tasks/{id}         — delete a task
PATCH  /api/tasks/{id}/status  — update status only
```

---

## Day 6 — Entities, Repository, Service

### Project Setup (10 min)

Generate at https://start.spring.io with:
- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Spring Boot Validation
- Lombok (optional, reduces boilerplate)

### Domain Model

**TaskStatus.java:**
```java
public enum TaskStatus { TODO, IN_PROGRESS, DONE }
```

**Priority.java:**
```java
public enum Priority { LOW, MEDIUM, HIGH }
```

**Task.java (Entity):**
```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Task() {}

    public Task(String title, String description, Priority priority) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = TaskStatus.TODO;
    }

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    // All getters and setters
    public Long getId()              { return id; }
    public String getTitle()         { return title; }
    public String getDescription()   { return description; }
    public TaskStatus getStatus()    { return status; }
    public Priority getPriority()    { return priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setTitle(String t)       { this.title = t; }
    public void setDescription(String d) { this.description = d; }
    public void setStatus(TaskStatus s)  { this.status = s; }
    public void setPriority(Priority p)  { this.priority = p; }
}
```

### DTOs

**CreateTaskRequest.java:**
```java
public record CreateTaskRequest(
    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be 2-100 characters")
    String title,

    @Size(max = 500, message = "Description max 500 characters")
    String description,

    @NotNull(message = "Priority is required")
    Priority priority
) {}
```

**UpdateTaskRequest.java:**
```java
public record UpdateTaskRequest(
    @NotBlank(message = "Title is required")
    String title,
    String description,
    @NotNull Priority priority,
    @NotNull TaskStatus status
) {}
```

**TaskResponse.java:**
```java
public record TaskResponse(
    Long id, String title, String description,
    TaskStatus status, Priority priority,
    LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(), task.getTitle(), task.getDescription(),
            task.getStatus(), task.getPriority(),
            task.getCreatedAt(), task.getUpdatedAt()
        );
    }
}
```

### Repository

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByPriority(Priority priority);
    List<Task> findByTitleContainingIgnoreCase(String keyword);
}
```

### Service

```java
@Service
@Transactional
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll(TaskStatus status) {
        List<Task> tasks = (status != null)
            ? repository.findByStatus(status)
            : repository.findAll();
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return repository.findById(id)
            .map(TaskResponse::from)
            .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public TaskResponse create(CreateTaskRequest request) {
        Task task = new Task(request.title(), request.description(), request.priority());
        return TaskResponse.from(repository.save(task));
    }

    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = repository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setStatus(request.status());
        return TaskResponse.from(task); // dirty checking — no explicit save needed
    }

    public TaskResponse updateStatus(Long id, TaskStatus newStatus) {
        Task task = repository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
        task.setStatus(newStatus);
        return TaskResponse.from(task);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) throw new TaskNotFoundException(id);
        repository.deleteById(id);
    }
}
```

---

## Day 7 — Controller + application.yml + Manual Testing

### Controller

```java
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> getAll(@RequestParam(required = false) TaskStatus status) {
        return taskService.findAll(status);
    }

    @GetMapping("/{id}")
    public TaskResponse getById(@PathVariable Long id) {
        return taskService.findById(id); // throws TaskNotFoundException if missing
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id, @RequestParam TaskStatus status) {
        return taskService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
```

### Exception

```java
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) {
        super("Task not found with ID: " + id);
    }
}
```

### application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskdb
    username: taskuser
    password: taskpass
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  application:
    name: task-manager
```

---

### Manual Testing with curl

```bash
# Create tasks
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn Spring Boot","priority":"HIGH"}'

curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Write unit tests","priority":"MEDIUM","description":"Cover service layer"}'

# Get all tasks
curl http://localhost:8080/api/tasks

# Filter by status
curl "http://localhost:8080/api/tasks?status=TODO"

# Get by ID
curl http://localhost:8080/api/tasks/1

# Update status
curl -X PATCH "http://localhost:8080/api/tasks/1/status?status=DONE"

# Update task
curl -X PUT http://localhost:8080/api/tasks/2 \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated title","priority":"HIGH","status":"IN_PROGRESS"}'

# Delete
curl -X DELETE http://localhost:8080/api/tasks/1

# Validation error (missing title)
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"priority":"HIGH"}'
```

---

## Completion Checklist

- [ ] App starts without errors connecting to PostgreSQL
- [ ] `POST /api/tasks` creates a task and returns 201
- [ ] `GET /api/tasks` lists all tasks
- [ ] `GET /api/tasks?status=TODO` filters correctly
- [ ] `GET /api/tasks/999` returns 500 (we'll fix to 404 in Week 4 Day 2)
- [ ] `PUT /api/tasks/{id}` updates the task
- [ ] `PATCH /api/tasks/{id}/status` updates status only
- [ ] `DELETE /api/tasks/{id}` deletes and returns 204
- [ ] Submitting empty title returns 400 with validation error
- [ ] Data persists after app restart (PostgreSQL is working)

## What You Practiced

| Concept | Where used |
|---------|-----------|
| Spring IoC/DI | All layers |
| `application.yml` | DB + server config |
| REST controller | `TaskController` |
| DTOs with validation | `CreateTaskRequest`, `UpdateTaskRequest` |
| JPA entity | `Task` |
| Spring Data repository | `TaskRepository` |
| `@Transactional` | `TaskService` |
| Enums in entities | `TaskStatus`, `Priority` |
| ResponseEntity | All controller methods |
| Custom exceptions | `TaskNotFoundException` |
