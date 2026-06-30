# Week 6 — Day 4: Event-Driven Patterns

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Event-Driven Architecture Overview (10 min)

In **event-driven architecture (EDA)**, services communicate by producing and consuming **events** (things that happened) rather than calling each other directly.

```
Traditional (REST):
task-service ──calls──> notification-service ──calls──> email-service
             (chains of sync calls — tight coupling)

Event-Driven:
task-service ──publishes──> [task-completed] event
                                  │
notification-service ─────────────┘ (subscribes independently)
analytics-service ────────────────── (subscribes independently)
audit-service ────────────────────── (subscribes independently)
```

**Key properties:**
- **Loose coupling**: task-service doesn't know who consumes its events
- **Easy extensibility**: add a new consumer without touching task-service
- **Resilience**: consumers work at their own pace; task-service doesn't wait

---

### 2. Event vs Command vs Query (10 min)

| Type | Meaning | Direction | Example |
|------|---------|-----------|---------|
| **Event** | Something happened (past tense) | Broadcast | `TaskCompleted`, `UserRegistered` |
| **Command** | Do something (imperative) | Targeted | `SendEmail`, `CreateUser` |
| **Query** | Ask for data | Request/response | `GetTaskById` |

**Events are facts** — they're immutable records of what happened. No one can tell an event "don't happen". Anyone can react to it.

Good event names are past tense: `TaskCreated`, `PaymentProcessed`, `OrderShipped`.

---

### 3. Event Payload Design (10 min)

**Thin event** (just IDs — consumer fetches details):
```json
{
  "eventType": "TASK_COMPLETED",
  "taskId": 123,
  "completedAt": "2024-01-15T10:30:00Z"
}
```
Pros: small payload, always fresh data. Cons: consumer must call back → re-coupling, extra network call.

**Fat event** (includes all relevant data):
```json
{
  "eventType": "TASK_COMPLETED",
  "taskId": 123,
  "title": "Buy groceries",
  "ownerEmail": "alice@example.com",
  "priority": "HIGH",
  "completedAt": "2024-01-15T10:30:00Z"
}
```
Pros: consumer is self-contained, no extra calls. Cons: larger payload, data may be stale if consumer is slow.

**Recommendation**: use fat events for notification/analytics; thin events when consumers need authoritative current state.

---

### 4. Saga Pattern — Distributed Transactions (20 min)

Without a shared database, how do you handle operations that span multiple services?

**Traditional approach** (doesn't work in microservices): two-phase commit (2PC) — distributed transactions. Too slow, too fragile.

**Saga pattern**: break a distributed operation into a sequence of local transactions, each publishing an event.

#### Choreography Saga (Event-based):
```
1. OrderService creates order → publishes OrderPlaced
2. InventoryService reserves stock → publishes StockReserved
3. PaymentService charges card → publishes PaymentProcessed
4. ShippingService ships → publishes OrderShipped

On failure:
PaymentService fails → publishes PaymentFailed
InventoryService listens → releases stock reservation
OrderService listens → marks order as Failed
```

Pros: no central coordinator, very decoupled. Cons: hard to visualize the overall flow, difficult to debug.

#### Orchestration Saga (Process coordinator):
```
OrderOrchestrator:
  1. → reserve inventory
  2. ← inventory reserved
  3. → process payment
  4. ← payment processed
  5. → ship order
  6. ← order shipped
  
  On any failure:
  → release inventory (compensating transaction)
  → refund payment (if already charged)
```

Pros: easier to understand and debug, explicit flow. Cons: orchestrator knows about all services (more coupling).

---

### 5. Eventual Consistency (10 min)

In a distributed system with async events, data is **eventually consistent** — for a brief window, different services may show different states.

Example:
```
1. User creates task → task-service saves it (immediately consistent)
2. task-service publishes TaskCreated event
3. analytics-service is slightly behind — for a few milliseconds it shows 
   the old count. Then it processes the event and becomes consistent.
```

**Design for it:**
- Show loading states in UI for counts/aggregates
- Use timestamps to detect stale data
- Design idempotent consumers — safe to process same event twice
- Add an `eventId` (UUID) to events; consumer tracks processed IDs to deduplicate

**Idempotent consumer:**
```java
@KafkaListener(topics = "task-events")
public void handle(TaskEvent event, Acknowledgment ack) {
    if (processedEventRepository.existsByEventId(event.eventId())) {
        log.info("Duplicate event {}, skipping", event.eventId());
        ack.acknowledge();
        return;
    }
    // process event
    processedEventRepository.save(new ProcessedEvent(event.eventId()));
    ack.acknowledge();
}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between an event, a command, and a query in event-driven systems?

**Q2.** What are the trade-offs between thin events and fat events?

**Q3.** What is a saga pattern? When do you need it?

**Q4.** What is the difference between choreography and orchestration sagas?

**Q5.** What is eventual consistency? Give an example of a user-visible impact.

---

### Part B — Design Challenge (20 min)

Design a **task completion saga** for your system:

1. When a task is marked COMPLETED in `task-service`:
   - Award achievement points in `user-service`
   - Send a congratulation email via `notification-service`
   - Log it in an `audit-service`

2. Design this as a **choreography saga**:
   - What events are published?
   - Who publishes each?
   - What happens if the points-awarding fails?

3. Design the same flow as an **orchestration saga** — how would a `TaskCompletionOrchestrator` coordinate it?

4. For the notification step: should you use a thin or fat event? Justify.

---

### Answers

**A1.** An **event** is a notification that something happened — past tense, immutable, broadcast to anyone interested (`TaskCompleted`). A **command** is a directive to perform an action — targeted at a specific service, the sender cares about the outcome (`SendEmail`). A **query** is a request for current data — synchronous request/response (`GetUser`). Events decouple producer from consumer; commands create explicit dependencies; queries are read-only.

**A2.** **Thin events**: small payload, consumer fetches latest data (always fresh, but requires calling back which re-couples services). **Fat events**: include all needed data (consumer is self-contained, no extra calls, but payload is larger and data could be slightly stale by the time it's processed). Use fat events when consumers need the data for their own processing without querying; use thin events when the consumer always needs the freshest state.

**A3.** A saga is a pattern for managing distributed transactions across multiple microservices, where each service has its own database (no shared DB, no 2PC). A saga breaks the operation into a sequence of local transactions, each publishing an event. If a step fails, compensating transactions roll back previous steps. You need sagas whenever a business operation spans data in multiple services.

**A4.** **Choreography**: services react to each other's events independently — no central coordinator. Each service knows what to do when it receives an event. Pros: decoupled; Cons: hard to see the big picture, hard to debug. **Orchestration**: a dedicated orchestrator service sends commands to each participant and reacts to their responses, coordinating the whole flow. Pros: explicit flow visible in one place; Cons: orchestrator knows about all participants (coupling).

**A5.** Eventual consistency means all parts of a distributed system will converge to the same state eventually, but there's a time window where different services show different data. User-visible: a user completes a task; the dashboard shows the completion immediately (from task-service), but the analytics page still shows the old total task count for a few milliseconds until the analytics-service processes the `TaskCompleted` event. The user might see a stale count briefly.

**Part B Sample:**
```
Choreography:
1. task-service: marks task DONE → publishes TaskCompleted (fat event with userId, taskTitle)
2. user-service: consumes TaskCompleted → awards points → publishes PointsAwarded
   On failure: publishes PointsAwardingFailed
3. notification-service: consumes TaskCompleted → sends email (independently of points)
4. audit-service: consumes TaskCompleted → logs entry

Failure handling:
If PointsAwarding fails: publish PointsAwardingFailed
No rollback needed for task completion (idempotent/independent steps)
Task remains DONE — points failure is a separate concern, may retry

Orchestration:
TaskCompletionOrchestrator:
  1. Listens for TaskCompleted event
  2. Sends AwardPoints command to user-service
  3. Receives PointsAwarded or PointsAwardingFailed
  4. Sends SendCompletionEmail command to notification-service
  5. Sends LogAuditEntry command to audit-service

Fat event for notification: notification-service needs ownerEmail + taskTitle
to send the email without making additional API calls → use fat event.
```
