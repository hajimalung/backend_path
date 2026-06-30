# Week 8 — Day 1: Participating in Architecture Discussions

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. What Senior Engineers Do in Architecture Discussions (10 min)

Architecture discussions are not just about drawing boxes and arrows. They involve:

**Asking the right questions before designing:**
- What are the functional requirements? (what must it do)
- What are the non-functional requirements? (scale, latency, availability, security)
- What are the constraints? (budget, team skill, existing systems, timeline)
- What does success look like? (metrics, SLAs)

**Common traps that junior engineers fall into:**
- Proposing the most "technically impressive" solution instead of the simplest one that works
- Designing for scale that doesn't exist yet ("we might have 10M users someday")
- Skipping non-functional requirements (then discovering the system needs 99.99% uptime)
- Not questioning assumptions in the requirements

---

### 2. Effective Question Framework for Design Sessions (15 min)

When a new feature or system is being discussed, ask in this order:

**Step 1: Clarify scope**
- "What is the MVP vs. full feature?"
- "Are there existing systems we integrate with or replace?"
- "What are the data ownership boundaries?"

**Step 2: Understand scale and SLAs**
- "How many users/requests per second at peak?"
- "What's the acceptable latency for this operation?"
- "What's the required availability? (99%, 99.9%, 99.99%)"
- "Is this operation on the critical path for users?"

**Step 3: Understand consistency requirements**
- "Is eventual consistency acceptable, or must data be immediately consistent?"
- "What happens if this service is down? Can the user flow continue?"
- "Are there financial or legal implications to incorrect data?"

**Step 4: Identify dependencies**
- "Which other services does this depend on?"
- "What happens to us if those services are slow or unavailable?"
- "Do we own the data model, or does another team?"

**Step 5: Propose options, not one answer**
- "We could do X (simpler, faster to build, less scalable) or Y (more complex, higher performance). Given the current scale, I'd recommend X."

---

### 3. Communicating Trade-offs (15 min)

Senior engineers speak in trade-offs, not absolutes.

**Bad (absolute):**
> "We should use Kafka for this."

**Good (trade-off):**
> "We could use Kafka here for decoupling and reliability — that gives us at-least-once delivery and the ability to replay events. The trade-off is operational complexity: we'd need to manage a Kafka cluster and handle idempotency in consumers. Given that this is a low-volume, non-critical notification, I'd propose starting with a simple REST call with retry and upgrading to Kafka if we see reliability issues."

**Trade-off structure:**
```
"We could [option A], which gives us [benefits] but requires [costs/complexity].
 Alternatively, [option B] is simpler but [limitations].
 Given [current constraints], I'd recommend [X] because [specific reason]."
```

---

### 4. Design Patterns That Come Up Repeatedly (10 min)

When you hear these problems, you should immediately know the pattern:

| Problem | Pattern |
|---------|---------|
| "We need to notify multiple systems when something happens" | Observer / Event-driven |
| "We can't afford for Service A to block when Service B is slow" | Circuit Breaker + Async |
| "The same request gets processed multiple times" | Idempotency key + deduplication |
| "We need to undo a distributed operation" | Saga + compensating transactions |
| "Reads are slow, same data requested repeatedly" | Cache + Cache-Aside |
| "We need different data models for reads vs writes" | CQRS |
| "Our monolith is too hard to change" | Strangler Fig — extract bounded contexts incrementally |
| "We need to evolve DB schema without downtime" | Expand-contract (parallel columns/tables) |

---

### 5. The Strangler Fig Pattern (10 min)

How to migrate a monolith to microservices without a big-bang rewrite:

```
Phase 1: Route new feature traffic to new service
  Monolith ← most traffic
  New Service ← new feature traffic only

Phase 2: Migrate one bounded context
  Monolith ← remaining features
  New Service ← migrated feature + new features

Phase 3: Monolith withers, new services grow
  Service A, Service B, Service C ← everything
  Monolith ← nothing (removed)
```

The key: **never stop shipping**. Each phase is a production deployment. No frozen development. No multi-year rewrites.

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What are non-functional requirements? Give 4 examples.

**Q2.** Why should you propose multiple options rather than one solution in design discussions?

**Q3.** What is the Strangler Fig Pattern? When do you use it?

**Q4.** A team wants to add a feature: "send a welcome email when a user registers." List 3 questions you should ask before designing the solution.

**Q5.** A service needs 99.99% uptime. How many minutes of downtime per year does that allow?

---

### Part B — Discussion Exercise (20 min)

You're in a design session. The requirement: **"Add a comment feature to tasks — users can post comments on any task they own."**

Answer these questions as you would in a real discussion:

1. What clarifying questions would you ask?
2. What are the data model options? (Evaluate at least 2.)
3. Where should the comment service live — in `task-service` or a separate `comment-service`?
4. What is the read pattern? (Who reads comments, how often, what format?)
5. Do you need full-text search on comments? What would you use?

---

### Answers

**A1.** Non-functional requirements define **how** the system performs (not what it does). Examples: (1) Performance/Latency — "API response time < 200ms at p99"; (2) Availability — "system must be up 99.9% of the time"; (3) Scalability — "must handle 10x current load without architecture changes"; (4) Security — "all PII must be encrypted at rest and in transit"; (5) Consistency — "financial data must be immediately consistent"; (6) Observability — "all errors must be alerted within 60 seconds".

**A2.** Proposing multiple options shows you understand trade-offs, not just one technique. It keeps the discussion collaborative — the team chooses based on business context you may not fully know. It demonstrates senior-level thinking: no solution is "best" in isolation — "best" depends on scale, budget, team expertise, and timeline. A single-option recommendation can come across as inflexible or incomplete analysis.

**A3.** Strangler Fig is a migration pattern for incrementally replacing a monolith with microservices. Named after a fig tree that grows around a host tree until it replaces it. You route specific functionality to new services while the monolith continues serving the rest. Over time, the monolith handles less and less until it can be removed. Use it when: you have a working monolith you don't want to disrupt, a large codebase you can't rewrite at once, or you need to keep shipping features during migration.

**A4.** Good questions: (1) "Does 'welcome email' need to be sent synchronously (during registration) or asynchronously? Can registration succeed even if the email fails?" (2) "What's the email delivery SLA — real-time, or is a few minutes delay acceptable?" (3) "Are there cases where we should NOT send a welcome email — bulk-created accounts, test accounts?" (4) "What email provider do we use, and do we have a template system?" (5) "Do we track email open/click rates — does analytics need to be notified?"

**A5.** 99.99% uptime = 0.01% downtime = 0.0001 × 365.25 days × 24h × 60min = **52.6 minutes per year**. That's less than 1 hour/year total. Achieving this requires: no single points of failure, multi-AZ deployment, blue-green or canary deployments, automated failover, redundant databases. This is expensive — validate this SLA is genuinely needed before committing to it.

**Part B Sample:**
```
Clarifying questions:
- "Can viewers (non-owners) see comments, or only the task owner?"
- "Is there a comment limit per task? Character limit per comment?"
- "Do we need reply threads (nested comments) or flat list?"
- "Do we need notifications when someone comments?"
- "What's the read:write ratio? Are comments primarily read or written?"

Data model options:
Option A: Comments table in task-service DB
  CREATE TABLE task_comments(id, task_id FK, author_username, body, created_at)
  Simple, same DB transaction, no cross-service calls for reads
  Risk: comments table grows large, slows task queries

Option B: Separate comment-service with its own DB
  Decoupled, independent scaling
  More complex: task API must federate data from two services
  Better if comments have complex features (moderation, full-text search)

Recommendation: Start with Option A (simpler, same service) — extract to separate service if comments grow large or need specialized features.

Where: task-service initially. Comments are tightly coupled to tasks.

Read pattern: likely paginated list with most recent first. 
Cache most recent comments per task (they're read often, change less).

Full-text search: PostgreSQL full-text search (tsvector/tsquery) for MVP.
Migrate to Elasticsearch if advanced search (facets, highlighting) needed.
```
