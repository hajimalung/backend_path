# 2-Month Java Backend Engineering Roadmap

> **Goal**: Reach senior software engineer level in Java, Spring Boot, and Microservices  
> **Profile**: Has programming experience in another language (Python/JS/C++)  
> **Commitment**: 1–2 hours/day (working professional)  
> **Start date**: Track in `tracker.md`

---

## Learning Architecture

```
Month 1                          Month 2
─────────────────────────────    ──────────────────────────────────
Week 1 │ Java Core               Week 5 │ Microservices + Docker
Week 2 │ Modern Java + Tooling   Week 6 │ Resilience + Kafka
Week 3 │ Spring Boot Core        Week 7 │ Design Patterns + System Design
Week 4 │ Spring Boot Advanced    Week 8 │ Senior Mindset + Capstone
```

### Milestone Checkpoints

| End of Week | You should be able to... |
|-------------|--------------------------|
| Week 2 | Build a working Java CLI app with clean OOP and collections |
| Week 4 | Build a secured, tested Spring Boot REST API with a real database |
| Week 6 | Run multiple microservices communicating via REST and Kafka |
| Week 8 | Contribute to system design discussions and write ADRs |

---

## Phase 1 — Java Mastery (Weeks 1–2)

### Week 1: Java Core (Fast-Track for Polyglots)

Since you already know programming concepts, this week is about learning Java's specific flavour — static typing, the JVM, and its OOP model.

| Day | Topic | Key Concepts |
|-----|-------|--------------|
| 1 | JDK setup + Java syntax crash course | JDK vs JRE, `javac`, types, operators, control flow |
| 2 | OOP in Java | Classes, interfaces, abstract classes, inheritance, polymorphism |
| 3 | Collections Framework | `List`, `Map`, `Set`, `Queue` — choosing the right one |
| 4 | Generics | `<T>`, bounded types `<T extends Comparable>`, wildcards `<?>` |
| 5 | Exceptions | Checked vs unchecked, `try-with-resources`, custom exceptions |
| 6 | String API, `record` (Java 16+), `enum` with methods | Immutability, pattern matching `instanceof` |
| 7 | Practice | Solve 5 LeetCode Easy problems in Java (focus on syntax, not algorithms) |

**Resources:**
- [Java Syntax Crash Course for Python devs](https://learnxinyminutes.com/docs/java/)
- [Baeldung — Java Collections Guide](https://www.baeldung.com/java-collections)
- [JetBrains Academy — Java track](https://hyperskill.org/tracks/1) (free tier)

---

### Week 2: Modern Java + Build Tooling

| Day | Topic | Key Concepts |
|-----|-------|--------------|
| 1 | Lambda expressions + functional interfaces | `Function<T,R>`, `Predicate<T>`, `Consumer<T>`, `Supplier<T>` |
| 2 | Streams API | `filter`, `map`, `reduce`, `collect`, `groupingBy`, `flatMap` |
| 3 | `Optional`, method references, `Comparator` chaining | Avoiding `NullPointerException`, `::` syntax |
| 4 | Concurrency basics | `Thread`, `Runnable`, `ExecutorService`, `CompletableFuture` |
| 5 | Maven fundamentals | `pom.xml`, lifecycle (`compile`, `test`, `package`), dependencies |
| 6–7 | **Mini Project 1** | CLI Task Manager — CRUD in-memory using Collections + Streams + OOP |

**Mini Project 1 spec**: Build a command-line Task Manager. A `Task` has id, title, status (TODO/IN_PROGRESS/DONE), and priority. Support: add task, list all, filter by status, mark as done, delete. Use `ArrayList`, `Stream`, and proper OOP.

**Resources:**
- [Baeldung — Java 8 Streams](https://www.baeldung.com/java-8-streams)
- [Baeldung — CompletableFuture](https://www.baeldung.com/java-completablefuture)
- [Maven in 5 Minutes](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)

---

## Phase 2 — Spring Boot (Weeks 3–4)

### Week 3: Spring Boot Core + REST + Persistence

| Day | Topic | Key Concepts |
|-----|-------|--------------|
| 1 | Spring IoC + Dependency Injection | `@Component`, `@Service`, `@Repository`, `@Bean`, `@Autowired`, constructor injection |
| 2 | Spring Boot auto-configuration | `@SpringBootApplication`, starters, `application.yml`, `@Value` |
| 3 | Building REST APIs | `@RestController`, `@GetMapping/@PostMapping`, DTOs, `@Valid`, `@RequestBody` |
| 4 | Spring Data JPA — basics | `@Entity`, `@Id`, `@GeneratedValue`, `JpaRepository`, H2 in-memory |
| 5 | PostgreSQL integration | `application.yml` datasource config, `@Transactional`, schema design |
| 6–7 | **Mini Project 2** | REST CRUD API — Task Manager backend with PostgreSQL |

**Mini Project 2 spec**: Rebuild your CLI Task Manager as a REST API. Endpoints: `POST /tasks`, `GET /tasks`, `GET /tasks/{id}`, `PUT /tasks/{id}`, `DELETE /tasks/{id}`. Use PostgreSQL, DTOs (separate from `@Entity`), and Bean Validation (`@NotBlank`, `@NotNull`).

**Resources:**
- [Spring Boot Getting Started](https://spring.io/guides/gs/spring-boot/)
- [Baeldung — Spring Data JPA](https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa)
- [Amigoscode — Spring Boot YouTube series](https://www.youtube.com/@amigoscode)

---

### Week 4: Spring Boot Advanced

| Day | Topic | Key Concepts |
|-----|-------|--------------|
| 1 | JPQL + custom queries | `@Query`, native queries, `Pageable`, `Sort`, `Page<T>` |
| 2 | Global exception handling | `@ControllerAdvice`, `@ExceptionHandler`, `ProblemDetail` (RFC 7807) |
| 3 | Spring Profiles + Configuration | `application-dev.yml`, `application-prod.yml`, `@ConfigurationProperties` |
| 4 | Testing — Unit + Integration | JUnit 5, Mockito (`@Mock`, `@InjectMocks`), `@SpringBootTest`, `MockMvc` |
| 5 | Spring Security basics + JWT | Filter chain, `SecurityFilterChain`, `OncePerRequestFilter`, JWT parsing |
| 6–7 | **Mini Project 2 Extension** | Add JWT auth + global error handling + full test suite to your Task API |

**Resources:**
- [Baeldung — Spring Security + JWT](https://www.baeldung.com/spring-security-oauth-jwt)
- [Baeldung — MockMvc](https://www.baeldung.com/integration-testing-in-spring)
- [Baeldung — @ControllerAdvice](https://www.baeldung.com/exception-handling-for-rest-with-spring)

---

## Phase 3 — Microservices (Weeks 5–6)

### Week 5: Microservices Architecture + Docker + Spring Cloud

| Day | Topic | Key Concepts |
|-----|-------|--------------|
| 1 | Microservices concepts | Bounded contexts, monolith vs microservices tradeoffs, Conway's Law, when NOT to split |
| 2 | Docker fundamentals | `Dockerfile`, `docker-compose.yml`, containerizing a Spring Boot app |
| 3 | Spring Cloud Eureka | Service registration, `@EnableEurekaServer`, `@EnableDiscoveryClient` |
| 4 | Spring Cloud Gateway | Routing, predicates, filters, load balancing |
| 5 | Inter-service communication | OpenFeign (`@FeignClient`), `WebClient`, when to use each |
| 6–7 | **Mini Project 3** | Split Task Manager into `user-service` + `task-service` running via `docker-compose` with Eureka + Gateway |

**Mini Project 3 spec**: `user-service` handles registration/login (JWT). `task-service` handles tasks, calls `user-service` via Feign to validate ownership. Both register with Eureka. All traffic enters through the Gateway.

**Resources:**
- [Spring Cloud Netflix Eureka docs](https://spring.io/projects/spring-cloud-netflix)
- [Java Brains — Microservices YouTube playlist](https://www.youtube.com/c/JavaBrainsChannel)
- [Docker Getting Started](https://docs.docker.com/get-started/)

---

### Week 6: Resilience, Messaging & Observability

| Day | Topic | Key Concepts |
|-----|-------|--------------|
| 1 | Resilience4j | Circuit breaker, retry, bulkhead, rate limiter, `@CircuitBreaker` annotation |
| 2 | Apache Kafka basics | Brokers, topics, partitions, consumer groups, offset management |
| 3 | Spring Kafka | `@KafkaListener`, `KafkaTemplate`, `@EnableKafka`, serialization |
| 4 | Event-driven patterns | Pub/sub, saga pattern (choreography vs orchestration), eventual consistency |
| 5 | Distributed tracing | Micrometer Tracing + Zipkin, trace/span IDs, correlation ID propagation |
| 6–7 | **Mini Project 3 Extension** | When a task is created, publish a Kafka event; `user-service` consumes it. Add circuit breaker on Feign calls. |

**Resources:**
- [Resilience4j Spring Boot docs](https://resilience4j.readme.io/docs/getting-started-3)
- [Baeldung — Kafka with Spring](https://www.baeldung.com/spring-kafka)
- [Baeldung — Distributed Tracing with Micrometer](https://www.baeldung.com/spring-boot-3-observability)

---

## Phase 4 — Architecture & Senior Mindset (Weeks 7–8)

### Week 7: Design Patterns + System Design

| Day | Topic | Key Concepts |
|-----|-------|--------------|
| 1 | SOLID principles | With concrete Java/Spring Boot examples for each principle |
| 2 | Creational patterns | Builder, Factory Method, Singleton (and why Spring's DI replaces most Singletons) |
| 3 | Behavioral patterns | Strategy, Observer, Decorator — all appear inside Spring itself |
| 4 | Domain-Driven Design basics | Bounded context, aggregates, value objects, domain events, anti-corruption layer |
| 5 | CQRS + Event Sourcing intro | Read vs write model separation, when to use it, complexity tradeoffs |
| 6 | Redis caching | `@Cacheable`, `@CacheEvict`, cache-aside pattern, TTL strategy |
| 7 | System design fundamentals | CAP theorem, eventual consistency, horizontal scaling, load balancing strategies |

**Resources:**
- *Effective Java* — Joshua Bloch (essential for Java idioms)
- *Building Microservices* — Sam Newman (architecture thinking)
- [refactoring.guru](https://refactoring.guru/design-patterns/java) — Design patterns in Java
- [ByteByteGo](https://bytebytego.com) — System design visuals

---

### Week 8: Senior Engineer Execution + Capstone

| Day | Topic | Key Concepts |
|-----|-------|--------------|
| 1 | Participating in architecture discussions | Asking the right questions: scale, SLAs, failure modes, data ownership |
| 2 | Architecture Decision Records (ADRs) | Format: Context / Decision / Consequences, when to write one |
| 3 | JVM performance basics | Heap, GC tuning, HikariCP connection pool config, profiling with VisualVM |
| 4 | Security — OWASP Top 10 in Spring context | SQL injection, IDOR, mass assignment, secrets in env vars, not in code |
| 5–6 | **Capstone System Design** | Design an e-commerce order system: draw component diagram, identify service boundaries, write 1 ADR, choose DB per service |
| 7 | Mock planning session | Find a real open-source RFC or design doc, write your own technical opinion/counter-proposal |

**Capstone Design Prompt**: Design the backend for an e-commerce platform. It should handle: user accounts, product catalog, order processing, payment (external), and notifications (email/SMS). Define: service boundaries, APIs between services, database choices per service, message queue usage, and failure handling.

**Resources:**
- [Architecture Decision Records — GitHub examples](https://github.com/joelparkerhenderson/architecture-decision-record)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [VisualVM — JVM profiler](https://visualvm.github.io/)

---

## Book List (Priority Order)

| Priority | Book | Why |
|----------|------|-----|
| Must-read | *Effective Java* — Joshua Bloch | Java idioms, common pitfalls, best practices |
| Must-read | *Building Microservices* — Sam Newman | Architecture thinking, service design |
| Recommended | *Spring Boot in Action* — Craig Walls | Practical Spring Boot reference |
| Recommended | *Designing Data-Intensive Applications* — Kleppmann | Distributed systems intuition |
| Reference | *Clean Code* — Robert Martin | Code quality habits |

---

## Key Websites

| Site | Use For |
|------|---------|
| [baeldung.com](https://www.baeldung.com) | Best Spring Boot/Java reference on the internet |
| [spring.io/guides](https://spring.io/guides) | Official getting-started guides |
| [roadmap.sh/java](https://roadmap.sh/java) | Visual learning path |
| [refactoring.guru](https://refactoring.guru) | Design patterns with Java examples |
| [bytebytego.com](https://bytebytego.com) | System design concepts visually explained |
| [javaguides.net](https://www.javaguides.net) | Spring Boot tutorials |

---

## Technology Stack Used in This Plan

| Layer | Technology |
|-------|-----------|
| Language | Java 21 (LTS) |
| Build tool | Maven |
| Framework | Spring Boot 3.x |
| Database | PostgreSQL + H2 (for tests) |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT |
| Testing | JUnit 5 + Mockito + Testcontainers |
| Messaging | Apache Kafka |
| Service mesh | Spring Cloud (Eureka + Gateway) |
| Resilience | Resilience4j |
| Tracing | Micrometer + Zipkin |
| Containers | Docker + docker-compose |
| Caching | Redis + Spring Cache |
