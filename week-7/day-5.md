# Week 7 — Day 5: CQRS and Event Sourcing Intro

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. The Traditional CRUD Problem (5 min)

In a standard CRUD system, reads and writes use the same model:

```java
// Same Task entity serves both:
taskRepository.save(task);           // write
taskRepository.findById(1L);         // read
taskRepository.findByOwner(user);    // read — complex query
```

Problems at scale:
- **Read-write contention**: heavy reads slow writes (same table, same indexes)
- **Model mismatch**: queries often need denormalized data (joins, aggregations) while writes need normalized data
- **Scaling**: you can't scale reads and writes independently

---

### 2. CQRS — Command Query Responsibility Segregation (20 min)

CQRS separates **commands** (write operations that change state) from **queries** (read operations that return data).

```
Commands (write side):              Queries (read side):
─────────────────────               ────────────────────
CreateTaskCommand                   GetTaskByIdQuery
UpdateTaskCommand                   GetTasksForUserQuery
CompleteTaskCommand                 GetTaskCountByStatusQuery
                ↓                              ↑
         Write Model                     Read Model
         (normalized,                   (denormalized,
          optimized for                  optimized for
          consistency)                   query speed)
         PostgreSQL                     PostgreSQL views /
                                        MongoDB / Elasticsearch
```

**Simplified CQRS (same DB, different models):**
```java
// Command side — domain model
@Service
public class TaskCommandService {
    private final TaskRepository taskRepository;

    @Transactional
    public Long createTask(CreateTaskCommand command) {
        Task task = new Task(command.title(), command.description(),
            command.priority(), command.ownerUsername());
        return taskRepository.save(task).getId();
    }

    @Transactional
    public void completeTask(CompleteTaskCommand command) {
        Task task = taskRepository.findById(command.taskId()).orElseThrow();
        task.complete();
        taskRepository.save(task);
    }
}

// Query side — optimized read model (could be a view, DTO projection, etc.)
@Service
public class TaskQueryService {
    private final EntityManager em;

    public TaskSummaryDto findById(Long id) {
        return em.createQuery(
            "SELECT new TaskSummaryDto(t.id, t.title, t.status, t.priority, t.ownerUsername) " +
            "FROM Task t WHERE t.id = :id", TaskSummaryDto.class)
            .setParameter("id", id)
            .getSingleResult();
    }

    public Page<TaskListItemDto> findByOwner(String username, Pageable pageable) {
        // Optimized query returning only what the UI needs — no N+1
        return em.createQuery("...", TaskListItemDto.class)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();
    }
}
```

**Key insight**: queries return data transfer objects (DTOs), not domain entities. The query side doesn't need to enforce business rules — it just reads efficiently.

---

### 3. Event Sourcing (20 min)

In traditional systems, you store **current state**:
```
Task: { id: 1, title: "Buy milk", status: DONE, priority: HIGH }
```

In Event Sourcing, you store **all events that led to the current state**:
```
TaskCreated    { taskId: 1, title: "Buy milk", priority: LOW,  at: 10:00 }
PriorityChanged{ taskId: 1, priority: HIGH,                    at: 11:00 }
TaskCompleted  { taskId: 1,                                    at: 14:00 }
```

The current state is **derived** by replaying all events.

**Benefits:**
- **Complete audit trail**: you know every change ever made, when, and (if recorded) by whom
- **Time travel**: reconstruct the state at any point in time
- **Replay**: reprocess events to build new projections (e.g., add analytics without backfill)
- **Event-driven**: events are produced as a side effect of commands

**Simple Event Sourcing implementation:**
```java
// Events are immutable records
public sealed interface TaskDomainEvent permits
    TaskCreated, TaskCompleted, PriorityChanged, TaskAssigned {}

public record TaskCreated(Long taskId, String title, Priority priority,
    String ownerUsername, Instant at) implements TaskDomainEvent {}

public record TaskCompleted(Long taskId, Instant at) implements TaskDomainEvent {}

public record PriorityChanged(Long taskId, Priority from, Priority to,
    Instant at) implements TaskDomainEvent {}

// Event store
@Entity
public class TaskEventStore {
    @Id @GeneratedValue private Long id;
    private Long taskId;
    private String eventType;
    @Column(columnDefinition = "jsonb") private String payload; // serialized event
    private Instant occurredAt;
    private Long sequenceNumber;
}
```

**Reconstruct state from events:**
```java
public Task reconstruct(Long taskId) {
    List<TaskDomainEvent> events = eventStoreRepository.findByTaskIdOrderBySequenceNumber(taskId)
        .stream().map(this::deserialize).toList();

    Task task = new Task();  // empty aggregate
    for (TaskDomainEvent event : events) {
        task.apply(event);   // each event mutates state
    }
    return task;
}

// In Task aggregate:
public void apply(TaskDomainEvent event) {
    switch (event) {
        case TaskCreated e -> {
            this.id = e.taskId(); this.title = e.title();
            this.priority = e.priority(); this.ownerUsername = e.ownerUsername();
            this.status = TaskStatus.TODO;
        }
        case TaskCompleted e -> this.status = TaskStatus.DONE;
        case PriorityChanged e -> this.priority = e.to();
        case TaskAssigned e -> this.ownerUsername = e.newOwner();
    }
}
```

---

### 4. CQRS + Event Sourcing Combined (5 min)

They're separate patterns but complement each other:

```
Command → [TaskCommandHandler] → apply domain logic
                                       ↓
                               append events to Event Store
                                       ↓
                         [Event Processor / Projection]
                                       ↓
                              update Read Model (denormalized)

Query → [TaskQueryService] → read Read Model (fast, no joins)
```

**When to use:**
- CQRS alone: different read/write scalability needs, complex query requirements
- Event Sourcing alone: audit trail, compliance, undo/replay requirements
- Both together: large-scale systems with complex domains and compliance needs

**When NOT to use**: simple CRUD apps, small teams, early-stage products. The added complexity is rarely worth it for simple domains.

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What does CQRS stand for? What is the core idea?

**Q2.** What does the query side of CQRS return — entities or DTOs? Why?

**Q3.** What is the main difference between storing state and storing events?

**Q4.** Name two concrete benefits of Event Sourcing.

**Q5.** A team is building a simple task manager for 5 internal users. Should they use Event Sourcing? Why or why not?

---

### Part B — Design Challenge (20 min)

Design a CQRS structure for your task-service:

1. List 3 commands (write operations) and create command objects for them.
2. List 3 queries (read operations) and create query DTO objects for them.
3. Sketch the `TaskCommandService` (just method signatures, no implementation).
4. Sketch the `TaskQueryService` (just method signatures, no implementation).
5. How would you separate controllers — one controller per side, or separate endpoints?

---

### Answers

**A1.** Command Query Responsibility Segregation. The core idea: separate the model used for writing data (commands — `CreateTask`, `UpdateTask`) from the model used for reading data (queries — `GetTaskById`, `GetTaskList`). This allows each side to be optimized independently — different data structures, potentially different databases, independent scaling.

**A2.** The query side returns **DTOs** (Data Transfer Objects), not domain entities. Entities carry business logic, invariants, and lazy-loaded relationships — wrong for read operations. DTOs are plain data containers, shaped exactly for what the caller needs (no extra fields, no lazy-loading issues, no domain logic). They can be projected directly from JPQL without loading full entities.

**A3.** Storing state: you only know the current state of data. If someone changes a task's priority, the old priority is gone. Storing events: every change is preserved as an immutable event in a log. You can reconstruct any historical state by replaying events up to a point in time. State storage is simpler; event storage is richer but more complex.

**A4.** Any two: (1) **Complete audit trail** — you know every change ever made, when, and by whom; (2) **Time travel** — reconstruct the system state at any past point in time; (3) **Replay** — reprocess all historical events to build new projections or fix bugs in projections; (4) **Natural event sourcing** — events are first-class citizens, making event-driven architectures simpler; (5) **Temporal queries** — answer "what was the state at date X?" trivially.

**A5.** No — Event Sourcing adds significant complexity: event schema versioning, projection rebuilding, distributed transaction handling, eventual consistency in read models. For a 5-user internal tool with simple CRUD, the overhead has no payoff. Start with simple CRUD, extract patterns when you hit actual pain points. "Premature optimization is the root of all evil" applies to architecture too.

**Part B Sample:**
```java
// Commands
record CreateTaskCommand(String title, String description, Priority priority, String ownerUsername) {}
record CompleteTaskCommand(Long taskId, String ownerUsername) {}
record UpdateTaskPriorityCommand(Long taskId, Priority newPriority, String ownerUsername) {}

// Query DTOs
record TaskDetailDto(Long id, String title, String description, TaskStatus status,
    Priority priority, String ownerUsername, LocalDate dueDate) {}
record TaskListItemDto(Long id, String title, TaskStatus status, Priority priority) {}
record TaskCountByStatusDto(Map<TaskStatus, Long> counts) {}

// Command service
interface TaskCommandService {
    Long create(CreateTaskCommand command);
    void complete(CompleteTaskCommand command);
    void updatePriority(UpdateTaskPriorityCommand command);
}

// Query service
interface TaskQueryService {
    TaskDetailDto findById(Long id);
    Page<TaskListItemDto> findByOwner(String username, Pageable pageable);
    TaskCountByStatusDto getCountsByStatus(String username);
}

// Controllers: two separate controllers
@RestController @RequestMapping("/api/tasks")
class TaskCommandController { /* POST, PUT, DELETE */ }

@RestController @RequestMapping("/api/tasks")
class TaskQueryController { /* GET */ }
```
