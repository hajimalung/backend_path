# Week 2 — Day 6 & 7: Mini Project 1 — CLI Task Manager

> Build time: 2 days (~1 hour each) | No separate test — this IS the test

---

## Project Goal

Build a **command-line Task Manager** in Java that demonstrates everything from Weeks 1-2:
- OOP design (classes, interfaces, enums)
- Collections (ArrayList, HashMap)
- Streams and Lambdas
- Optional
- Exception handling
- Maven project structure

---

## Day 6 — Design + Core Implementation

### Step 1: Create Maven Project (10 min)

```bash
mvn archetype:generate \
  -DgroupId=com.learn.taskmanager \
  -DartifactId=task-manager-cli \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DarchetypeVersion=1.4 \
  -DinteractiveMode=false

cd task-manager-cli
```

Update `pom.xml` — set Java 21 and add JUnit 5:
```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.1</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

### Step 2: Design the Domain Model (10 min)

Plan your classes before coding:

```
com.learn.taskmanager/
├── model/
│   ├── Task.java          ← the core entity
│   ├── TaskStatus.java    ← enum: TODO, IN_PROGRESS, DONE
│   └── Priority.java      ← enum: LOW, MEDIUM, HIGH
├── service/
│   └── TaskService.java   ← business logic
├── exception/
│   └── TaskNotFoundException.java
└── Main.java              ← entry point + CLI loop
```

---

### Step 3: Implement the Model (15 min)

**TaskStatus.java:**
```java
package com.learn.taskmanager.model;

public enum TaskStatus {
    TODO, IN_PROGRESS, DONE;

    public boolean isComplete() {
        return this == DONE;
    }
}
```

**Priority.java:**
```java
package com.learn.taskmanager.model;

public enum Priority {
    LOW(1), MEDIUM(5), HIGH(10);

    private final int weight;

    Priority(int weight) { this.weight = weight; }

    public int getWeight() { return weight; }
}
```

**Task.java:**
```java
package com.learn.taskmanager.model;

import java.time.LocalDateTime;

public class Task {
    private static int idCounter = 1;

    private final int id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private final LocalDateTime createdAt;

    public Task(String title, String description, Priority priority) {
        this.id          = idCounter++;
        this.title       = title;
        this.description = description;
        this.priority    = priority;
        this.status      = TaskStatus.TODO;
        this.createdAt   = LocalDateTime.now();
    }

    // Getters
    public int getId()              { return id; }
    public String getTitle()        { return title; }
    public String getDescription()  { return description; }
    public TaskStatus getStatus()   { return status; }
    public Priority getPriority()   { return priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setTitle(String title)           { this.title = title; }
    public void setStatus(TaskStatus status)     { this.status = status; }
    public void setPriority(Priority priority)   { this.priority = priority; }

    @Override
    public String toString() {
        return String.format("[%d] %-30s | %-11s | %-6s | Created: %s",
            id, title, status, priority, createdAt.toLocalDate());
    }
}
```

---

### Step 4: TaskNotFoundException (5 min)

```java
package com.learn.taskmanager.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(int id) {
        super("Task with ID " + id + " not found");
    }
}
```

---

### Step 5: TaskService (20 min)

```java
package com.learn.taskmanager.service;

import com.learn.taskmanager.exception.TaskNotFoundException;
import com.learn.taskmanager.model.Priority;
import com.learn.taskmanager.model.Task;
import com.learn.taskmanager.model.TaskStatus;

import java.util.*;
import java.util.stream.Collectors;

public class TaskService {

    private final List<Task> tasks = new ArrayList<>();

    public Task addTask(String title, String description, Priority priority) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title cannot be blank");
        }
        Task task = new Task(title, description, priority);
        tasks.add(task);
        return task;
    }

    public List<Task> getAllTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public Optional<Task> findById(int id) {
        return tasks.stream()
            .filter(t -> t.getId() == id)
            .findFirst();
    }

    public Task getById(int id) {
        return findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public List<Task> filterByStatus(TaskStatus status) {
        return tasks.stream()
            .filter(t -> t.getStatus() == status)
            .collect(Collectors.toList());
    }

    public List<Task> filterByPriority(Priority priority) {
        return tasks.stream()
            .filter(t -> t.getPriority() == priority)
            .collect(Collectors.toList());
    }

    public List<Task> getSortedByPriority() {
        return tasks.stream()
            .sorted(Comparator.comparingInt(t -> -t.getPriority().getWeight()))
            .collect(Collectors.toList());
    }

    public void markDone(int id) {
        getById(id).setStatus(TaskStatus.DONE);
    }

    public void markInProgress(int id) {
        getById(id).setStatus(TaskStatus.IN_PROGRESS);
    }

    public boolean deleteTask(int id) {
        return tasks.removeIf(t -> t.getId() == id);
    }

    public Map<TaskStatus, Long> getStatusSummary() {
        return tasks.stream()
            .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
    }
}
```

---

## Day 7 — CLI + Tests + Polish

### Step 6: Main CLI Loop (30 min)

```java
package com.learn.taskmanager;

import com.learn.taskmanager.exception.TaskNotFoundException;
import com.learn.taskmanager.model.Priority;
import com.learn.taskmanager.model.TaskStatus;
import com.learn.taskmanager.service.TaskService;

import java.util.Scanner;

public class Main {
    private static final TaskService service = new TaskService();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        seedData(); // add sample tasks
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            handleChoice(choice);
        }
    }

    private static void printMenu() {
        System.out.println("\n=== Task Manager ===");
        System.out.println("1. List all tasks");
        System.out.println("2. Add task");
        System.out.println("3. Mark task as done");
        System.out.println("4. Mark task as in-progress");
        System.out.println("5. Delete task");
        System.out.println("6. Filter by status");
        System.out.println("7. View summary");
        System.out.println("0. Exit");
        System.out.print("Choice: ");
    }

    private static void handleChoice(String choice) {
        try {
            switch (choice) {
                case "1" -> service.getAllTasks().forEach(System.out::println);
                case "2" -> addTask();
                case "3" -> updateStatus("done");
                case "4" -> updateStatus("progress");
                case "5" -> deleteTask();
                case "6" -> filterByStatus();
                case "7" -> printSummary();
                case "0" -> { System.out.println("Goodbye!"); System.exit(0); }
                default  -> System.out.println("Invalid option.");
            }
        } catch (TaskNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Validation error: " + e.getMessage());
        }
    }

    private static void addTask() {
        System.out.print("Title: ");
        String title = scanner.nextLine();
        System.out.print("Description: ");
        String desc = scanner.nextLine();
        System.out.print("Priority (LOW/MEDIUM/HIGH): ");
        Priority priority = Priority.valueOf(scanner.nextLine().toUpperCase());
        var task = service.addTask(title, desc, priority);
        System.out.println("Added: " + task);
    }

    private static void updateStatus(String type) {
        System.out.print("Task ID: ");
        int id = Integer.parseInt(scanner.nextLine());
        if (type.equals("done")) service.markDone(id);
        else service.markInProgress(id);
        System.out.println("Updated: " + service.getById(id));
    }

    private static void deleteTask() {
        System.out.print("Task ID to delete: ");
        int id = Integer.parseInt(scanner.nextLine());
        boolean removed = service.deleteTask(id);
        System.out.println(removed ? "Deleted task " + id : "Task not found");
    }

    private static void filterByStatus() {
        System.out.print("Status (TODO/IN_PROGRESS/DONE): ");
        TaskStatus status = TaskStatus.valueOf(scanner.nextLine().toUpperCase());
        service.filterByStatus(status).forEach(System.out::println);
    }

    private static void printSummary() {
        var summary = service.getStatusSummary();
        summary.forEach((status, count) ->
            System.out.println(status + ": " + count + " task(s)"));
    }

    private static void seedData() {
        service.addTask("Set up JDK", "Install Java 21", Priority.HIGH);
        service.addTask("Learn Streams", "Study Stream API", Priority.MEDIUM);
        service.addTask("Build REST API", "Spring Boot project", Priority.HIGH);
        service.markDone(1);
    }
}
```

---

### Step 7: Write Unit Tests (20 min)

```java
package com.learn.taskmanager.service;

import com.learn.taskmanager.exception.TaskNotFoundException;
import com.learn.taskmanager.model.Priority;
import com.learn.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {
    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService();
    }

    @Test
    void addTask_validInput_returnsTask() {
        var task = service.addTask("Learn Java", "Study basics", Priority.HIGH);
        assertEquals("Learn Java", task.getTitle());
        assertEquals(TaskStatus.TODO, task.getStatus());
        assertEquals(Priority.HIGH, task.getPriority());
    }

    @Test
    void addTask_blankTitle_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.addTask("", "desc", Priority.LOW));
    }

    @Test
    void markDone_existingTask_changesStatus() {
        var task = service.addTask("Task", "desc", Priority.MEDIUM);
        service.markDone(task.getId());
        assertEquals(TaskStatus.DONE, service.getById(task.getId()).getStatus());
    }

    @Test
    void getById_nonExistent_throwsNotFoundException() {
        assertThrows(TaskNotFoundException.class, () -> service.getById(999));
    }

    @Test
    void filterByStatus_returnsOnlyMatchingTasks() {
        service.addTask("T1", "", Priority.LOW);
        service.addTask("T2", "", Priority.LOW);
        service.markDone(service.getAllTasks().get(0).getId());

        var done = service.filterByStatus(TaskStatus.DONE);
        assertEquals(1, done.size());
    }

    @Test
    void deleteTask_existingTask_removesIt() {
        var task = service.addTask("Delete me", "", Priority.LOW);
        assertTrue(service.deleteTask(task.getId()));
        assertTrue(service.getAllTasks().isEmpty());
    }
}
```

**Run tests:**
```bash
mvn test
```

---

## Completion Checklist

- [ ] Project compiles with `mvn compile`
- [ ] All 6 `TaskServiceTest` tests pass with `mvn test`
- [ ] Can add a task via CLI
- [ ] Can list all tasks
- [ ] Can mark a task as done
- [ ] Can filter by status
- [ ] Can delete a task
- [ ] Invalid task ID shows a clean error message (no stack trace to user)
- [ ] Blank title is rejected with a clear error

## What You Practiced

| Topic | Where used |
|-------|-----------|
| OOP | `Task`, `TaskService`, inheritance of exceptions |
| Enums with fields | `Priority`, `TaskStatus` |
| Collections | `ArrayList`, `Collections.unmodifiableList` |
| Streams | `filter`, `map`, `sorted`, `collect`, `groupingBy` |
| Optional | `findById()` return type |
| Custom exceptions | `TaskNotFoundException` |
| Generics | `List<Task>`, `Map<TaskStatus, Long>` |
| Maven | Project structure, JUnit dependency |
| Unit testing | `TaskServiceTest` |
