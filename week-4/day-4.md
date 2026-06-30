# Week 4 — Day 4: Testing — JUnit 5, Mockito, MockMvc

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Testing Pyramid Overview (5 min)

```
        /\
       /  \
      / E2E\           — Few: slow, test full app
     /------\
    / Integr \         — Some: test layers together (DB, HTTP)
   /----------\
  /   Unit     \       — Many: fast, isolated, mock dependencies
 /--------------\
```

**Unit tests**: test a single class in isolation with mocked dependencies.
**Integration tests**: test multiple layers together (e.g., controller → service → real DB).
**E2E tests**: test full HTTP flows (we'll cover this in Week 8).

---

### 2. JUnit 5 Basics (15 min)

```xml
<!-- Already included in spring-boot-starter-test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    @BeforeAll
    static void beforeAll() {
        System.out.println("Runs ONCE before all tests in class");
    }

    @BeforeEach
    void setUp() {
        System.out.println("Runs before EACH test — set up fresh state");
    }

    @AfterEach
    void tearDown() {
        System.out.println("Runs after each test — cleanup");
    }

    @Test
    void basic_assertions() {
        assertEquals(4, 2 + 2);
        assertNotEquals(5, 2 + 2);
        assertTrue("hello".contains("ell"));
        assertFalse("".contains("x"));
        assertNull(null);
        assertNotNull("value");
    }

    @Test
    void exception_test() {
        assertThrows(IllegalArgumentException.class, () -> {
            throw new IllegalArgumentException("bad input");
        });

        // Capture exception message
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> { throw new IllegalArgumentException("bad input"); });
        assertEquals("bad input", ex.getMessage());
    }

    @Test
    void assertion_with_message() {
        // Message shown only on failure
        assertEquals(4, 2 + 2, "Math should work");
    }

    @Test
    @DisplayName("Task with blank title should throw")
    void blankTitleThrows() {
        // @DisplayName shows a readable name in test reports
    }

    @Test
    @Disabled("Not implemented yet")
    void skippedTest() { }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void blankTitlesAreInvalid(String title) {
        assertThrows(IllegalArgumentException.class,
            () -> new TaskService(null).validateTitle(title));
    }
}
```

---

### 3. Mockito — Mocking Dependencies (20 min)

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)  // enables Mockito annotations
class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;    // fake implementation

    @Mock
    AppProperties appProperties;

    @InjectMocks
    TaskService taskService;          // real class, mocks injected

    @Test
    void create_validRequest_savesTask() {
        // Arrange — define mock behaviour
        when(appProperties.getMaxTasks()).thenReturn(100);
        when(taskRepository.count()).thenReturn(5L);
        when(taskRepository.existsByTitleIgnoreCase("Learn JPA")).thenReturn(false);

        Task savedTask = new Task("Learn JPA", "desc", Priority.HIGH);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        CreateTaskRequest request = new CreateTaskRequest("Learn JPA", "desc", Priority.HIGH);

        // Act
        TaskResponse result = taskService.create(request);

        // Assert
        assertNotNull(result);
        assertEquals("Learn JPA", result.title());

        // Verify the mock was called as expected
        verify(taskRepository).save(any(Task.class));     // called once
        verify(taskRepository, times(1)).count();          // called exactly once
        verify(taskRepository, never()).deleteById(any()); // never called
    }

    @Test
    void findById_notFound_throwsException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,
            () -> taskService.findById(999L));

        verify(taskRepository).findById(999L);
    }

    @Test
    void create_exceedsLimit_throwsException() {
        when(appProperties.getMaxTasks()).thenReturn(5);
        when(taskRepository.count()).thenReturn(5L);

        CreateTaskRequest req = new CreateTaskRequest("Task", null, Priority.LOW);
        assertThrows(IllegalStateException.class, () -> taskService.create(req));

        verify(taskRepository, never()).save(any());  // save should NOT be called
    }

    @Test
    void markDone_updatesStatus() {
        Task task = new Task("Test", "desc", Priority.LOW);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenReturn(task);

        taskService.markDone(1L);

        assertEquals(TaskStatus.DONE, task.getStatus());
        verify(taskRepository).save(task);
    }
}
```

---

### 4. MockMvc — Integration Testing Controllers (15 min)

Tests the HTTP layer without a real server:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)   // only loads web layer + TaskController
class TaskControllerTest {

    @Autowired
    MockMvc mockMvc;                 // simulated HTTP client

    @MockBean                        // Spring mock — replaces real bean
    TaskService taskService;

    @Autowired
    ObjectMapper objectMapper;       // Jackson for JSON serialization

    @Test
    void getAll_returnsOk() throws Exception {
        List<TaskResponse> tasks = List.of(
            new TaskResponse(1L, "Task 1", null, TaskStatus.TODO, Priority.HIGH, null, null)
        );
        when(taskService.findAll(null)).thenReturn(tasks);

        mockMvc.perform(get("/api/tasks")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())                          // 200
            .andExpect(jsonPath("$.length()").value(1))          // array has 1 element
            .andExpect(jsonPath("$[0].title").value("Task 1"))   // field check
            .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(taskService.findById(999L)).thenThrow(new TaskNotFoundException(999L));

        mockMvc.perform(get("/api/tasks/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Task not found with ID: 999"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest("New Task", null, Priority.HIGH);
        TaskResponse resp = new TaskResponse(1L, "New Task", null, TaskStatus.TODO, Priority.HIGH, null, null);
        when(taskService.create(any())).thenReturn(resp);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("New Task"));
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        String invalidJson = """
            {"title": "", "priority": "HIGH"}
            """;

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.title").exists());
    }
}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between `@Mock` and `@MockBean`? When do you use each?

**Q2.** What does `@ExtendWith(MockitoExtension.class)` do?

**Q3.** What is the difference between `@WebMvcTest` and `@SpringBootTest`?

**Q4.** What does `verify(repo, never()).save(any())` assert?

**Q5.** What does `@ParameterizedTest` with `@ValueSource` allow you to do?

---

### Part B — Coding Challenge (20 min)

Write tests for your Mini Project 2:

1. `TaskServiceTest` using Mockito (`@ExtendWith(MockitoExtension.class)`):
   - `create_validInput_returnsResponse()`
   - `create_duplicateTitle_throwsDuplicateTitleException()`
   - `findById_notFound_throwsTaskNotFoundException()`
   - `delete_existingTask_callsDeleteById()`

2. `TaskControllerTest` using `@WebMvcTest(TaskController.class)`:
   - `getAll_returnsTaskList()`
   - `getById_existingId_returns200()`
   - `getById_missingId_returns404()`
   - `create_missingPriority_returns400()`

Run with `mvn test` and verify all pass.

---

### Answers

**A1.** `@Mock` (Mockito) creates a mock object for pure unit tests — no Spring context loaded, very fast. `@MockBean` (Spring) creates a mock AND registers it as a Spring bean in the ApplicationContext, replacing the real bean. Use `@Mock` + `@ExtendWith(MockitoExtension.class)` for unit tests; use `@MockBean` in `@WebMvcTest` or `@SpringBootTest`.

**A2.** It registers Mockito's `JUnit 5 extension` which: (1) processes `@Mock` and `@InjectMocks` annotations, (2) validates mock usage after each test, (3) creates mocks before each test and resets them after. Without this, Mockito annotations don't work.

**A3.** `@WebMvcTest(Controller.class)` loads **only the web layer** (controller, filter, `@ControllerAdvice`) — no service, repo, or DB. Very fast. `@SpringBootTest` loads the **full Spring ApplicationContext** — all beans, auto-config, actual DB connections. Use `@WebMvcTest` for controller tests (mock the service), `@SpringBootTest` for end-to-end integration tests.

**A4.** Asserts that the `save()` method on `repo` was **never called** during the test. Useful for verifying that a guard condition (like limit check or duplicate check) prevented the save from happening.

**A5.** It runs the same test method once for each value in `@ValueSource`, passing each as a method argument. In the example, the test runs 4 times — once for `""`, `" "`, `"\t"`, `"\n"`. Great for testing boundary conditions and multiple invalid inputs without duplicating test code.
