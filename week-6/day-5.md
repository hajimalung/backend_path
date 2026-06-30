# Week 6 — Day 5: Distributed Tracing with Micrometer + Zipkin

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. The Observability Problem (5 min)

In a monolith, debugging is simple — one log file, one call stack. In microservices, a single user request flows through multiple services:

```
Client → gateway → task-service → user-service (Feign) → database
         ↓
         task-service → Kafka → notification-service
```

If the request fails or is slow, which service caused it? You'd need to correlate logs across 4+ services and 4+ log files — impossible without distributed tracing.

**Distributed tracing** ties all operations in a single request together using shared IDs:
- **Trace ID**: unique ID for the entire request (same across all services)
- **Span ID**: unique ID for one operation within the trace (one per service call, DB query, etc.)
- **Parent Span ID**: which span triggered this one (builds the call graph)

---

### 2. Setup: Micrometer Tracing + Zipkin (15 min)

**Add to each microservice:**
```xml
<!-- Micrometer tracing with Brave (OpenZipkin) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<!-- Sends trace data to Zipkin -->
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
<!-- Actuator exposes metrics endpoints -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**application.yml:**
```yaml
management:
  tracing:
    sampling:
      probability: 1.0          # 1.0 = trace 100% of requests (dev only; use 0.1 in prod)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
    # adds traceId and spanId to every log line
```

**Start Zipkin with docker-compose:**
```yaml
zipkin:
  image: openzipkin/zipkin:3
  ports:
    - "9411:9411"
```

Open `http://localhost:9411` — Zipkin UI for viewing traces.

---

### 3. How Trace Context Propagates (15 min)

Spring Boot auto-configures trace context propagation:
- For **HTTP** (RestTemplate, Feign, WebClient): injects `traceparent` header
- For **Kafka**: injects trace headers into Kafka record headers
- For **Spring MVC** / Gateway: extracts incoming trace context

```
Incoming request:
  traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
                   └── trace-id ─────────────────┘└── span-id ──────┘

Feign call from task-service to user-service automatically adds:
  traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-b9c7c989f97918e1-01
                   └── same trace-id ─────────────┘└── new span-id ─┘
```

All logs across all services with the same `traceId` belong to one user request.

---

### 4. Custom Spans and Tags (15 min)

Create custom spans for important operations:

```java
@Service
public class TaskService {

    private final Tracer tracer;           // from micrometer-tracing
    private final TaskRepository taskRepository;

    public TaskService(Tracer tracer, TaskRepository taskRepository) {
        this.tracer = tracer;
        this.taskRepository = taskRepository;
    }

    public TaskResponse create(CreateTaskRequest request, String username) {
        // Create a custom span
        Span span = tracer.nextSpan().name("task.create");
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            span.tag("task.title", request.title());
            span.tag("task.priority", request.priority().name());
            span.tag("owner.username", username);

            Task saved = taskRepository.save(
                new Task(request.title(), request.description(), request.priority(), username));

            span.tag("task.id", saved.getId().toString());
            span.event("task-saved");
            return TaskResponse.from(saved);
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

---

### 5. Metrics with Micrometer (10 min)

Micrometer provides application metrics (separate from tracing but often used together):

```java
@Service
public class TaskService {

    private final MeterRegistry meterRegistry;
    private final Counter taskCreatedCounter;
    private final Timer taskCreateTimer;

    public TaskService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.taskCreatedCounter = Counter.builder("tasks.created")
            .description("Number of tasks created")
            .register(meterRegistry);
        this.taskCreateTimer = Timer.builder("tasks.create.time")
            .description("Time to create a task")
            .register(meterRegistry);
    }

    public TaskResponse create(CreateTaskRequest request, String username) {
        return taskCreateTimer.record(() -> {
            Task saved = taskRepository.save(...);
            taskCreatedCounter.increment();
            meterRegistry.gauge("tasks.total", taskRepository.count());
            return TaskResponse.from(saved);
        });
    }
}
```

Expose via Actuator:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
```

Access: `GET /actuator/metrics/tasks.created`

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is a trace ID? What is a span ID? How are they related?

**Q2.** How does trace context propagate from `task-service` to `user-service` when using Feign?

**Q3.** What is the sampling probability? Why would you set it to 0.1 in production?

**Q4.** What is the difference between tracing and metrics?

**Q5.** What log pattern change do you make so that every log line includes the trace ID?

---

### Part B — Hands-on (20 min)

1. Add Micrometer Tracing + Zipkin dependencies to all your services.
2. Configure `management.tracing.sampling.probability: 1.0` in each service.
3. Update the log pattern to include `%X{traceId}`.
4. Start Zipkin with docker-compose.
5. Make an HTTP call that flows through: gateway → task-service → user-service (Feign).
6. Open `http://localhost:9411`, search for traces, and verify:
   - You see one trace spanning multiple services
   - Click on the trace to see individual spans
   - The trace ID appears in logs of both services

---

### Answers

**A1.** A **trace ID** is a globally unique identifier for the entire request journey across all services — every span in the same user request shares the same trace ID. A **span ID** is a unique identifier for one operation within the trace (a single HTTP call, a DB query, a Kafka send). Spans are linked by parent-child relationships: the span in `task-service` is the parent of the span in `user-service` (triggered by the Feign call). Together, they form a directed acyclic graph (DAG) representing the full call tree.

**A2.** When `task-service` makes a Feign call to `user-service`, Micrometer Tracing automatically injects the W3C `traceparent` header (or B3 `X-B3-TraceId`/`X-B3-SpanId` headers) into the outgoing HTTP request. Spring Boot's Feign + Micrometer integration handles this automatically — no code needed. `user-service`'s servlet filter extracts these headers and creates a child span with the same trace ID.

**A3.** Sampling probability controls what percentage of requests are traced. `1.0` = trace all requests. In production with high traffic (thousands of req/s), tracing everything creates huge amounts of data — storing and querying it is expensive and adds overhead to every request. `0.1` = sample 10%, which gives enough data for performance analysis and debugging without overwhelming your trace storage.

**A4.** **Tracing** captures the journey of individual requests — useful for debugging why a specific request was slow or failed. It answers: "what happened to request X?". **Metrics** are aggregated numerical measurements over time (counters, gauges, histograms) — useful for monitoring system health and alerting. They answer: "how is the system performing overall?" (p99 latency, error rate, requests/second). Both are part of observability alongside logs.

**A5.** Update the logging pattern in `application.yml`:
```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]"
```
`%X{traceId:-}` reads the `traceId` from the MDC (Mapped Diagnostic Context) — Micrometer Tracing puts it there automatically. If no trace is active, `-` is shown as fallback. This makes every log line include the trace and span IDs, enabling log aggregation tools (ELK, Grafana Loki) to group logs by trace.
