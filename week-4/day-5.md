# Week 4 — Day 5: Spring Security + JWT Authentication

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Spring Security Overview (10 min)

Spring Security is a filter chain that intercepts every HTTP request:

```
HTTP Request
    ↓
[SecurityFilterChain]
    ↓
UsernamePasswordAuthenticationFilter (form login)
    ↓
JwtAuthenticationFilter (custom — we'll build this)
    ↓
AuthorizationFilter (checks if user has permission)
    ↓
Your Controller
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

---

### 2. JWT — JSON Web Token (10 min)

A JWT is a signed, self-contained token:
```
header.payload.signature

eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSIsImlhdCI6MTcwMDAwMDAwMH0.abc123
```

- **Header**: algorithm used (`HS256`)
- **Payload**: claims (user ID, email, roles, expiry)
- **Signature**: HMAC of header + payload using a secret — prevents tampering

Tokens are **stateless** — the server doesn't need to store sessions. Verify the signature, trust the payload.

---

### 3. JWT Utility Class (15 min)

```java
@Component
public class JwtUtils {

    @Value("${app.security.jwt-secret}")
    private String jwtSecret;

    @Value("${app.security.jwt-expiration-ms:3600000}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);    // throws if expired or invalid signature
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

---

### 4. Security Configuration (20 min)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())           // disabled — using JWT, not sessions
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // no sessions
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()   // public endpoints
                .requestMatchers("/h2-console/**").permitAll() // H2 console (dev)
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()                 // everything else requires auth
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(h -> h.frameOptions(fo -> fo.disable())) // for H2 console iframe
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
```

**JWT Filter — runs on every request:**
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);  // no token — continue without auth
            return;
        }

        String token = authHeader.substring(7);      // remove "Bearer " prefix

        if (jwtUtils.isTokenValid(token)) {
            String username = jwtUtils.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
```

**Auth Controller:**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authManager, JwtUtils jwtUtils) {
        this.authManager = authManager;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        String token = jwtUtils.generateToken(request.username());
        return ResponseEntity.ok(Map.of("token", token));
    }
}

record LoginRequest(String username, String password) {}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What are the three parts of a JWT? What does the signature protect against?

**Q2.** Why do we set session management to `STATELESS` in a JWT-based API?

**Q3.** What does `OncePerRequestFilter` guarantee?

**Q4.** What does `SecurityContextHolder.getContext().setAuthentication(authToken)` do?

**Q5.** Why should you use `BCryptPasswordEncoder` instead of plain SHA or MD5?

---

### Part B — Coding Challenge (20 min)

1. Add Spring Security to your Mini Project 2 with JWT.

2. Create a `User` entity with `username` and `password` (hashed). Implement `UserDetailsService` loading users from the DB.

3. Create `POST /api/auth/register` — register a new user (hash password with BCrypt).

4. Create `POST /api/auth/login` — validate credentials, return JWT.

5. Test:
   - Register: `curl -X POST /api/auth/register -d '{"username":"alice","password":"pass123"}'`
   - Login: `curl -X POST /api/auth/login -d '{"username":"alice","password":"pass123"}'` → get token
   - Access protected: `curl -H "Authorization: Bearer <token>" /api/tasks`
   - Access without token → 403 Forbidden

---

### Answers

**A1.** Header (algorithm), Payload (claims — user data), Signature (HMAC of header+payload using a secret key). The signature protects against **tampering** — if anyone modifies the payload, the signature won't match and the token is rejected.

**A2.** JWT is stateless — user info is embedded in the token. No server-side session is needed. `STATELESS` tells Spring Security to not create or use HTTP sessions, which is important for scalability (any server instance can validate any token without shared session storage).

**A3.** `OncePerRequestFilter` guarantees the filter's `doFilterInternal()` runs **exactly once per request**, even in cases where the filter chain might be invoked multiple times (e.g., during request dispatching). It prevents duplicate authentication processing.

**A4.** It stores the authenticated user's identity in the `SecurityContext` (a thread-local store) for the duration of this request. All security checks downstream (like `@PreAuthorize`, `.authenticated()`) read from here to know who the current user is.

**A5.** BCrypt is specifically designed for password hashing — it's **intentionally slow** (configurable work factor), includes a **salt** automatically (preventing rainbow table attacks), and the hash includes the salt so you don't need to store it separately. MD5/SHA are too fast (brute-force is cheap) and don't include salt by default.

**Part B Solution sketch:**
```java
// UserDetailsService implementation
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
            .map(u -> User.withUsername(u.getUsername())
                .password(u.getPassword())
                .roles("USER")
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}

// Register endpoint
@PostMapping("/register")
public ResponseEntity<String> register(@RequestBody LoginRequest req) {
    if (userRepository.existsByUsername(req.username())) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already taken");
    }
    AppUser user = new AppUser(req.username(), passwordEncoder.encode(req.password()));
    userRepository.save(user);
    return ResponseEntity.status(HttpStatus.CREATED).body("User registered");
}
```
