# Week 8 — Day 2: Architecture Decision Records (ADRs)

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. What Is an ADR? (10 min)

An **Architecture Decision Record** is a short document that captures a significant architectural decision: what was decided, why, and what the consequences are.

**Why ADRs matter:**
- New team members can understand *why* the system is built this way — not just *what* it does
- Prevents re-litigating the same decisions ("why are we using Kafka again?")
- Creates accountability — decisions are dated, authored, and context-documented
- Enables learning — you can look back and see if a decision aged well

**What deserves an ADR?**
- Technology choices (databases, frameworks, messaging systems)
- Structural choices (microservices split, API design)
- Process choices (testing strategy, deployment model)
- Anything where a reasonable engineer could disagree with the choice

---

### 2. ADR Format (10 min)

The most common format (Michael Nygard's template):

```markdown
# ADR-001: Use PostgreSQL as Primary Database

**Date**: 2024-01-15  
**Status**: Accepted  
**Author**: Alice Chen  
**Deciders**: Alice Chen, Bob Kumar, Carol Smith

## Context

We need a relational database for the Task Manager API. The data has clear relational 
structure (users have tasks, tasks have owners). We need ACID transactions for 
task status updates and strong query capabilities.

The team has strong SQL experience. We're hosted on AWS.

## Decision

We will use **PostgreSQL 16** as our primary database.

## Rationale

- Full ACID compliance required for task state transitions
- Team has deep PostgreSQL experience — low ramp-up cost
- Rich JSON support (jsonb) if we need schemaless fields later
- Native AWS RDS support with automated backups and read replicas
- Strong full-text search if needed (avoids Elasticsearch for basic use)
- Open source — no licensing cost

Rejected alternatives:
- **MySQL**: less powerful JSON support, weaker window functions
- **MongoDB**: no ACID cross-document transactions; relational structure fits better in SQL
- **DynamoDB**: limited query flexibility, complex operations at read patterns we're building

## Consequences

**Positive**:
- ACID transactions for all task state changes
- Mature tooling (pgAdmin, DataGrip, Flyway migrations)
- Easy local dev with Docker

**Negative**:
- Vertical scaling limits (mitigated with read replicas + sharding later if needed)
- Schema migrations required for changes (discipline needed)

**Risks**:
- Connection pool exhaustion under high load — mitigated with HikariCP tuning and PgBouncer if needed
```

---

### 3. ADR Status Lifecycle (5 min)

| Status | Meaning |
|--------|---------|
| `Proposed` | Under discussion |
| `Accepted` | Decision made, in effect |
| `Deprecated` | Still in use but no longer recommended |
| `Superseded by ADR-XXX` | Replaced by a newer decision |

When a decision changes, you don't edit the old ADR — you write a new one that supersedes it and mark the old one as `Superseded by ADR-00N`. This preserves history.

---

### 4. Where to Store ADRs (5 min)

Best practice: **alongside the code**, in version control.

```
project/
├── src/
├── docs/
│   └── adr/
│       ├── 0001-use-postgresql.md
│       ├── 0002-use-jwt-for-auth.md
│       ├── 0003-kafka-for-async-events.md
│       └── 0004-circuit-breaker-for-feign.md
└── pom.xml
```

Tools:
- **adr-tools** (command-line tool for creating ADRs)
- **Log4brains** (web UI for browsing ADRs)
- Any markdown editor + Git

---

### 5. Writing Good ADR Context (15 min)

The **Context** section is the most important part. It must answer:
- What problem are we trying to solve?
- What are the constraints (team, time, budget, existing systems)?
- What options did we consider?
- What are the decision drivers (why does this matter)?

**Bad context:**
> "We need authentication."

**Good context:**
> "The Task Manager API must authenticate users before they can access task data. Requirements:
> - Token must be stateless (no server-side session — we need horizontal scaling)
> - Token must carry user identity (we need username for task ownership)
> - Token must expire (security requirement: max 1 hour)
> - We have no existing SSO infrastructure
> - Team has zero SAML/OIDC experience but has worked with JWT before
> - External client (web SPA) must store the token client-side"

---

### 6. Practical ADR Examples from Your Project (5 min)

Decisions you've made in this course that deserve ADRs:

| ADR | Decision |
|-----|---------|
| ADR-001 | PostgreSQL as primary database |
| ADR-002 | JWT (stateless) for authentication |
| ADR-003 | Microservices: split at User/Task boundary |
| ADR-004 | Eureka for service discovery |
| ADR-005 | Kafka for async cross-service events |
| ADR-006 | Resilience4j circuit breaker for inter-service calls |
| ADR-007 | Redis for read caching with Cache-Aside pattern |

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the purpose of the "Context" section in an ADR?

**Q2.** When a decision changes, do you edit the old ADR or write a new one? Why?

**Q3.** What four items does a good ADR "Consequences" section cover?

**Q4.** Where should ADRs be stored, and why?

**Q5.** Not every decision needs an ADR. Give 2 examples of decisions that DO need one, and 2 that don't.

---

### Part B — Write an ADR (20 min)

Write a complete ADR for one decision from your project. Choose one:
- **ADR-002: JWT authentication for the Task Manager API**, or
- **ADR-005: Apache Kafka for cross-service events**

Use the template from section 2. Your ADR should include:
- Meaningful Context (not generic)
- Clear Decision
- At least 2 rejected alternatives with reasons
- Positive and negative Consequences
- At least 1 Risk

---

### Answers

**A1.** The Context section explains **why a decision was needed** — the problem, constraints, and driving forces. Without good context, the decision looks arbitrary. Good context lets a future engineer reading the ADR understand: "given these constraints and this situation, this decision made sense." It also helps evaluate later whether the context changed (which may warrant revisiting the decision).

**A2.** Write a **new ADR** and mark the old one as `Superseded by ADR-00N`. Editing the old ADR destroys the historical record of what was decided and when. The old ADR represents what was true at that point in time — it's an immutable fact about the past. The new ADR explains the new decision and why the old one no longer applies. This gives you a complete audit trail of how the architecture evolved.

**A3.** A good Consequences section covers: (1) **Positive consequences** — what benefits does this decision bring?; (2) **Negative consequences** — what do we lose or accept as costs?; (3) **Risks** — what could go wrong, and how is it mitigated?; (4) sometimes: **follow-up decisions** — what other decisions does this one necessitate?

**A4.** ADRs should be stored **in version control alongside the code** (e.g., `docs/adr/` folder in the repository). Reasons: (1) ADRs and code evolve together — code commits and ADRs are in the same history; (2) all developers can access them with `git clone`; (3) PR reviews can include ADR creation; (4) ADRs stay in sync with the code they describe (a separate wiki can drift out of date).

**A5.** Decisions that DO need ADRs: (1) "We'll use Kafka instead of direct REST for cross-service events" (meaningful trade-off, affects multiple teams, hard to reverse); (2) "We'll split the monolith — User and Task are separate services" (structural decision, long-term impact). Decisions that don't need ADRs: (1) "We'll name the variable `taskService` instead of `svc`" (trivial, easily changed); (2) "We'll sort the task list by created date" (UI-level, easily changed, no architectural impact).

**Part B Sample (ADR-005):**
```markdown
# ADR-005: Apache Kafka for Cross-Service Events

**Date**: 2024-02-01
**Status**: Accepted

## Context

task-service needs to notify notification-service when tasks are created or completed.
Requirements:
- task-service must not block waiting for notification-service
- notification-service may be temporarily unavailable (restarts, deploys)
- Events must not be lost during notification-service downtime
- Multiple consumers may need to react to the same event (notifications + analytics)
- Current volume: ~1000 task events/day — low

## Decision

Use Apache Kafka with spring-kafka for async cross-service event publishing.

## Rationale

- Events are durable (retained on disk, not lost when consumer is down)
- Multiple consumer groups can independently read the same events
- Supports replay — add new consumers later that process historical events
- Spring Kafka provides auto-configured producers/consumers

Rejected: RabbitMQ — messages deleted after consume (no replay), fan-out requires extra binding config
Rejected: Direct REST — synchronous coupling, blocks on consumer unavailability

## Consequences

Positive: decoupled services, no data loss during consumer downtime, extensible
Negative: operational complexity (Kafka cluster), eventual consistency
Risk: offset lag if consumer is slow — mitigated with monitoring and consumer group scaling
```
