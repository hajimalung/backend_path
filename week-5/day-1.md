# Week 5 — Day 1: Microservices Concepts

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. What Are Microservices? (10 min)

Microservices is an architectural style where an application is built as a collection of **small, independently deployable services**, each running in its own process and communicating over a network.

**Monolith vs Microservices:**

| Aspect | Monolith | Microservices |
|--------|----------|---------------|
| Deployment | All or nothing | Deploy each service independently |
| Scaling | Scale the whole app | Scale only the bottleneck service |
| Tech stack | One per app | Each service can use different tech |
| Data | Single shared database | Each service owns its data |
| Team | One big team | Small teams per service |
| Failure | One bug can crash all | Failures are isolated |
| Complexity | Simple to start | Complex distributed system |

---

### 2. When NOT to Use Microservices (10 min)

This is what senior engineers know that juniors often don't.

**Don't use microservices when:**
- Team is small (< 5-10 engineers) — overhead outweighs benefits
- Domain is not well understood — splitting too early creates wrong boundaries
- You need low latency — network calls add overhead vs in-process calls
- Strong data consistency is critical — distributed transactions are hard
- You're building a prototype or MVP — iterate faster with a monolith

**Martin Fowler's advice**: *Start with a monolith, migrate to microservices when you feel the pain.*

Microservices introduce: network latency, service discovery, distributed tracing, eventual consistency, complex deployments. None of these exist in a monolith.

---

### 3. Bounded Contexts (DDD) (10 min)

A **bounded context** defines the boundary within which a specific domain model applies. It's the natural unit of a microservice.

```
E-commerce system bounded contexts:

┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Catalog    │  │    Orders    │  │   Payment    │  │ Notification │
│              │  │              │  │              │  │              │
│ Product      │  │ Order        │  │ Transaction  │  │ Email        │
│ Category     │  │ LineItem     │  │ Invoice      │  │ SMS          │
│ Inventory    │  │ Shipping     │  │ Refund       │  │ Push         │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
     │                  │                  │                  │
     └──────────────────┴──────────────────┴──────────────────┘
                     Each has its own DB!
```

**Key rule**: A service should be the **single source of truth** for its domain. Other services request data via API, not by reading the same database.

---

### 4. How Services Communicate (15 min)

**Synchronous (request/response):**
- REST over HTTP — simple, well-understood
- gRPC — binary protocol, very fast, uses protobuf

```
OrderService ──HTTP POST──> PaymentService
             <──200 OK──────
```

Pros: simple, immediate response. Cons: tight coupling, if PaymentService is down, OrderService fails.

**Asynchronous (event-driven):**
- Message broker (Kafka, RabbitMQ)
- OrderService publishes `OrderPlaced` event, NotificationService consumes it

```
OrderService ──event──> [Kafka] ──event──> NotificationService
```

Pros: loose coupling, resilient to failures. Cons: eventual consistency, harder to debug.

**Choosing between them:**
- Use sync when you need an immediate response (e.g., "is payment approved?")
- Use async when you can proceed without the response (e.g., "send confirmation email")

---

### 5. Conway's Law (5 min)

> "Any organization that designs a system will produce a design whose structure is a copy of the organization's communication structure." — Melvin Conway

In practice: if your team is split by domain (Catalog team, Orders team, Payment team), your microservices will naturally align with those boundaries. If your team is split by layer (frontend team, backend team, DB team), you'll build a layered monolith.

**Implication**: Microservices require organizational changes, not just technical ones. You can't have 3 teams sharing one database table and claim to have microservices.

---

### 6. The 8 Fallacies of Distributed Computing (10 min)

Things developers wrongly assume when building distributed systems:
1. The network is reliable
2. Latency is zero
3. Bandwidth is infinite
4. The network is secure
5. Topology doesn't change
6. There is one administrator
7. Transport cost is zero
8. The network is homogeneous

**Why this matters**: Every microservice call is a network call. You must design for: timeouts, retries, partial failures, and eventual consistency — none of which exist in a monolith.

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What are two specific reasons you should NOT use microservices for a startup MVP?

**Q2.** What does "each service owns its data" mean? Why is this important?

**Q3.** Give one situation where synchronous REST between services is appropriate and one where async messaging is better.

**Q4.** What is Conway's Law? What does it imply for team structure?

**Q5.** What does "bounded context" mean? In an e-commerce app, name 3 natural bounded contexts.

---

### Part B — Design Challenge (20 min)

Design the service boundaries for a simplified **ride-sharing app** (like Uber):

1. Identify at least 4 bounded contexts (services)
2. For each service, list: its responsibilities and its data (what tables it owns)
3. Choose the communication type (sync/async) for these flows:
   - Driver accepts a ride request
   - Ride is completed → send receipt email
   - User requests a ride → check driver availability
4. Identify one data consistency challenge this split creates

---

### Answers

**A1.** (Any two of): (1) Domain not understood yet — splitting creates wrong boundaries you'll regret; (2) Small team — distributed systems need more operational overhead (observability, service discovery, multiple deployments); (3) Need to iterate fast — changing a monolith is faster than coordinating API changes across services; (4) Strong consistency needs — implementing distributed transactions (sagas) is complex.

**A2.** Each microservice is the sole owner of its database tables — no other service can access them directly. This prevents tight coupling through the database. If services shared a DB, a schema change in one place could break another service silently. Ownership forces explicit API contracts.

**A3.** Sync: When a service needs an answer to proceed — e.g., Order Service calls Payment Service to authorize payment before confirming the order. Async: When a service can continue without waiting — e.g., after an order is placed, publish an event to trigger email notification. The order doesn't need to wait for the email to be sent.

**A4.** Systems mirror the communication structure of the organization that built them. If the Payments team and Orders team are separate, they'll build separate services with a clear API boundary. Implication: to build true microservices, you need autonomous teams — each owning their service end-to-end (DB, code, deployment).

**A5.** A bounded context is a boundary within which a specific model and language are consistent. Three natural bounded contexts in e-commerce: **Catalog** (products, pricing, inventory), **Orders** (shopping cart, order lifecycle, fulfillment), **Payments** (transactions, invoices, refunds).

**Part B Sample Answer:**
```
Services:
1. User Service — accounts, auth, profiles
2. Ride Service — ride requests, matching, status
3. Driver Service — driver profiles, availability, location
4. Payment Service — fares, charges, receipts
5. Notification Service — emails, push notifications

Communication:
- User requests ride → Ride Service: SYNC (immediate availability check)
- Driver accepts ride → update Ride status: SYNC
- Ride completed → send receipt: ASYNC (event to Payment Service and Notification Service)

Consistency challenge: When a ride completes, payment must be charged AND the driver's earnings updated AND a receipt sent. If payment fails but we already marked the ride complete, we have inconsistent state.
Solution: Saga pattern — each step is a separate event. Compensating transactions on failure.
```
