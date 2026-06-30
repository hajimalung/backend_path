# Week 6 — Day 2: Apache Kafka — Concepts and Architecture

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Why Kafka? (10 min)

In a microservices system, services need to communicate asynchronously. REST is synchronous — if the downstream service is slow or down, your service waits or fails.

**Kafka solves this**: services publish **events** to Kafka. Other services **consume** events independently. The publisher doesn't wait for consumers.

```
Without Kafka (REST):
task-service ──HTTP──> notification-service
             waiting...   (notification-service slow/down → task-service hangs)

With Kafka (Events):
task-service ──event──> [Kafka] ──event──> notification-service (at its own pace)
             returns immediately (Kafka buffers the event)
```

Kafka is also:
- **Durable**: events are persisted to disk — no event lost if consumer is down
- **Replayable**: consumers can re-read past events (unlike message queues that delete after consume)
- **Scalable**: horizontal scaling of both producers and consumers

---

### 2. Core Concepts (20 min)

**Topic**: named log of events. Think of it as a category/channel.
```
topic: "task-created"
topic: "task-completed"
topic: "user-registered"
```

**Partition**: topics are split into ordered partitions for parallelism.
```
topic: task-created (3 partitions)
  Partition 0: [event1, event4, event7, ...]
  Partition 1: [event2, event5, event8, ...]
  Partition 2: [event3, event6, event9, ...]
```
Events within a partition are strictly ordered. Events across partitions are not.

**Offset**: position of an event within a partition. Consumers track their offset — allows resume and replay.

**Broker**: a Kafka server. Production runs a cluster of brokers for fault tolerance.

**Producer**: writes events to a topic.

**Consumer**: reads events from a topic.

**Consumer Group**: a group of consumers sharing the work of reading a topic. Each partition is assigned to exactly one consumer in the group.
```
Consumer Group "notification-group":
  Consumer A ← Partition 0
  Consumer B ← Partition 1 + Partition 2

If Consumer A dies:
  Consumer B ← Partition 0 + Partition 1 + Partition 2 (rebalance)
```

**Key**: events with the same key always go to the same partition (guarantees ordering for related events, e.g., all events for Task #123).

---

### 3. Kafka Architecture Diagram (5 min)

```
Producers                    Brokers                    Consumers
                    ┌─────────────────────┐
task-service ──────→│  topic: task-events │────→ notification-service
                    │  Partition 0 ▓▓▓▓▓▓│
user-service ──────→│  Partition 1 ▓▓▓▓  │────→ analytics-service
                    │  Partition 2 ▓▓▓▓▓ │
                    └─────────────────────┘
                         (data persisted
                          on disk 7 days)
```

---

### 4. Kafka vs RabbitMQ (10 min)

| Aspect | Kafka | RabbitMQ |
|--------|-------|----------|
| Model | Log (pull-based) | Queue (push-based) |
| Retention | Events kept for days/weeks | Messages deleted after consume |
| Replay | Yes — consumers can re-read | No — once consumed, gone |
| Ordering | Per-partition ordering | Per-queue ordering |
| Throughput | Very high (millions/sec) | High (tens of thousands/sec) |
| Use case | Event streaming, audit logs, analytics | Task queues, simple messaging |

**Use Kafka when**: high throughput, need event replay, event sourcing, audit trail, multiple consumer groups reading the same events.

**Use RabbitMQ when**: simple task queues, complex routing (exchanges, bindings), message TTL, dead-letter queues.

---

### 5. Key Configuration Concepts (10 min)

**Replication Factor**: how many broker copies of each partition exist.
- `replication-factor: 3` → 1 leader + 2 replicas
- Survive up to 2 broker failures
- Production minimum: 3

**Retention**: how long Kafka keeps events
- `log.retention.hours=168` (7 days, default)
- Or by size: `log.retention.bytes=1073741824` (1GB)

**Acknowledgment (acks)**:
- `acks=0` — fire and forget (no confirmation, fastest, data loss possible)
- `acks=1` — leader confirms (moderate durability)
- `acks=all` — all replicas confirm (highest durability, slower)

**Consumer offset commit**:
- Auto-commit (every 5s by default): risk of processing same event twice or losing events
- Manual commit: you control when to commit after successful processing

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between a Kafka topic and a Kafka partition?

**Q2.** What is a consumer group and how are partitions assigned within one?

**Q3.** If you have 3 partitions and 5 consumers in the same group, what happens?

**Q4.** Why does using the same key for related events matter?

**Q5.** What is the main advantage Kafka has over a traditional message queue (like RabbitMQ) for an audit log use case?

---

### Part B — Design Challenge (20 min)

Design the Kafka event architecture for your Task Manager microservices:

1. Identify at least 3 events the system should produce (name each topic).
2. For each event, specify: producer, topic name, key (what determines the partition), payload fields.
3. Identify at least 2 consumers for these events.
4. What retention period makes sense for each topic? Why?
5. If `notification-service` goes down for 2 hours, what happens to the events? How does Kafka handle this?

---

### Answers

**A1.** A **topic** is a named category for events — a logical grouping (e.g., `task-created`). A **partition** is a physical unit — each topic is divided into N partitions, each an ordered, append-only log on disk. Partitions enable parallelism: multiple consumers can read from different partitions simultaneously.

**A2.** A consumer group is a set of consumers cooperating to read from a topic. Kafka ensures each partition is assigned to exactly **one consumer** within the group at any time. Different consumer groups read independently — each gets its own copy of all events.

**A3.** Two consumers will be idle — there are only 3 partitions so only 3 consumers can be active simultaneously. Scaling consumers beyond the partition count provides no throughput benefit. To use all 5 consumers, you'd need at least 5 partitions.

**A4.** Events with the same key always go to the same partition, which guarantees **ordering** for that key. For example, if you key task events by `taskId`, all events for Task #123 (created, updated, completed) land in the same partition and are processed in order. Without a key, events are distributed round-robin across partitions and may arrive out of order at consumers.

**A5.** Kafka **persists events** and retains them for a configured period (e.g., 7 days or indefinitely for compliance). A message queue deletes messages after consumption. For an audit log, you want every event preserved so you can replay history, answer "what happened to this record?", and support new consumers that need to read historical data. Kafka enables this; a traditional queue does not.

**Part B Sample:**
```
Events:
1. topic: task-events
   producer: task-service
   key: task-id
   payload: {eventType: CREATED/UPDATED/COMPLETED, taskId, title, ownerUsername, timestamp}
   consumers: notification-service (send email), analytics-service (dashboard stats)
   retention: 30 days (audit trail)

2. topic: user-events
   producer: user-service
   key: user-id
   payload: {eventType: REGISTERED/DEACTIVATED, userId, username, timestamp}
   consumers: notification-service (welcome email), task-service (clean up tasks on deactivate)
   retention: 90 days (compliance)

3. topic: notification-events (dead letter / tracking)
   producer: notification-service
   payload: {notificationId, type, recipientId, status, timestamp}
   retention: 7 days

If notification-service goes down for 2 hours:
- Kafka continues accepting events from task-service
- Events accumulate in the partition (with their offsets)
- When notification-service restarts, it resumes from its last committed offset
- No events are lost — it processes the backlog (may need to handle spikes)
```
