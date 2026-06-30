# Week 8 — Day 3: JVM Performance Tuning

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. JVM Memory Model (10 min)

The JVM divides memory into regions:

```
JVM Process Memory
├── Heap (tunable)
│   ├── Young Generation
│   │   ├── Eden Space           ← new objects created here
│   │   ├── Survivor 0 (S0)     ← survives minor GC
│   │   └── Survivor 1 (S1)
│   └── Old Generation (Tenured) ← long-lived objects
├── Non-Heap
│   ├── Metaspace              ← class metadata (JDK 8+ — no more PermGen)
│   ├── Code Cache             ← JIT-compiled native code
│   └── Thread Stacks          ← one per thread
└── Native Memory (off-heap)   ← NIO buffers, direct memory
```

**Key JVM flags:**
```bash
# Heap size
-Xms512m         # initial heap size
-Xmx2g           # maximum heap size

# In containers — respect container limits
-XX:+UseContainerSupport        # auto-detect container CPU/memory limits
-XX:MaxRAMPercentage=75.0       # use 75% of container RAM for heap

# GC logging (for tuning)
-Xlog:gc*:file=/logs/gc.log:time,uptime,tags
```

**In Docker/Spring Boot:**
```yaml
# docker-compose.yml
environment:
  JAVA_OPTS: "-Xms256m -Xmx512m -XX:+UseContainerSupport"
```

---

### 2. Garbage Collection Basics (10 min)

GC reclaims memory from objects no longer referenced. Two types of collections:

**Minor GC (Young Generation)**:
- Frequent, fast (milliseconds)
- Cleans Eden + Survivors
- Most objects die young (short-lived request objects)

**Major/Full GC (Old Generation + all of heap)**:
- Infrequent, slow (can pause for seconds)
- Triggered when Old Gen fills up
- "Stop-the-world" pauses kill latency

**GC algorithms in Java 21:**
| Algorithm | Flag | When to use |
|-----------|------|-------------|
| G1GC | `-XX:+UseG1GC` | Default in Java 9+ — good balance |
| ZGC | `-XX:+UseZGC` | Low-latency (<1ms pauses) — large heaps |
| Shenandoah | `-XX:+UseShenandoahGC` | Very low latency — GraalVM |
| Serial | `-XX:+UseSerialGC` | Single-core, tiny footprint |

**For Spring Boot microservices** (low latency, moderate heap):
```bash
-XX:+UseZGC                  # Java 21 — near-zero GC pauses
-XX:MaxRAMPercentage=75.0    # in containers
```

---

### 3. Profiling Spring Boot Applications (15 min)

**Actuator Metrics (built-in):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, prometheus
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true  # capture latency histograms
```

Key metrics to monitor:
```
GET /actuator/metrics/jvm.memory.used
GET /actuator/metrics/jvm.gc.pause
GET /actuator/metrics/http.server.requests
GET /actuator/metrics/hikaricp.connections.active
```

**Identifying memory leaks:**
```bash
# Heap dump (to analyze with Eclipse MAT or VisualVM)
jmap -dump:format=b,file=heap.hprof <pid>

# Or configure JVM to dump on OOM
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heap.hprof
```

**Thread dump (to identify deadlocks or blocked threads):**
```bash
jstack <pid> > thread-dump.txt
# Or via Actuator:
GET /actuator/threaddump
```

---

### 4. HikariCP Connection Pool Tuning (10 min)

Database connections are expensive. HikariCP is Spring Boot's default connection pool.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20         # max connections to DB
      minimum-idle: 5               # keep 5 ready when idle
      connection-timeout: 30000     # fail if no connection in 30s
      idle-timeout: 600000          # close idle connections after 10min
      max-lifetime: 1800000         # recycle connections every 30min
      connection-test-query: SELECT 1  # verify connection health
      leak-detection-threshold: 60000  # log if connection held >60s
```

**How to size the pool:**
```
Optimal pool size = (CPU cores) × 2 + effective spindle count (for SSDs: 1)
For 4 CPU cores: 4 × 2 + 1 = 9

For microservices: start with max-pool-size = 10, monitor hikaricp.connections.pending
```

**PostgreSQL max_connections** must be greater than sum of all pool sizes across all instances:
```
10 instances × 20 connections = 200 connections needed
PostgreSQL default: max_connections = 100 → need to increase or use PgBouncer
```

---

### 5. Application Performance Tips (5 min)

**Database queries (most common bottleneck):**
- Use pagination — never `findAll()` on large tables
- Add indexes for queried columns (`@Index` on `@Table`)
- Use `FetchType.LAZY` for associations — avoid loading entire object graphs
- Use projections for read queries — fetch only needed columns
- Monitor slow queries with `spring.jpa.properties.hibernate.format_sql=true` + slow query log

**Spring Boot startup time:**
- Use GraalVM Native Image for fast startup (experimental for complex apps)
- `spring.main.lazy-initialization=true` — defer bean creation until first use
- Avoid heavy `@PostConstruct` methods

**Enable HTTP/2:**
```yaml
server:
  http2:
    enabled: true
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between Minor GC and Full GC? Which one causes significant latency issues?

**Q2.** What JVM flag would you use in a Docker container to limit heap to 75% of container memory?

**Q3.** What is `hikaricp.connections.pending`? What does it indicate if it's consistently > 0?

**Q4.** What is `FetchType.LAZY` and why does it help performance?

**Q5.** Why would you set `leak-detection-threshold` in HikariCP configuration?

---

### Part B — Performance Audit (20 min)

Review your Task Manager project for potential performance issues:

1. Open `TaskRepository` — are there any methods that could return large result sets without pagination?
2. Open your Task entity — are all `@OneToMany` / `@ManyToOne` relationships `LAZY`?
3. Open `application.yml` — do you have HikariCP configured with reasonable pool size?
4. Does `GET /api/tasks` support pagination? If not, add `Pageable` support.
5. Add the following metric check:
   ```bash
   curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
   curl http://localhost:8080/actuator/metrics/jvm.memory.used
   ```
   Document what you observe.

---

### Answers

**A1.** **Minor GC** collects the Young Generation (Eden + Survivors). It's fast (milliseconds), happens frequently, and is mostly stop-the-world for a very short time. **Full GC** (or Major GC) collects the entire heap including Old Generation. It's slow (can pause for hundreds of milliseconds to seconds), happens infrequently, and is stop-the-world. Full GC causes latency spikes and timeout errors in production — minimizing Full GC frequency is a key performance goal.

**A2.** `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`. `UseContainerSupport` tells the JVM to respect container memory limits (instead of using total host memory). `MaxRAMPercentage=75.0` allocates 75% of that container limit to the heap, leaving 25% for non-heap (Metaspace, thread stacks, native memory).

**A3.** `hikaricp.connections.pending` is the number of threads waiting for a database connection because the pool is exhausted. If it's consistently > 0, it means your pool size is too small for the current load — queries are queuing. This causes latency to increase significantly (waiting time + query time). Fix: increase `maximum-pool-size` (within PostgreSQL limits) or optimize queries to release connections faster.

**A4.** `FetchType.LAZY` tells Hibernate to not load the associated collection until it's actually accessed in code. With `EAGER`, loading a `Task` also loads all its related `Comment` objects even if you never use them. Lazy loading only fetches the association when you call `task.getComments()`. This reduces data transfer from DB, reduces memory usage, and avoids loading entire object graphs for simple operations. Tradeoff: `LazyInitializationException` if you access the collection outside an active transaction.

**A5.** `leak-detection-threshold` logs a warning when a connection is held longer than the specified duration (e.g., 60 seconds) without being returned to the pool. This helps detect **connection leaks** — code paths where a connection is borrowed but never returned (e.g., exception thrown before `connection.close()`, missing `@Transactional` boundaries). Without this setting, connection leaks silently exhaust the pool, causing all subsequent requests to hang until the `connection-timeout` is reached.
