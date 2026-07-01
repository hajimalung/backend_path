# Learning Tracker — 2-Month Java Backend Engineering

> Update this file daily. Check off items as you complete them.  
> Start date: \***\*\_\_\_\_\*\*** | Target end date: \***\*\_\_\_\_\*\***

---

## Progress Overview

| Phase                             | Weeks | Status         |
| --------------------------------- | ----- | -------------- |
| Phase 1 — Java Mastery            | 1–2   | ⬜ Not started |
| Phase 2 — Spring Boot             | 3–4   | ⬜ Not started |
| Phase 3 — Microservices           | 5–6   | ⬜ Not started |
| Phase 4 — Architecture & Capstone | 7–8   | ⬜ Not started |

---

## Week 1 — Java Core

> Focus: Learn Java-specific syntax, OOP model, and collections  
> Date: \***\*\_\_\_\_\*\*** to \***\*\_\_\_\_\*\***

### Daily Tasks

- [x] **Day 1** — JDK setup, Java syntax vs your existing language, types, operators, control flow
- [x] **Day 2** — OOP: classes, interfaces, abstract classes, inheritance, polymorphism
- [ ] **Day 3** — Collections: `List`, `Map`, `Set`, `Queue` — when to use each
- [ ] **Day 4** — Generics: `<T>`, bounded types, wildcards
- [ ] **Day 5** — Exceptions: checked vs unchecked, `try-with-resources`, custom exceptions
- [ ] **Day 6** — String API, `record`, `enum` with methods
- [ ] **Day 7** — Practice: solve 5 LeetCode Easy in Java (Two Sum, Reverse String, etc.)

### Checkpoint

- [ ] Can I explain the difference between `ArrayList` and `LinkedList`?
- [ ] Can I write a class hierarchy with interfaces and abstract classes?
- [ ] Can I handle exceptions properly in Java?

**Notes / blockers:**

> _(write notes here)_

---

## Week 2 — Modern Java + Build Tooling

> Focus: Streams, Lambdas, Concurrency, Maven  
> Date: \***\*\_\_\_\_\*\*** to \***\*\_\_\_\_\*\***

### Daily Tasks

- [ ] **Day 1** — Lambda expressions, `Function`, `Predicate`, `Consumer`, `Supplier`
- [ ] **Day 2** — Streams API: `filter`, `map`, `reduce`, `collect`, `groupingBy`
- [ ] **Day 3** — `Optional`, method references (`::` syntax), `Comparator` chaining
- [ ] **Day 4** — Concurrency: `Thread`, `Runnable`, `ExecutorService`, `CompletableFuture`
- [ ] **Day 5** — Maven: `pom.xml` structure, lifecycle, adding dependencies
- [ ] **Day 6** — Start Mini Project 1: CLI Task Manager
- [ ] **Day 7** — Complete and clean up Mini Project 1

### Mini Project 1: CLI Task Manager ✅

> `Task` has: id, title, status (TODO/IN_PROGRESS/DONE), priority  
> Features: add, list all, filter by status, mark done, delete  
> Must use: `ArrayList`, `Stream`, proper OOP

- [ ] Project compiles and runs via Maven
- [ ] All 5 features work
- [ ] Uses Streams for filtering
- [ ] Clean OOP (no logic in `main`)

**Notes / blockers:**

> _(write notes here)_

---

## Week 3 — Spring Boot Core + REST + Persistence

> Focus: IoC/DI, REST APIs, JPA, PostgreSQL  
> Date: \***\*\_\_\_\_\*\*** to \***\*\_\_\_\_\*\***

### Daily Tasks

- [ ] **Day 1** — Spring IoC + DI: `@Component`, `@Service`, `@Repository`, constructor injection
- [ ] **Day 2** — Spring Boot auto-config, starters, `application.yml`, `@Value`
- [ ] **Day 3** — REST APIs: `@RestController`, `@GetMapping/@PostMapping`, DTOs, `@Valid`
- [ ] **Day 4** — Spring Data JPA: `@Entity`, `@Id`, `JpaRepository`, H2 in-memory DB
- [ ] **Day 5** — PostgreSQL: datasource config, `@Transactional`, schema design
- [ ] **Day 6** — Start Mini Project 2: Task Manager REST API
- [ ] **Day 7** — Complete and test Mini Project 2

### Mini Project 2: Task Manager REST API ✅

> Endpoints: `POST /tasks`, `GET /tasks`, `GET /tasks/{id}`, `PUT /tasks/{id}`, `DELETE /tasks/{id}`  
> Uses: PostgreSQL, DTOs (separate from `@Entity`), Bean Validation

- [ ] All 5 endpoints work
- [ ] PostgreSQL connected and persisting data
- [ ] DTOs used (request + response separated from entity)
- [ ] `@Valid` validation on request body

**Notes / blockers:**

> _(write notes here)_

---

## Week 4 — Spring Boot Advanced

> Focus: Advanced JPA, error handling, testing, Spring Security + JWT  
> Date: \***\*\_\_\_\_\*\*** to \***\*\_\_\_\_\*\***

### Daily Tasks

- [ ] **Day 1** — JPQL, `@Query`, native queries, `Pageable`, `Sort`
- [ ] **Day 2** — Global exception handling: `@ControllerAdvice`, `@ExceptionHandler`, error DTOs
- [ ] **Day 3** — Spring Profiles: `application-dev.yml`, `@ConfigurationProperties`
- [ ] **Day 4** — Testing: JUnit 5, Mockito (`@Mock`, `@InjectMocks`), `MockMvc`
- [ ] **Day 5** — Spring Security + JWT: filter chain, `OncePerRequestFilter`, JWT validation
- [ ] **Day 6** — Add JWT auth + error handling to Mini Project 2
- [ ] **Day 7** — Add test coverage to Mini Project 2

### Mini Project 2 Extension Checklist ✅

- [ ] JWT login flow works (`POST /auth/login` returns token)
- [ ] Protected endpoints reject requests without valid JWT
- [ ] All 4xx errors return consistent JSON error format
- [ ] At least 1 unit test (service layer with Mockito)
- [ ] At least 1 integration test (`@SpringBootTest` + `MockMvc`)

**Notes / blockers:**

> _(write notes here)_

---

## Week 5 — Microservices + Docker + Spring Cloud

> Focus: Service decomposition, Docker, Eureka, Gateway, Feign  
> Date: \***\*\_\_\_\_\*\*** to \***\*\_\_\_\_\*\***

### Daily Tasks

- [ ] **Day 1** — Microservices concepts: bounded contexts, tradeoffs, when NOT to split
- [ ] **Day 2** — Docker: `Dockerfile`, `docker-compose.yml`, run Spring Boot in container
- [ ] **Day 3** — Spring Cloud Eureka: setup server + register services
- [ ] **Day 4** — Spring Cloud Gateway: routing, predicates, filters
- [ ] **Day 5** — Feign Client: `@FeignClient`, error handling, fallbacks
- [ ] **Day 6** — Start Mini Project 3: Split Task Manager into 2 services
- [ ] **Day 7** — Complete Mini Project 3 with `docker-compose`

### Mini Project 3: Microservices Task System ✅

> `user-service`: registration, login (JWT)  
> `task-service`: task CRUD, calls `user-service` via Feign  
> Infrastructure: Eureka registry + Spring Cloud Gateway

- [ ] Both services start and register in Eureka
- [ ] API Gateway routes requests to correct service
- [ ] `task-service` calls `user-service` successfully via Feign
- [ ] `docker-compose up` starts the full system

**Notes / blockers:**

> _(write notes here)_

---

## Week 6 — Resilience, Kafka & Observability

> Focus: Circuit breaker, Kafka messaging, distributed tracing  
> Date: \***\*\_\_\_\_\*\*** to \***\*\_\_\_\_\*\***

### Daily Tasks

- [ ] **Day 1** — Resilience4j: `@CircuitBreaker`, retry, rate limiter
- [ ] **Day 2** — Kafka basics: brokers, topics, partitions, consumer groups, offsets
- [ ] **Day 3** — Spring Kafka: `@KafkaListener`, `KafkaTemplate`, serialization
- [ ] **Day 4** — Event-driven patterns: pub/sub, saga choreography vs orchestration
- [ ] **Day 5** — Distributed tracing: Micrometer + Zipkin, correlation IDs
- [ ] **Day 6** — Add Kafka + circuit breaker to Mini Project 3
- [ ] **Day 7** — Add Zipkin tracing + verify trace flows across services

### Mini Project 3 Extension Checklist ✅

- [ ] Task creation publishes a Kafka event
- [ ] `user-service` consumes the event and logs/processes it
- [ ] Circuit breaker on Feign call (returns fallback if `user-service` is down)
- [ ] Traces visible in Zipkin UI across both services

**Notes / blockers:**

> _(write notes here)_

---

## Week 7 — Design Patterns + System Design

> Focus: SOLID, patterns, DDD, CQRS, Redis, CAP theorem  
> Date: \***\*\_\_\_\_\*\*** to \***\*\_\_\_\_\*\***

### Daily Tasks

- [ ] **Day 1** — SOLID principles with Java/Spring examples for each
- [ ] **Day 2** — Creational patterns: Builder, Factory, Singleton (in DI context)
- [ ] **Day 3** — Behavioral patterns: Strategy, Observer, Decorator
- [ ] **Day 4** — DDD basics: bounded context, aggregates, value objects, domain events
- [ ] **Day 5** — CQRS intro: read vs write models, event sourcing concept
- [ ] **Day 6** — Redis caching: `@Cacheable`, `@CacheEvict`, TTL, cache-aside pattern
- [ ] **Day 7** — System design: CAP theorem, eventual consistency, load balancing

### Checkpoint

- [ ] Can I give an example of each SOLID principle violation and how to fix it?
- [ ] Can I explain when to use Strategy vs Decorator pattern?
- [ ] Can I explain CAP theorem with a concrete database example?
- [ ] Can I draw a sequence diagram for a CQRS flow?

**Notes / blockers:**

> _(write notes here)_

---

## Week 8 — Senior Mindset + Capstone

> Focus: Architecture skills, performance, security, final project  
> Date: \***\*\_\_\_\_\*\*** to \***\*\_\_\_\_\*\***

### Daily Tasks

- [ ] **Day 1** — Participating in architecture discussions: right questions for scale/failure/ownership
- [ ] **Day 2** — Writing ADRs: Context / Decision / Consequences format, examples
- [ ] **Day 3** — JVM performance: heap, GC, HikariCP tuning, VisualVM profiling
- [ ] **Day 4** — OWASP Top 10 in Spring: SQL injection, IDOR, secrets management
- [ ] **Day 5** — Start Capstone: e-commerce backend system design
- [ ] **Day 6** — Complete Capstone design + write 1 ADR
- [ ] **Day 7** — Mock planning session: find an open-source design doc, write your technical opinion

### Capstone System Design Checklist ✅

> Design an e-commerce platform backend: user accounts, product catalog, orders, payment (external), notifications

- [ ] Component diagram drawn (services, databases, queues)
- [ ] Service boundaries defined and justified
- [ ] Database type chosen per service (with reasoning)
- [ ] Kafka usage defined for async flows (e.g., order placed → notify)
- [ ] At least 1 failure scenario addressed (e.g., payment service down)
- [ ] 1 ADR written (e.g., "Why we chose PostgreSQL over MongoDB for orders")

**Notes / blockers:**

> _(write notes here)_

---

## Final Self-Assessment

> Complete this at the end of Week 8

### Java

- [ ] Comfortable with Streams, Lambdas, Optional
- [ ] Understand Java's type system, generics, and collections
- [ ] Know when to use `CompletableFuture` vs `ExecutorService`

### Spring Boot

- [ ] Can build a production-ready REST API with security, validation, error handling, and tests
- [ ] Understand Spring's IoC container and how beans are wired
- [ ] Can write unit and integration tests with meaningful coverage

### Microservices

- [ ] Can decompose a monolith into services with clear boundaries
- [ ] Know how to implement service discovery, API gateway, and inter-service calls
- [ ] Can add resilience (circuit breaker) and async messaging (Kafka)
- [ ] Can trace requests across services

### Architecture / Senior Skills

- [ ] Can contribute to architecture discussions with informed opinions
- [ ] Can write clear ADRs
- [ ] Know the OWASP Top 10 and how they apply to Spring apps
- [ ] Understand SOLID, common design patterns, and DDD basics
- [ ] Can apply CAP theorem reasoning to database choices

---

## Resources Quick Reference

| Resource              | URL                                           |
| --------------------- | --------------------------------------------- |
| Baeldung              | https://www.baeldung.com                      |
| Spring Guides         | https://spring.io/guides                      |
| Design Patterns       | https://refactoring.guru/design-patterns/java |
| System Design         | https://bytebytego.com                        |
| Java Roadmap          | https://roadmap.sh/java                       |
| Java Brains (YouTube) | https://www.youtube.com/c/JavaBrainsChannel   |
| Amigoscode (YouTube)  | https://www.youtube.com/@amigoscode           |
| OWASP Top 10          | https://owasp.org/www-project-top-ten/        |
