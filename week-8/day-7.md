# Week 8 — Day 7: Mock Planning Session + Final Self-Assessment

> Study time: 45 min | Assessment: 45 min

---

## Part 1: Mock Planning Session (45 min)

This simulates a real sprint planning / design discussion. Work through this as if you're a participating engineer in a team.

---

### Scenario

Your team is building a **real-time task notification system** for a B2B SaaS product. Product manager presents the following requirements:

> "When a task is assigned to someone or marked as urgent, that person should see a notification within 5 seconds. We expect about 500 active users at peak, each receiving up to 20 notifications per day."

---

### Your Job: Answer These Questions As a Senior Engineer Would

Work through these systematically. Write your answers before reading the sample responses.

**1. Clarifying Questions (what would you ask the PM?)**

Write 5 clarifying questions before you begin designing. Think about: scope, edge cases, constraints, success criteria.

**2. Non-Functional Requirements**

Extract NFRs from the description:
- Latency requirement?
- Scale estimate (messages/second)?
- What happens when the user is offline?
- Does "notification" persist? Can users see old notifications?

**3. Technology Options**

List 3 ways to push notifications to a browser in real-time:
- Option A: HTTP Polling
- Option B: Server-Sent Events (SSE)
- Option C: WebSockets

For each: list pros, cons, and when you'd choose it.

**4. Your Recommendation**

Given the requirements (500 users, 5-second delivery, browser + mobile), which option do you recommend and why? What are the trade-offs?

**5. Backend Design**

Sketch the backend components:
- Which existing service sends the notification trigger?
- Do you need a new service?
- How do notifications get delivered from Kafka event → user's browser?
- What do you store in the DB?

**6. Data Model**

Design the `notifications` table:
- What fields?
- What indexes?
- How do you mark notifications as read?

---

### Sample Answers (Read After Your Own)

**1. Clarifying Questions:**
- "Does this need to work on mobile apps (native) as well, or browsers only?"
- "Should notifications persist? Can users see a notification history?"
- "What happens if the user is offline — do they get the notification when they come back?"
- "Is 5 seconds a hard SLA or a target? What's acceptable degradation?"
- "Are there notification preferences — can users mute specific types?"

**2. NFRs:**
- Latency: ≤5 seconds end-to-end
- Scale: 500 users × 20 notifications/day = 10,000/day ≈ 0.12/second average. Peak ~5/second. Very low.
- Offline: store in DB, deliver when user reconnects
- Persistence: yes — notification history required

**3. Technology Comparison:**

| Option | How it works | Pros | Cons | Use when |
|--------|-------------|------|------|---------|
| Polling | Client requests every N seconds | Simple | Wasteful at scale, adds latency | Very low traffic, simple setup |
| SSE | Server pushes over HTTP (one direction) | Simple, HTTP/1.1 compatible, auto-reconnect | One-way only, no binary | Notifications, live feeds |
| WebSocket | Full-duplex TCP connection | True real-time, bi-directional | Complex, stateful connections | Chat, collaborative editing |

**4. Recommendation:**
> **Server-Sent Events (SSE)** — this is a read-only push use case (server → client). WebSockets are overkill (we don't need client → server streaming). SSE is simpler, HTTP-based, works with standard load balancers, and auto-reconnects. At 500 concurrent users, we easily handle open SSE connections in a single service.

**5. Backend Design:**
```
task-service → publishes TaskAssigned/TaskMarkedUrgent event → [Kafka: notification-events]
                                                                        ↓
                                                          notification-service
                                                          (consumes from Kafka,
                                                           stores in DB,
                                                           pushes to SSE endpoint)

User's browser ←── SSE connection ←── notification-service:
  GET /api/notifications/stream (SSE endpoint, long-lived connection)
  Each time a notification arrives for this user → push to their open SSE connection
```

**6. Data Model:**
```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_username VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,          -- TASK_ASSIGNED, TASK_URGENT
    title VARCHAR(255) NOT NULL,
    body TEXT,
    reference_id BIGINT,               -- taskId (nullable)
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_username, is_read, created_at DESC);
-- Query: unread notifications for a user, ordered by newest
```

---

## Part 2: Final Self-Assessment

Answer each question honestly. These reflect senior-level competencies.

---

### Java + Spring Fundamentals

- [ ] I can explain the difference between interface and abstract class with an example
- [ ] I can write a lambda and use Streams without looking up syntax
- [ ] I can explain IoC and dependency injection from first principles
- [ ] I can configure Spring Profiles for dev/test/prod environments
- [ ] I understand what `@Transactional(readOnly = true)` does and when to use it

### REST API Design

- [ ] I can design a RESTful API for a new domain without help
- [ ] I understand HTTP status codes: 200, 201, 400, 401, 403, 404, 409, 500
- [ ] I can implement global exception handling with `@RestControllerAdvice`
- [ ] I use DTOs to decouple API contracts from database entities
- [ ] I know how to add pagination to list endpoints

### Security

- [ ] I can explain JWT structure (header.payload.signature) and stateless auth
- [ ] I understand RBAC and can implement method-level security with `@PreAuthorize`
- [ ] I know what BCrypt is and why we use it instead of SHA/MD5
- [ ] I can identify at least 3 OWASP Top 10 risks and their mitigations in Spring

### Microservices

- [ ] I can explain what a bounded context is and how it maps to a microservice
- [ ] I understand the trade-offs between synchronous (REST/Feign) and async (Kafka) communication
- [ ] I can set up Eureka service discovery and use `lb://` URIs with Feign
- [ ] I understand what circuit breakers are and when they open/close
- [ ] I can configure Spring Cloud Gateway routes with predicates and filters

### Testing

- [ ] I can write a unit test with Mockito mocks without looking up syntax
- [ ] I can write a controller test with `@WebMvcTest` and `MockMvc`
- [ ] I understand the difference between `@Mock` and `@MockBean`
- [ ] I know what `@Transactional` on a test class does

### Design Patterns & Architecture

- [ ] I can explain and apply Strategy, Observer, and Builder patterns
- [ ] I understand the difference between DDD entities and value objects
- [ ] I know what CAP theorem is and can choose CP vs AP for given scenarios
- [ ] I can propose a caching strategy with appropriate TTLs
- [ ] I understand what an ADR is and have written at least one

### Soft Skills

- [ ] I ask clarifying questions before proposing solutions
- [ ] I present trade-offs rather than single-option recommendations
- [ ] I can estimate storage/throughput requirements from user/request numbers
- [ ] I know when NOT to use microservices or event sourcing

---

## Score Interpretation

Count your checked boxes:

| Score | Assessment |
|-------|-----------|
| 30-35 (85%+) | Ready to participate as junior-senior. Take on design tasks. |
| 22-29 (63-82%) | Solid foundation. Revisit unchecked areas; take on guided design tasks. |
| 15-21 (43-60%) | Good start. Review Weeks 4-7 materials; build more mini-projects. |
| < 15 | Revisit foundational weeks. Focus on Weeks 1-3 fundamentals. |

---

## What's Next?

Regardless of your score, here are concrete next steps:

**For deeper Java mastery:**
- Read *Effective Java* (Joshua Bloch) — 90 specific items, each a lesson
- Contribute to an open-source Spring Boot project
- Solve 30 LeetCode Medium problems using Java (focus on collections + algorithms)

**For interview preparation:**
- Practice the system design questions from *Designing Data-Intensive Applications* (Kleppmann)
- Do mock system design interviews (Pramp, Interviewing.io)
- Review your Task Manager + E-Commerce designs; can you present them in 45 minutes?

**For production experience:**
- Deploy your microservices stack to a real cloud (AWS Free Tier, Railway, Render)
- Set up a real monitoring stack (Grafana + Prometheus + Loki)
- Implement a CI/CD pipeline (GitHub Actions → Docker Hub → deployment)

**Recommended timeline for next 2 months:**
- Month 3: Production deployments, CI/CD, Kubernetes basics
- Month 4: Advanced patterns (event sourcing implementation, CQRS with read stores, reactive programming with WebFlux)

---

## Congratulations

You've covered:
- **Java 21** fundamentals → advanced generics, concurrency, streams
- **Spring Boot 3** → IoC, REST, JPA, Security, profiles, actuator
- **Microservices** → Eureka, Gateway, Feign, Docker
- **Resilience** → Circuit breakers, retries, bulkheads
- **Messaging** → Kafka, event-driven design, sagas
- **Observability** → Distributed tracing, metrics
- **Design patterns** → SOLID, Strategy, Observer, Builder, Decorator
- **Architecture** → DDD, CQRS, Event Sourcing, CAP theorem
- **Security** → JWT, OWASP Top 10, access control
- **Performance** → Redis caching, JVM tuning, HikariCP

This is a foundation that takes most engineers years to build. Keep building, keep shipping, and apply these patterns in real work.
