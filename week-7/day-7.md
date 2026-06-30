# Week 7 — Day 7: System Design Fundamentals

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. How to Approach System Design (5 min)

Senior engineers are expected to participate in designing systems. The typical process:

1. **Clarify requirements** — functional (what it does) and non-functional (scale, latency, availability)
2. **Estimate scale** — users, requests per second, data size
3. **High-level design** — components and their interactions
4. **Detailed design** — dive into critical components
5. **Identify bottlenecks** — what breaks at scale? How do you fix it?

---

### 2. CAP Theorem (10 min)

> A distributed system can only guarantee two of three properties simultaneously:

- **C — Consistency**: every read returns the most recent write (or an error)
- **A — Availability**: every request gets a response (not necessarily the most recent data)
- **P — Partition Tolerance**: system continues despite network partitions (nodes can't communicate)

In a real distributed system, **network partitions always happen** — so you must choose between **C and A** when a partition occurs.

**CP systems** (Consistency + Partition Tolerance): return an error rather than stale data. Example: distributed databases like HBase, Zookeeper. Use when: financial data, inventory counts.

**AP systems** (Availability + Partition Tolerance): return best-available data even if stale. Example: Cassandra, CouchDB. Use when: user profiles, social media feeds.

**CA systems** (Consistency + Availability): can't exist in a distributed system (all real systems have partitions). Traditional single-node RDBMS is effectively CA — because there's only one node, no partition is possible.

---

### 3. Horizontal vs Vertical Scaling (10 min)

**Vertical scaling (Scale Up)**: add more resources to one server (more RAM, faster CPU, bigger disk).
- Pros: simple, no code changes
- Cons: has physical limits, single point of failure, downtime during upgrades

**Horizontal scaling (Scale Out)**: add more servers, distribute load.
- Pros: theoretically unlimited, fault tolerant (some can fail)
- Cons: requires load balancer, statelessness, distributed coordination

**Making services horizontally scalable:**
1. **No local state** — don't store sessions or data in memory (use Redis, DB)
2. **Idempotent operations** — safe to retry if a request hits a different instance
3. **External config** — configs via env vars, not hardcoded

Your Spring Boot services (stateless, JWT auth) are already horizontally scalable — run multiple instances behind a load balancer.

---

### 4. Load Balancers and Patterns (10 min)

**Layer 4 (TCP)**: routes by IP/port — fast, no content inspection. Examples: AWS NLB.

**Layer 7 (HTTP)**: routes by URL, headers, cookies — smarter. Examples: Nginx, AWS ALB, Spring Cloud Gateway.

**Load balancing algorithms:**
- **Round Robin**: requests distributed sequentially A→B→C→A→B→C
- **Least Connections**: send to server with fewest active connections (better for long-lived requests)
- **IP Hash**: same client always hits same server (useful for session affinity — but avoid with JWT)
- **Weighted**: send more traffic to more powerful servers

---

### 5. Database Scaling Patterns (10 min)

**Read Replicas**: one primary (writes), multiple replicas (reads). Most systems are read-heavy (80/20 rule).
```
Writes → Primary DB
Reads  → Replica 1, Replica 2, Replica 3 (load balanced)
```

**Database Sharding**: split data horizontally across multiple DBs by a shard key.
```
userId % 3 == 0 → DB Shard 0
userId % 3 == 1 → DB Shard 1
userId % 3 == 2 → DB Shard 2
```
Pros: scales writes. Cons: complex — cross-shard queries are hard, choosing the right shard key is critical.

**Connection Pooling**: databases have limited connections. Use a connection pool (HikariCP in Spring Boot):
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20         # max connections to DB
      minimum-idle: 5               # keep 5 ready
      connection-timeout: 30000     # fail fast if no connection available in 30s
      idle-timeout: 600000          # close idle connections after 10min
```

---

### 6. Eventual Consistency in System Design (5 min)

When you scale out, strict consistency is expensive. Accept eventual consistency where it makes sense:

**Strictly consistent** (ACID transactions): financial transactions, inventory deduction, auth.

**Eventually consistent** (acceptable staleness): social media likes, view counts, recommendation engines, notification read status.

Design pattern for eventual consistency:
1. Write to primary DB (immediately consistent)
2. Async event triggers downstream updates
3. Read may see stale data briefly — design UI to handle this (loading states, optimistic updates)

---

### 7. Back-of-Envelope Estimation (10 min)

Interviewers and planning sessions often ask for quick estimates. Key numbers to memorize:

| Operation | Approximate latency |
|-----------|-------------------|
| L1 cache access | 1 ns |
| L2 cache access | 4 ns |
| Main memory access | 100 ns |
| SSD read | 100 µs |
| HDD read | 10 ms |
| Network round-trip (same datacenter) | 0.5 ms |
| Network round-trip (cross-continent) | 150 ms |

| Unit | Size |
|------|------|
| KB | 10³ bytes |
| MB | 10⁶ bytes |
| GB | 10⁹ bytes |
| TB | 10¹² bytes |

**Example estimate**: Task Manager for 1M users, 10 tasks each, 100 bytes per task:
- Storage: 1M × 10 × 100B = 1GB — trivial
- Reads: assume 1% DAU (10K users), each making 5 reads = 50K reads/day ≈ 0.6 reads/second — easily handled by 1 DB

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** Explain CAP theorem. If you're building a payment system, do you choose CP or AP? Why?

**Q2.** What makes a Spring Boot service horizontally scalable?

**Q3.** What is a read replica? What problem does it solve?

**Q4.** What is the difference between Round Robin and Least Connections load balancing?

**Q5.** Estimate: you need to store 5 years of audit logs, 1000 events/day, 500 bytes per event. How much storage do you need?

---

### Part B — System Design Mini (20 min)

Design a URL shortener (like bit.ly):

1. **Requirements**: user submits a long URL, gets a short URL back. Anyone with the short URL gets redirected to the long URL.
2. **Scale**: 100 writes/second, 10,000 reads/second.
3. **Design**: sketch the components (API, DB, cache). What do you store? How do you generate the short code?
4. **Why is heavy caching appropriate here?**
5. **Should this be AP or CP? Justify.**

---

### Answers

**A1.** CAP states a distributed system can guarantee only 2 of: Consistency (all reads see latest write), Availability (every request gets a response), Partition Tolerance (survives network splits). In a real distributed system, partitions happen — so choose C or A when partition occurs. **Payment systems: CP**. If a partition occurs, it's safer to return an error ("service unavailable") than to let a payment proceed without knowing if another instance already processed it — double charges or inconsistent balances are unacceptable. Correctness > availability.

**A2.** A Spring Boot service is horizontally scalable when: (1) **Stateless** — no in-memory session or user state (JWT auth helps here — token carries state); (2) **Externalized config** — all config via env vars, no hardcoded values; (3) **Idempotent** — retrying a request to a different instance produces the same result; (4) **Shared storage** — session data in Redis, files in S3, not local disk.

**A3.** A read replica is a copy of the primary database that receives changes via replication. Write operations go only to the primary; read operations are distributed across replicas. It solves: (1) **read scalability** — most apps are read-heavy; replicas handle read load; (2) **read performance** — reads don't compete with writes on the primary; (3) **geographic distribution** — replicas in different regions reduce latency.

**A4.** **Round Robin**: requests distributed in a fixed sequence (A, B, C, A, B, C). Simple, even distribution — good when all requests are similar in duration. **Least Connections**: send request to the server with the fewest active connections — good when requests vary in processing time (some long, some short). Least Connections is fairer when you have slow requests that can pile up on one server.

**A5.** 5 years × 365 days × 1000 events × 500 bytes = 5 × 365 × 500,000 bytes = 5 × 365 × 0.5 MB ≈ 912 MB ≈ 1 GB. Trivially small — a single PostgreSQL instance handles this easily. Even at 10x scale (10,000 events/day), it's ~10 GB — still manageable with indexes and periodic archiving.

**Part B: URL Shortener Design:**
```
API Service (Spring Boot, stateless, 3 instances):
  POST /shorten → { longUrl } → returns { shortCode: "abc123", shortUrl: "https://sho.rt/abc123" }
  GET  /{code}  → 302 redirect to longUrl

Short code generation:
  Option 1: random 6-char base62 string (62^6 = 56 billion possible codes)
  Option 2: increment integer → encode in base62 (avoids collision check)

Storage (PostgreSQL):
  Table: url_mappings(code VARCHAR PK, long_url TEXT, created_at, owner)
  Index: PRIMARY KEY on code (read-heavy — B-tree index)

Caching (Redis, TTL 24h):
  Key: shortcode → long_url
  ON WRITE: cache new mapping immediately
  ON READ: check cache first, then DB

Why cache is ideal: reads >> writes (100:1 ratio from the prompt — 10,000 reads/sec vs 100 writes/sec).
The same popular URLs are read repeatedly. URL mappings rarely change after creation.

AP or CP:
  AP — if Redis is down, fall back to DB (still available, slightly slower)
  If a partition splits write and read nodes briefly, serving a slightly stale URL is acceptable
  (worst case: URL not found for a few seconds, user retries). Not a safety issue.
  Strong consistency is NOT required — use AP.
```
