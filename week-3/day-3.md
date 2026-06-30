# Week 3 — Day 3: Building REST APIs with Spring Boot

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. REST Principles (5 min)

| Principle | Meaning |
|-----------|---------|
| Stateless | Each request contains all needed info |
| Resource-based | URLs represent nouns, not verbs (`/tasks`, not `/getTasks`) |
| HTTP verbs | GET=read, POST=create, PUT=full update, PATCH=partial update, DELETE=remove |
| Status codes | 200 OK, 201 Created, 204 No Content, 400 Bad Request, 404 Not Found, 500 Server Error |

**Good REST design:**
```
GET    /tasks          — list all tasks
POST   /tasks          — create a task
GET    /tasks/{id}     — get a specific task
PUT    /tasks/{id}     — replace a task
PATCH  /tasks/{id}     — partially update a task
DELETE /tasks/{id}     — delete a task

GET    /users/{id}/tasks — tasks belonging to a user (nested resource)
```

---

### 2. The Controller Layer (20 min)

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController                    // @Controller + @ResponseBody on every method
@RequestMapping("/api/tasks")      // base path for all methods in this class
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // GET /api/tasks
    @GetMapping
    public List<TaskResponse> getAllTasks() {
        return taskService.findAll();
    }

    // GET /api/tasks/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return taskService.findById(id)
            .map(ResponseEntity::ok)                      // 200 with body
            .orElse(ResponseEntity.notFound().build());   // 404
    }

    // POST /api/tasks  — body: {"title":"...", "priority":"HIGH"}
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created); // 201
    }

    // PUT /api/tasks/{id}
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/tasks/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build(); // 204
    }

    // GET /api/tasks?status=TODO&priority=HIGH
    @GetMapping("/search")
    public List<TaskResponse> search(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return taskService.search(status, page, size);
    }
}
```

---

### 3. DTOs — Request and Response Objects (15 min)

Never expose your `@Entity` directly — use DTOs to control what goes in and out.

**Request DTO (with validation):**
```java
import jakarta.validation.constraints.*;

public record CreateTaskRequest(
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be 3-100 characters")
    String title,

    @Size(max = 500, message = "Description too long")
    String description,

    @NotNull(message = "Priority is required")
    Priority priority
) {}
```

**Response DTO:**
```java
public record TaskResponse(
    Long id,
    String title,
    String description,
    TaskStatus status,
    Priority priority,
    LocalDateTime createdAt
) {
    // Factory method — convert from entity to DTO
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getCreatedAt()
        );
    }
}
```

**Why DTOs?**
1. Security — hide internal fields (e.g., password hashes, internal IDs)
2. Versioning — API shape independent from DB schema
3. Validation — validate request data without polluting the entity

---

### 4. Validation Annotations (10 min)

Enable validation by adding `@Valid` on the `@RequestBody` parameter.

```java
// Add spring-boot-starter-validation to pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Common validation annotations:
```java
@NotNull       // value is not null
@NotBlank      // not null AND not empty/whitespace (for String)
@NotEmpty      // not null AND not empty (for String, Collection)
@Size(min=, max=)    // for String, Collection, Array
@Min(value=)         // for numbers
@Max(value=)         // for numbers
@Email               // valid email format
@Pattern(regexp=)    // regex match
@Positive            // > 0
@PositiveOrZero      // >= 0
@Future              // date in the future
@Past                // date in the past
```

When validation fails, Spring returns `400 Bad Request` with details. We'll add a clean error response format in Week 4 Day 2.

---

### 5. ResponseEntity (10 min)

`ResponseEntity<T>` gives full control over the HTTP response:

```java
// 200 OK with body
ResponseEntity.ok(body)

// 201 Created with body and Location header
ResponseEntity
    .created(URI.create("/api/tasks/" + id))
    .body(taskResponse)

// 204 No Content (DELETE success)
ResponseEntity.noContent().build()

// 404 Not Found
ResponseEntity.notFound().build()

// 400 with body
ResponseEntity.badRequest().body("Invalid input")

// Custom status
ResponseEntity.status(HttpStatus.CONFLICT).body("Already exists")
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What HTTP method and status code should you use when creating a resource?

**Q2.** What is the difference between `@PathVariable` and `@RequestParam`?

**Q3.** Why should you use DTOs instead of returning `@Entity` objects directly from your controller?

**Q4.** What annotation enables validation of a `@RequestBody`? What happens if validation fails?

**Q5.** What does `ResponseEntity.noContent().build()` return in terms of HTTP status and body?

---

### Part B — Coding Challenge (20 min)

Build a `TaskController` for a Spring Boot project with:

1. `GET /tasks` — returns hardcoded list of 3 tasks as `List<TaskResponse>`
2. `GET /tasks/{id}` — returns a task if found (from the hardcoded list), or 404
3. `POST /tasks` — accepts `CreateTaskRequest` (with `@NotBlank title`, `@NotNull priority`), creates and returns 201
4. `DELETE /tasks/{id}` — returns 204 if found, 404 otherwise

Keep it simple — store tasks in a `List<TaskResponse>` in the controller for now (we'll add a real service + DB in Day 4-5).

Test using **Postman** or **curl**:
```bash
curl http://localhost:8080/tasks
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","priority":"HIGH"}'
```

---

### Answers

**A1.** Use `POST` to create a resource. The response should be `201 Created`. Optionally include a `Location` header pointing to the newly created resource's URL (e.g., `Location: /api/tasks/5`).

**A2.** `@PathVariable` extracts a value from the URL path — e.g., `/tasks/{id}` with `@PathVariable Long id`. `@RequestParam` extracts query string parameters — e.g., `/tasks?status=TODO` with `@RequestParam String status`. Path variables are part of the URL structure; query params are optional filters.

**A3.** (1) Security — entities can have fields you don't want to expose (passwords, audit fields). (2) Decoupling — your API contract is independent from your DB schema (you can rename columns without breaking the API). (3) Validation — DTOs carry validation rules for incoming data without affecting the entity. (4) Versioning — you can evolve the API and DB independently.

**A4.** `@Valid` on the `@RequestBody` parameter enables Bean Validation. If validation fails, Spring returns `400 Bad Request` with a `MethodArgumentNotValidException`. By default the response body has validation error details in a somewhat verbose format — we'll add a custom error format in Week 4.

**A5.** HTTP status `204 No Content` with an **empty body**. Used for successful DELETE, PUT, or PATCH operations where there's nothing meaningful to return.

**Part B Hint — Controller skeleton:**
```java
@RestController
@RequestMapping("/tasks")
public class TaskController {
    private final List<TaskResponse> store = new ArrayList<>(List.of(
        new TaskResponse(1L, "Task One",   "Desc", TaskStatus.TODO, Priority.HIGH),
        new TaskResponse(2L, "Task Two",   "Desc", TaskStatus.DONE, Priority.LOW),
        new TaskResponse(3L, "Task Three", "Desc", TaskStatus.TODO, Priority.MEDIUM)
    ));
    private long nextId = 4L;

    @GetMapping
    public List<TaskResponse> getAll() { return store; }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long id) {
        return store.stream()
            .filter(t -> t.id().equals(id))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest req) {
        var task = new TaskResponse(nextId++, req.title(), "", TaskStatus.TODO, req.priority());
        store.add(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean removed = store.removeIf(t -> t.id().equals(id));
        return removed ? ResponseEntity.noContent().build()
                       : ResponseEntity.notFound().build();
    }
}
```
