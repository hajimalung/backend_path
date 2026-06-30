# Week 4 — Day 1: JPQL and Custom Queries

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Derived Query Methods (Review + Advanced) (10 min)

Spring Data JPA derives queries from method names automatically:

```java
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Derived queries — no SQL needed
    List<Task>     findByStatus(TaskStatus status);
    List<Task>     findByStatusAndPriority(TaskStatus status, Priority priority);
    List<Task>     findByTitleContainingIgnoreCase(String keyword);
    List<Task>     findByCreatedAtAfter(LocalDateTime date);
    List<Task>     findByStatusOrderByCreatedAtDesc(TaskStatus status);
    Optional<Task> findFirstByOrderByCreatedAtDesc();  // most recently created
    long           countByStatus(TaskStatus status);
    boolean        existsByTitleIgnoreCase(String title);
    void           deleteByStatus(TaskStatus status);

    // Complex: between, in, like, isNull, isNotNull
    List<Task> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    List<Task> findByPriorityIn(List<Priority> priorities);
    List<Task> findByDescriptionIsNull();
}
```

---

### 2. JPQL — Java Persistence Query Language (20 min)

JPQL looks like SQL but **operates on Java objects/fields**, not tables/columns.

```java
// @Query with JPQL
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Simple JPQL — references class name (Task) and field names (status, title)
    @Query("SELECT t FROM Task t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<Task> findByStatusSorted(@Param("status") TaskStatus status);

    // Named parameters
    @Query("SELECT t FROM Task t WHERE t.priority = :p AND t.status != :s")
    List<Task> findActiveByPriority(@Param("p") Priority priority, @Param("s") TaskStatus excludedStatus);

    // COUNT / aggregation
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = :status")
    long countByStatusJPQL(@Param("status") TaskStatus status);

    // JOIN FETCH — solves N+1 (loads Task + User in one query)
    @Query("SELECT t FROM Task t JOIN FETCH t.assignee WHERE t.status = 'TODO'")
    List<Task> findTodoTasksWithAssignee();

    // Projection — select only specific fields (returns Object[])
    @Query("SELECT t.id, t.title, t.status FROM Task t WHERE t.priority = :p")
    List<Object[]> findTitleAndStatusByPriority(@Param("p") Priority priority);

    // Return DTO directly (interface projection)
    @Query("SELECT t.id AS id, t.title AS title, t.status AS status FROM Task t")
    List<TaskSummary> findSummaries();

    // DELETE query
    @Modifying                             // required for UPDATE/DELETE JPQL
    @Transactional
    @Query("UPDATE Task t SET t.status = 'DONE' WHERE t.priority = :p")
    int markAllDoneByPriority(@Param("p") Priority priority);
}
```

**Interface projection (type-safe):**
```java
public interface TaskSummary {
    Long getId();
    String getTitle();
    TaskStatus getStatus();
}
```

---

### 3. Native SQL Queries (10 min)

When JPQL isn't enough (DB-specific features, complex CTEs, etc.):

```java
// nativeQuery = true — raw SQL, references table names and column names
@Query(value = "SELECT * FROM tasks WHERE status = ?1 LIMIT ?2",
       nativeQuery = true)
List<Task> findTopNByStatus(String status, int limit);

// With named params in native SQL
@Query(value = "SELECT * FROM tasks WHERE created_at > :date AND user_id = :userId",
       nativeQuery = true)
List<Task> findRecentTasksByUser(@Param("userId") Long userId, @Param("date") LocalDateTime date);
```

---

### 4. Pagination and Sorting (20 min)

Essential for any real API — never return unbounded lists.

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

// Repository — add Pageable to any find method
public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.status = :s")
    Page<Task> findByStatusPaged(@Param("s") TaskStatus status, Pageable pageable);
}
```

```java
// Service
@Transactional(readOnly = true)
public Page<TaskResponse> findPaged(TaskStatus status, int page, int size, String sortBy) {
    Sort sort = Sort.by(Sort.Direction.DESC, sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<Task> taskPage = (status != null)
        ? taskRepository.findByStatus(status, pageable)
        : taskRepository.findAll(pageable);

    return taskPage.map(TaskResponse::from);
}
```

```java
// Controller
@GetMapping
public ResponseEntity<Page<TaskResponse>> getAll(
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy) {
    Page<TaskResponse> result = taskService.findPaged(status, page, size, sortBy);
    return ResponseEntity.ok(result);
}
```

**Test with curl:**
```bash
# Page 0, size 5, sorted by createdAt descending
curl "http://localhost:8080/api/tasks?page=0&size=5&sortBy=createdAt"

# Page 1 of TODO tasks
curl "http://localhost:8080/api/tasks?status=TODO&page=1&size=3"
```

**What `Page<T>` returns:**
```json
{
  "content": [...],
  "totalElements": 25,
  "totalPages": 3,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false
}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between JPQL and native SQL in `@Query`?

**Q2.** When do you need `@Modifying` on a `@Query` method?

**Q3.** What does `Page<Task>` contain beyond just the list of tasks?

**Q4.** What is the N+1 problem? Which JPQL feature solves it?

**Q5.** What is an interface projection in Spring Data JPA?

---

### Part B — Coding Challenge (20 min)

Add to your Mini Project 2 `TaskRepository`:

1. A JPQL query `findHighPriorityTodos()` that returns all HIGH priority tasks with status TODO, sorted by createdAt.

2. An `@Modifying` query `archiveOldDoneTasks(LocalDateTime before)` that deletes DONE tasks created before a given date.

3. An interface projection `TaskSummary` with `id`, `title`, `status` and a query that returns `List<TaskSummary>`.

4. Add pagination to `GET /api/tasks` — accept `page`, `size`, and `sortBy` query params.

Test all 4 with curl.

---

### Answers

**A1.** JPQL references **Java class and field names** (e.g., `Task`, `task.status`) and is DB-agnostic — Hibernate translates it to the target dialect. Native SQL uses actual **table and column names** and is database-specific. Use JPQL for portability; native SQL for DB-specific features or complex queries JPQL can't express.

**A2.** `@Modifying` is required for `UPDATE` and `DELETE` JPQL statements. Without it, Spring Data JPA throws an exception (it expects queries to return results, not affect rows). You also need `@Transactional` on the method or calling service.

**A3.** `Page<T>` contains: `content` (the actual list), `totalElements` (total count), `totalPages`, `number` (current page, 0-based), `size` (page size), `first` (is this the first page?), `last` (is this the last?). This lets clients implement proper pagination UI.

**A4.** N+1: loading N entities causes N additional queries for each entity's lazy relationship. Example: 50 tasks → 51 queries when accessing `task.getAssignee()` in a loop. `JOIN FETCH` in JPQL loads parent + related entities in a single SQL JOIN, reducing it to 1 query.

**A5.** An interface with getter methods matching aliases in your `@Query` result. Spring Data creates a proxy that maps query result columns to these methods, giving you type-safe, named access to projected fields without creating a full DTO class.

**Part B Solution:**
```java
// In TaskRepository
@Query("SELECT t FROM Task t WHERE t.priority = 'HIGH' AND t.status = 'TODO' ORDER BY t.createdAt")
List<Task> findHighPriorityTodos();

@Modifying
@Transactional
@Query("DELETE FROM Task t WHERE t.status = 'DONE' AND t.createdAt < :before")
int archiveOldDoneTasks(@Param("before") LocalDateTime before);

// Interface projection
public interface TaskSummary {
    Long getId();
    String getTitle();
    TaskStatus getStatus();
}

@Query("SELECT t.id AS id, t.title AS title, t.status AS status FROM Task t")
List<TaskSummary> findAllSummaries();

// Page<Task> in repository
Page<Task> findAll(Pageable pageable);
```
