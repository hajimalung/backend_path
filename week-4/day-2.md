# Week 4 — Day 2: Global Exception Handling

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. The Problem — Default Spring Error Responses (5 min)

Without custom handling, Spring returns verbose HTML or inconsistent JSON:
```json
{
  "timestamp": "2024-01-15T10:30:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/tasks/999"
}
```

We want clean, consistent error responses across all endpoints:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Task not found with ID: 999",
  "path": "/api/tasks/999",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

### 2. Error Response DTO (5 min)

```java
public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    LocalDateTime timestamp
) {
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            LocalDateTime.now()
        );
    }
}
```

---

### 3. @ControllerAdvice — Global Exception Handler (30 min)

`@ControllerAdvice` intercepts exceptions thrown anywhere in the application and converts them to proper HTTP responses:

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestControllerAdvice          // @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    // Handle your custom not-found exception
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(
            TaskNotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Handle illegal arguments (e.g., bad enum value, business rule violations)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
            ErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI())
        );
    }

    // Handle illegal state (e.g., duplicate email)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI())
        );
    }

    // Handle @Valid validation failures — return all field errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 400);
        response.put("error", "Validation Failed");
        response.put("errors", fieldErrors);
        response.put("path", request.getRequestURI());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.badRequest().body(response);
    }

    // Handle missing @RequestParam
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
            ErrorResponse.of(HttpStatus.BAD_REQUEST,
                "Required parameter missing: " + ex.getParameterName(),
                request.getRequestURI())
        );
    }

    // Catch-all — unexpected errors (never expose stack trace to clients!)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        // Log the full exception here for debugging
        // log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError().body(
            ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",  // generic message — don't leak internals
                request.getRequestURI())
        );
    }
}
```

---

### 4. Custom Exceptions Hierarchy (10 min)

Build a proper exception hierarchy for your domain:

```java
// Base exception for all domain errors
public abstract class DomainException extends RuntimeException {
    private final HttpStatus status;

    protected DomainException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}

// Specific exceptions
public class TaskNotFoundException extends DomainException {
    public TaskNotFoundException(Long id) {
        super("Task not found with ID: " + id, HttpStatus.NOT_FOUND);
    }
}

public class DuplicateTitleException extends DomainException {
    public DuplicateTitleException(String title) {
        super("A task with title '" + title + "' already exists", HttpStatus.CONFLICT);
    }
}

public class InvalidStatusTransitionException extends DomainException {
    public InvalidStatusTransitionException(TaskStatus from, TaskStatus to) {
        super("Cannot transition from " + from + " to " + to, HttpStatus.BAD_REQUEST);
    }
}
```

**Handle base exception in one handler:**
```java
@ExceptionHandler(DomainException.class)
public ResponseEntity<ErrorResponse> handleDomainException(
        DomainException ex, HttpServletRequest request) {
    return ResponseEntity.status(ex.getStatus()).body(
        ErrorResponse.of(ex.getStatus(), ex.getMessage(), request.getRequestURI())
    );
}
```

---

### 5. RFC 7807 — ProblemDetail (Spring 6+) (10 min)

Spring 6 / Spring Boot 3 has built-in support for RFC 7807 `ProblemDetail`:

```java
// Simpler approach using Spring's built-in ProblemDetail
@ExceptionHandler(TaskNotFoundException.class)
public ProblemDetail handleNotFound(TaskNotFoundException ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setTitle("Task Not Found");
    pd.setInstance(URI.create(request.getRequestURI()));
    pd.setProperty("timestamp", LocalDateTime.now()); // custom extension fields
    return pd;
}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is `@RestControllerAdvice`? How does it differ from `@ControllerAdvice`?

**Q2.** In what order does Spring apply `@ExceptionHandler` methods when multiple could match?

**Q3.** Why should you NEVER expose the full exception message or stack trace to clients in production?

**Q4.** What HTTP status code should duplicate resource errors return? What about resource not found?

**Q5.** What class does Spring throw when `@Valid` validation fails?

---

### Part B — Coding Challenge (20 min)

Add proper error handling to your Mini Project 2:

1. Create the `GlobalExceptionHandler` with handlers for:
   - `TaskNotFoundException` → 404
   - `MethodArgumentNotValidException` → 400 with field-level errors
   - `IllegalArgumentException` → 400
   - `Exception` → 500 (generic)

2. Create custom exceptions: `TaskNotFoundException(Long id)` and `DuplicateTitleException(String title)`.

3. Update `TaskService.create()` to throw `DuplicateTitleException` if a task with the same title (case-insensitive) already exists.

4. Test these scenarios with curl:
   - `GET /api/tasks/999` → should now return 404 JSON, not 500
   - `POST /api/tasks` with no title → 400 with field errors
   - `POST /api/tasks` with duplicate title → 409 Conflict

---

### Answers

**A1.** `@ControllerAdvice` is a specialization of `@Component` that provides global exception handling, data binding, and model attributes across all controllers. `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody` on every handler method, meaning responses are automatically serialized to JSON (no need for `ResponseEntity`).

**A2.** Spring finds the most specific handler. If a `TaskNotFoundException extends DomainException extends RuntimeException`, and you have handlers for all three, Spring picks `TaskNotFoundException`. If none matches the exact type, it walks up the hierarchy. `Exception.class` is the most generic catch-all.

**A3.** Security — exception messages and stack traces may reveal: (1) internal system paths, (2) database schema details, (3) library versions (useful for known exploit targeting), (4) business logic that attackers could exploit. Always log the full exception server-side and return a generic message to clients.

**A4.** Duplicate resource → `409 Conflict`. Resource not found → `404 Not Found`. Other status codes: `400 Bad Request` (invalid input), `401 Unauthorized` (not authenticated), `403 Forbidden` (authenticated but not allowed), `422 Unprocessable Entity` (semantically invalid).

**A5.** `MethodArgumentNotValidException`. It contains a `BindingResult` with `FieldError` objects listing which fields failed validation and why. Access them via `ex.getBindingResult().getFieldErrors()`.

**Part B — Key code:**
```java
// DuplicateTitleException
public class DuplicateTitleException extends RuntimeException {
    public DuplicateTitleException(String title) {
        super("Task with title '" + title + "' already exists");
    }
}

// In TaskService.create()
public TaskResponse create(CreateTaskRequest request) {
    if (repository.existsByTitleIgnoreCase(request.title())) {
        throw new DuplicateTitleException(request.title());
    }
    Task task = new Task(request.title(), request.description(), request.priority());
    return TaskResponse.from(repository.save(task));
}

// In TaskRepository
boolean existsByTitleIgnoreCase(String title);

// In GlobalExceptionHandler
@ExceptionHandler(DuplicateTitleException.class)
public ResponseEntity<ErrorResponse> handleDuplicate(
        DuplicateTitleException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI()));
}
```
