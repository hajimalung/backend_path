# Week 3 — Day 5: PostgreSQL Integration + Transactions

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Switch from H2 to PostgreSQL (10 min)

**Install PostgreSQL locally** (if not already): https://www.postgresql.org/download/

```bash
# Create database and user
psql -U postgres
CREATE DATABASE taskdb;
CREATE USER taskuser WITH ENCRYPTED PASSWORD 'taskpass';
GRANT ALL PRIVILEGES ON DATABASE taskdb TO taskuser;
\q
```

**pom.xml — replace H2 with PostgreSQL driver:**
```xml
<!-- Remove H2, add PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

**application.yml:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskdb
    username: taskuser
    password: taskpass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update        # update = add missing tables/columns, don't drop existing
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
```

---

### 2. Entity Relationships (20 min)

In a real app, entities relate to each other.

**@ManyToOne (Many tasks belong to one User):**
```java
@Entity
@Table(name = "tasks")
public class Task {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)    // don't load User until accessed
    @JoinColumn(name = "user_id")          // FK column in tasks table
    private User assignee;

    // ... other fields
}
```

**@OneToMany (One User has many Tasks):**
```java
@Entity
@Table(name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @OneToMany(mappedBy = "assignee",     // "assignee" = field name in Task
               cascade = CascadeType.ALL,  // operations cascade to tasks
               fetch = FetchType.LAZY)
    private List<Task> tasks = new ArrayList<>();
}
```

**Cascade types:**
- `PERSIST` — saving User also saves its Tasks
- `MERGE` — updating User also updates Tasks
- `REMOVE` — deleting User also deletes Tasks
- `ALL` — all of the above
- `DETACH`, `REFRESH` — less common

**FetchType:**
- `LAZY` — don't load related entities until accessed (default for collections — recommended)
- `EAGER` — always load related entities with the parent (default for `@ManyToOne`, can cause N+1)

---

### 3. The N+1 Problem (10 min)

A classic JPA performance trap:

```java
// BAD — N+1 queries
List<Task> tasks = taskRepository.findAll(); // 1 query: SELECT * FROM tasks
for (Task task : tasks) {
    System.out.println(task.getAssignee().getName()); // N queries: SELECT * FROM users WHERE id=?
}
// If 100 tasks → 101 queries!

// GOOD — fetch join (1 query)
@Query("SELECT t FROM Task t JOIN FETCH t.assignee")
List<Task> findAllWithAssignee();
// 1 query: SELECT t.*, u.* FROM tasks t INNER JOIN users u ON t.user_id = u.id
```

---

### 4. Transactions in Depth (15 min)

```java
@Service
public class TaskService {

    private final TaskRepository taskRepo;
    private final UserRepository userRepo;

    // Reads: use readOnly=true — Spring skips dirty checking (faster)
    @Transactional(readOnly = true)
    public List<TaskResponse> findAll() {
        return taskRepo.findAll().stream().map(TaskResponse::from).toList();
    }

    // Writes: default transaction (read-write)
    @Transactional
    public TaskResponse assignTask(Long taskId, Long userId) {
        Task task = taskRepo.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        task.setAssignee(user);
        // No need to call save() — JPA detects the change (dirty checking)
        // and writes it at the end of the transaction
        return TaskResponse.from(task);
    }

    @Transactional
    public void transferTasks(Long fromUserId, Long toUserId) {
        List<Task> tasks = taskRepo.findByAssigneeId(fromUserId);
        User newOwner = userRepo.findById(toUserId)
            .orElseThrow(() -> new UserNotFoundException(toUserId));

        // If ANY of these fail, ALL are rolled back
        for (Task task : tasks) {
            task.setAssignee(newOwner); // dirty checking — auto-saves at commit
        }
        // Commit happens here — all tasks transferred atomically
    }
}
```

**Transaction propagation:**
```java
// REQUIRED (default) — join existing or create new
@Transactional(propagation = Propagation.REQUIRED)

// REQUIRES_NEW — always create a new transaction (suspends current)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void auditLog(String message) { ... } // always commits, even if outer tx rolls back

// NOT_SUPPORTED — run without transaction
// NEVER — throw if called within a transaction
```

---

### 5. Data Seeding with @PostConstruct (5 min)

```java
@Component
public class DataSeeder {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public DataSeeder(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct                              // runs after Spring injects all dependencies
    public void seed() {
        if (taskRepository.count() == 0) {      // only seed if empty
            User user = userRepository.save(new User("Alice", "alice@example.com"));
            taskRepository.save(new Task("Learn JPA", Priority.HIGH, user));
            taskRepository.save(new Task("Build API",  Priority.MEDIUM, user));
        }
    }
}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between `ddl-auto: update` and `ddl-auto: create`?

**Q2.** What is the N+1 problem? How does `JOIN FETCH` solve it?

**Q3.** What does `@Transactional(readOnly = true)` do? Why is it useful?

**Q4.** What is `CascadeType.ALL`? Give a scenario where you'd want it and one where you wouldn't.

**Q5.** What is the difference between `FetchType.LAZY` and `FetchType.EAGER`?

---

### Part B — Coding Challenge (20 min)

1. Set up PostgreSQL connection in `application.yml` (or keep H2 if PostgreSQL isn't installed).

2. Create a `User` entity (`id`, `name`, `email`, `tasks` OneToMany).

3. Create a `UserRepository extends JpaRepository<User, Long>` with `Optional<User> findByEmail(String email)`.

4. Create a `UserService` with:
   - `register(String name, String email): User` — throws `IllegalStateException` if email already exists
   - `assignTask(Long userId, Long taskId): Task` — links a task to a user

5. Test the `register()` method prevents duplicate emails in a `@Transactional` context.

---

### Answers

**A1.** `create` drops and recreates all tables every time the app starts — you lose all data on restart. `update` only adds missing tables and columns — existing data is preserved. Never use `create` or `create-drop` in production. Use `validate` or `none` in production (run migrations with Flyway/Liquibase).

**A2.** N+1 happens when loading N entities triggers N additional queries for each entity's lazy-loaded relationship. Example: loading 100 tasks then accessing each task's user = 101 queries. `JOIN FETCH` rewrites it as a single SQL JOIN, fetching tasks AND their users in one query.

**A3.** Marks the transaction as read-only, which: (1) allows Hibernate to skip dirty checking (comparing entity state before and after — expensive), (2) can enable DB-level optimizations, (3) signals to the connection pool that a read-replica can be used. Use it on any method that only reads data.

**A4.** `CascadeType.ALL` propagates all operations (persist, merge, remove, etc.) from parent to children. Want it: `User` → `Address` (an address only makes sense with a user). Don't want it: `Order` → `Product` (products exist independently; deleting an order shouldn't delete products).

**A5.** `LAZY` (recommended for collections) loads related entities only when accessed. `EAGER` loads them immediately with the parent, always. EAGER can cause performance problems — use LAZY + explicit `JOIN FETCH` in queries when you need the data.

**Part B Solution:**
```java
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @OneToMany(mappedBy = "assignee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Task> tasks = new ArrayList<>();

    protected User() {}
    public User(String name, String email) { this.name = name; this.email = email; }
    // getters...
}

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

@Service @Transactional
public class UserService {
    private final UserRepository userRepo;
    private final TaskRepository taskRepo;

    public UserService(UserRepository userRepo, TaskRepository taskRepo) {
        this.userRepo = userRepo; this.taskRepo = taskRepo;
    }

    public User register(String name, String email) {
        if (userRepo.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email already registered: " + email);
        }
        return userRepo.save(new User(name, email));
    }

    public Task assignTask(Long userId, Long taskId) {
        User user = userRepo.findById(userId).orElseThrow();
        Task task = taskRepo.findById(taskId).orElseThrow();
        task.setAssignee(user);
        return taskRepo.save(task);
    }
}
```
