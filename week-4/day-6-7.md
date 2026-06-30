# Week 4 — Day 6 & 7: Mini Project 2 Extension — Auth + Tests

> Build time: 2 days (~1 hour each) | No separate test — this IS the test

---

## Goal

Extend your Task Manager API with:
1. JWT Authentication (register + login)
2. Task ownership (tasks belong to the logged-in user)
3. Full test suite (unit + integration)

---

## Day 6 — Authentication Integration

### Step 1: Update User Entity

```java
@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Task> tasks = new ArrayList<>();

    protected AppUser() {}
    public AppUser(String username, String password) {
        this.username = username; this.password = password;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
```

### Step 2: Update Task Entity — Add Owner

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "owner_id", nullable = false)
private AppUser owner;
```

### Step 3: Auth Service

```java
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;

    // constructor injection...

    public void register(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("Username already taken: " + username);
        }
        userRepository.save(new AppUser(username, passwordEncoder.encode(rawPassword)));
    }

    public String login(String username, String password) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );
        return jwtUtils.generateToken(username);
    }
}
```

### Step 4: Get Current User in TaskService

```java
@Service
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private AppUser getCurrentUser() {
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public TaskResponse create(CreateTaskRequest request) {
        AppUser owner = getCurrentUser();
        Task task = new Task(request.title(), request.description(), request.priority(), owner);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll(TaskStatus status) {
        AppUser owner = getCurrentUser();
        List<Task> tasks = (status != null)
            ? taskRepository.findByOwnerAndStatus(owner, status)
            : taskRepository.findByOwner(owner);
        return tasks.stream().map(TaskResponse::from).toList();
    }
}
```

### Step 5: Update Repository

```java
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByOwner(AppUser owner);
    List<Task> findByOwnerAndStatus(AppUser owner, TaskStatus status);
    Optional<Task> findByIdAndOwner(Long id, AppUser owner);
}
```

---

## Day 7 — Full Test Suite

### Unit Tests: AuthService

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authManager;
    @Mock JwtUtils jwtUtils;
    @InjectMocks AuthService authService;

    @Test
    void register_newUser_savesWithHashedPassword() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("hashed");

        authService.register("alice", "pass");

        verify(userRepository).save(argThat(u ->
            u.getUsername().equals("alice") && u.getPassword().equals("hashed")));
    }

    @Test
    void register_duplicateUsername_throwsException() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> authService.register("alice", "pass"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsToken() {
        when(jwtUtils.generateToken("alice")).thenReturn("jwt.token.here");
        String token = authService.login("alice", "pass");
        assertEquals("jwt.token.here", token);
        verify(authManager).authenticate(any());
    }
}
```

### Unit Tests: TaskService

```java
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;
    @InjectMocks TaskService taskService;

    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new AppUser("alice", "hashed");
        // Mock SecurityContext
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("alice");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_validRequest_savesTaskWithOwner() {
        Task saved = new Task("Buy Milk", null, Priority.LOW, mockUser);
        when(taskRepository.save(any())).thenReturn(saved);

        CreateTaskRequest req = new CreateTaskRequest("Buy Milk", null, Priority.LOW);
        TaskResponse result = taskService.create(req);

        assertEquals("Buy Milk", result.title());
        verify(taskRepository).save(argThat(t -> t.getOwner().equals(mockUser)));
    }
}
```

### Integration Tests: AuthController

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional                           // rollback after each test
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void register_and_login_returnsToken() throws Exception {
        // Register
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"testuser","password":"pass123"}"""))
            .andExpect(status().isCreated());

        // Login
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"testuser","password":"pass123"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andReturn();

        String token = objectMapper.readTree(
            result.getResponse().getContentAsString()).get("token").asText();
        assertFalse(token.isBlank());
    }

    @Test
    void accessProtectedEndpoint_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isForbidden());
    }

    @Test
    void accessProtectedEndpoint_withValidToken_returns200() throws Exception {
        // Register + login to get a real token
        String token = registerAndLogin("user2", "pass");

        mockMvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    private String registerAndLogin(String user, String pass) throws Exception {
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"username":"%s","password":"%s"}""".formatted(user, pass)));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"username":"%s","password":"%s"}""".formatted(user, pass)))
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("token").asText();
    }
}
```

### Run Tests
```bash
mvn test
# Target: all tests pass (green)
```

---

## Completion Checklist

- [ ] `POST /api/auth/register` creates user with hashed password
- [ ] `POST /api/auth/login` returns JWT token
- [ ] `GET /api/tasks` without token → 403
- [ ] `GET /api/tasks` with valid token → 200 (only own tasks)
- [ ] Creating a task links it to the authenticated user
- [ ] `AuthServiceTest` — 3 unit tests pass
- [ ] `TaskServiceTest` — covers create and findAll with mocked security
- [ ] `AuthControllerIntegrationTest` — full register → login → access flow passes
- [ ] `mvn test` shows 0 failures

## What You Practiced

| Concept | Where used |
|---------|-----------|
| Spring Security filter chain | `SecurityConfig`, `JwtAuthenticationFilter` |
| JWT token generation/validation | `JwtUtils` |
| BCrypt password hashing | `AuthService.register()` |
| SecurityContextHolder | `TaskService.getCurrentUser()` |
| `@OneToMany` with ownership | `AppUser` → `Task` |
| Unit tests with SecurityContext mock | `TaskServiceTest` |
| Integration tests with `@SpringBootTest` | `AuthControllerIntegrationTest` |
| Transactional test rollback | `@Transactional` on test class |
