# Week 7 — Day 4: Domain-Driven Design (DDD) Basics

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. What Is DDD? (5 min)

Domain-Driven Design is an approach to software development where:
- The **domain** (business problem space) drives the design
- Code uses the same language as domain experts (**Ubiquitous Language**)
- The software model mirrors the business model

**Without DDD** — code reflects tech, not domain:
```
UserTable → UserDto → UserRequest → UserEntity → UserModel → UserBean
```
What does `UserBean` mean to a product manager? Nothing.

**With DDD** — code reflects business:
```
Customer → Order → Product → Payment → Shipment
```
A domain expert reads this and immediately understands it.

---

### 2. Ubiquitous Language (5 min)

Every term in code should have the same meaning as in business conversations.

**Example from task management:**

| Wrong (tech jargon) | Right (domain language) |
|--------------------|------------------------|
| `UserRecord` | `Owner` |
| `ItemDTO` | `Task` |
| `StatusCode` | `TaskStatus` |
| `processItem()` | `assignTask()`, `completeTask()` |
| `flagRecord()` | `markUrgent()` |

Use domain language in: class names, method names, variable names, API endpoint names, database table names.

---

### 3. Key DDD Building Blocks (25 min)

#### Entities
Objects with an **identity** that persists over time. Two entities are the same if they have the same ID, even if other fields differ.

```java
@Entity
public class Task {  // Entity — has identity (id)
    @Id @GeneratedValue
    private Long id;

    private String title;         // can change
    private TaskStatus status;    // can change
    private Priority priority;    // can change
    private String ownerUsername; // can change

    // Business methods — not just getters/setters
    public void complete() {
        if (this.status == TaskStatus.DONE) throw new IllegalStateException("Already done");
        this.status = TaskStatus.DONE;
    }

    public void assignTo(String username) {
        this.ownerUsername = Objects.requireNonNull(username);
    }
}
```

#### Value Objects
Objects defined by their **value**, not identity. Two Value Objects are equal if all fields are equal. They are **immutable**.

```java
public record Priority(String level, int score) {
    // Validation in compact constructor
    public Priority {
        if (score < 0 || score > 100)
            throw new IllegalArgumentException("Score must be 0-100");
        Objects.requireNonNull(level);
    }

    // Factory methods with domain meaning
    public static Priority high() { return new Priority("HIGH", 80); }
    public static Priority medium() { return new Priority("MEDIUM", 50); }
    public static Priority low() { return new Priority("LOW", 20); }

    public boolean isUrgent() { return score >= 80; }
}
```

```java
public record EmailAddress(String value) {
    public EmailAddress {
        if (!value.matches("^[^@]+@[^@]+\\.[^@]+$"))
            throw new IllegalArgumentException("Invalid email: " + value);
    }
}
// Two EmailAddress("alice@example.com") are equal — value objects don't need ID
```

#### Aggregates + Aggregate Root
An aggregate is a **cluster of entities and value objects** with clear boundaries. The **Aggregate Root** is the entry point — all interactions go through it.

```java
@Entity
public class Order {  // Aggregate Root
    @Id @GeneratedValue
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();  // part of same aggregate

    private OrderStatus status;
    private Money totalAmount;

    // All interactions with OrderLine go through Order (the root)
    public void addItem(Product product, int quantity) {
        if (status != OrderStatus.DRAFT)
            throw new IllegalStateException("Cannot add items to submitted order");
        lines.add(new OrderLine(product.getId(), product.getName(),
            quantity, product.getPrice()));
        recalculateTotal();
    }

    public void submit() {
        if (lines.isEmpty()) throw new IllegalStateException("Cannot submit empty order");
        this.status = OrderStatus.SUBMITTED;
    }

    private void recalculateTotal() {
        this.totalAmount = lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
    }
}

// Repository only exists for aggregate roots
public interface OrderRepository extends JpaRepository<Order, Long> {
    // NOT: OrderLineRepository — lines are accessed through Order
}
```

#### Domain Services
Business logic that doesn't naturally belong to any single entity:

```java
@Service
public class TaskAssignmentService {  // Domain Service
    // Assigns a task to the highest-available team member
    // Logic spans Task + TeamMember — doesn't belong in either
    public void assignOptimally(Task task, List<TeamMember> candidates) {
        TeamMember best = candidates.stream()
            .filter(TeamMember::isAvailable)
            .min(Comparator.comparing(TeamMember::currentWorkload))
            .orElseThrow(() -> new NoAvailableMemberException());
        task.assignTo(best.getUsername());
    }
}
```

#### Domain Events
Something meaningful that happened in the domain:

```java
// Events are facts — past tense, immutable
public record TaskCompleted(Long taskId, String title, String ownerUsername, Instant occurredAt) {}

// Published from within the aggregate method
public class Task {
    @Transient
    private List<Object> domainEvents = new ArrayList<>();

    public void complete() {
        this.status = TaskStatus.DONE;
        domainEvents.add(new TaskCompleted(id, title, ownerUsername, Instant.now()));
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
}
```

---

### 4. Repositories in DDD (5 min)

A DDD repository is an abstraction that **hides persistence details** from the domain. It looks like an in-memory collection.

```java
// DDD Repository interface (domain layer — no JPA imports)
public interface TaskRepository {
    Optional<Task> findById(Long id);
    Task save(Task task);
    List<Task> findByOwner(String ownerUsername);
    void remove(Task task);
}

// Spring Data implementation (infrastructure layer)
public interface SpringDataTaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByOwnerUsername(String ownerUsername);
}
```

Repositories exist **only for aggregate roots** — not for every entity.

---

### 5. Bounded Context (10 min)

Each bounded context has its own model, its own ubiquitous language, and its own database:

```
"Customer" in Order context:      "Customer" in Support context:
  - id                               - id
  - shippingAddress                  - email
  - paymentMethods                   - openTickets
  - orderHistory                     - preferredContactTime
```

The same word means different things in different contexts. This is OK — that's why boundaries exist.

In microservices: one bounded context ≈ one microservice.

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between an Entity and a Value Object? Give one example of each from a task management domain.

**Q2.** What is an Aggregate Root? Why should all access to an aggregate go through its root?

**Q3.** What is Ubiquitous Language? How does it manifest in code?

**Q4.** Why should a JPA Repository only exist for Aggregate Roots, not for every entity?

**Q5.** What is a Domain Service? Give an example from a task management domain.

---

### Part B — Design Challenge (20 min)

Model the domain for a **project management system** (like Jira-light):

1. Identify at least 3 entities and 2 value objects.
2. Identify the aggregate root(s). What entities are inside the aggregate?
3. What domain events would this system emit?
4. Name 2 bounded contexts in this system. How would the word "User" or "Project" mean differently in each?
5. Write a Java record for one of your value objects, with validation.

---

### Answers

**A1.** **Entity**: has identity — two objects are equal if they have the same ID, regardless of other fields. Example: `Task` (Task #5 is always Task #5 even after its title is edited). **Value Object**: defined by value — two objects are equal if all fields are equal; they're immutable. Example: `Priority` (there's no "Priority #5" — two `Priority.HIGH` values are interchangeable).

**A2.** An Aggregate Root is the single entry point to a cluster of related objects. All external interactions (from application services, APIs) go through the root, never directly to inner entities. This ensures: (1) invariants are enforced (e.g., "can't add items to a submitted order" — Order enforces this, not external code); (2) the aggregate is always in a consistent state; (3) only the root has a repository — you can't accidentally bypass business rules by directly saving an `OrderLine`.

**A3.** Ubiquitous Language means every term in code matches the exact term used by domain experts in business discussions. In code: class names (`Task`, `Owner`, `Sprint` — not `ItemRecord`, `UserEntry`, `IterationPeriod`), method names (`complete()`, `assignTo()`, `escalate()` — not `setStatus(DONE)`, `setOwner(user)`, `setPriority(CRITICAL)`), API endpoints (`POST /tasks/{id}/complete` — not `/tasks/update-flag`).

**A4.** Non-root entities exist only within the aggregate context — they don't have an independent lifecycle. `OrderLine` makes no sense without `Order`. If you create an `OrderLineRepository`, you can save an `OrderLine` directly, bypassing `Order`'s invariants (e.g., quantity validation, total recalculation). The aggregate root repository ensures all writes go through the root where invariants can be enforced.

**A5.** A Domain Service handles business logic that doesn't naturally fit in a single entity. Example in task management: `SprintCapacityCalculator` — determines if a sprint can accept a new task based on available capacity, team member assignments, and task estimates. This spans multiple entities (`Sprint`, `TeamMember`, `Task`) and doesn't belong to any one of them.

**Part B Sample:**
```
Entities: Project, Sprint, Task (aggregate root per context)
Value Objects: StoryPoints (int value, 0-100), SprintDuration (startDate, endDate, validation)

Aggregates:
- Sprint is the aggregate root containing SprintTasks (inner entities)
- Project contains Project metadata
- Task is its own aggregate root (independent lifecycle)

Domain Events:
- SprintStarted, SprintCompleted, TaskMoved (task moved between sprints),
  StoryPointsUpdated, SprintCapacityExceeded

Bounded Contexts:
- Planning context: "User" = TeamMember with skills, capacity, sprint assignments
- Reporting context: "User" = Developer with completed tasks, velocity metrics

Value Object:
public record StoryPoints(int value) {
    public StoryPoints {
        if (value < 0 || value > 100)
            throw new IllegalArgumentException("Story points must be 0-100, was: " + value);
    }
    public boolean isLarge() { return value >= 13; }
}
```
