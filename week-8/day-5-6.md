# Week 8 — Day 5 & 6: Capstone — E-Commerce Backend System Design

> Build time: 2 days (~1 hour each) | This is your final project

---

## Goal

Design and partially implement a simplified e-commerce backend, applying everything you've learned:
- Microservices + DDD (bounded contexts)
- REST API design with security
- Event-driven patterns
- Resilience
- Caching
- Proper error handling

---

## Day 5 — System Design Phase

### Step 1: Requirements Clarification

**Functional requirements (MVP):**
- Users can browse products
- Users can add products to a cart
- Users can place an order
- Orders trigger payment processing
- Order confirmation email is sent
- Inventory is decremented on order

**Non-functional requirements:**
- 1000 concurrent users (scale-up if needed later)
- API response time < 200ms at p99 for product browsing
- Order placement must be consistent — no double charging
- 99.9% availability (max ~9 hours downtime/year)
- Products data can be cached (eventually consistent)
- Order/payment data must be immediately consistent (ACID)

---

### Step 2: Bounded Contexts (DDD)

Identify natural service boundaries:

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Catalog       │  │     Order       │  │    Payment      │
│                 │  │                 │  │                 │
│ Product         │  │ Order           │  │ Transaction     │
│ Category        │  │ OrderLine       │  │ PaymentMethod   │
│ Inventory       │  │ CartItem        │  │ Invoice         │
│                 │  │                 │  │                 │
│ catalog-service │  │  order-service  │  │ payment-service │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │                    │
         └────────────────────┴────────────────────┘
                    Each has its own DB

┌─────────────────┐  ┌─────────────────┐
│      User       │  │  Notification   │
│                 │  │                 │
│ Account         │  │ EmailTemplate   │
│ Address         │  │ NotificationLog │
│ Auth/JWT        │  │                 │
│                 │  │                 │
│  user-service   │  │notification-svc │
└─────────────────┘  └─────────────────┘
```

---

### Step 3: Architecture Diagram

```
External Client (Web/Mobile)
          ↓
   [API Gateway :8080]
     ├── /api/auth/**         → user-service:8081
     ├── /api/users/**        → user-service:8081
     ├── /api/products/**     → catalog-service:8082 (+ Redis cache)
     ├── /api/cart/**         → order-service:8083
     └── /api/orders/**       → order-service:8083
                                      │
                              [Kafka: order-events]
                                      │
              ┌───────────────────────┼───────────────────────┐
              ↓                       ↓                       ↓
    payment-service:8084    catalog-service (inventory)  notification-svc:8085
    (processes payment)     (decrements stock)           (sends email)

[Eureka :8761] ← all services register
[Zipkin  :9411] ← all services send traces
```

---

### Step 4: Data Models

**catalog-service:**
```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    category VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    parent_id BIGINT REFERENCES categories(id)
);
```

**order-service:**
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    owner_username VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE order_lines (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL,          -- reference, not FK (cross-service)
    product_name VARCHAR(255) NOT NULL,  -- denormalized (snapshot at order time)
    unit_price NUMERIC(10,2) NOT NULL,   -- snapshot at order time
    quantity INT NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL
);
```

---

### Step 5: API Design

**catalog-service:**
```
GET  /api/products                    → paginated list (optional filter by category)
GET  /api/products/{id}               → product detail
GET  /api/products/search?q=keyword   → full-text search

POST /api/products               (ADMIN) → create product
PUT  /api/products/{id}/stock    (ADMIN) → update inventory
```

**order-service:**
```
POST /api/cart/items                  → add item to cart (session or user-based)
GET  /api/cart                        → view cart
DELETE /api/cart/items/{productId}    → remove item

POST /api/orders                      → place order (creates Order, triggers payment)
GET  /api/orders                      → order history
GET  /api/orders/{id}                 → order detail
```

---

### Step 6: Order Placement Saga Design

Order placement spans 3 services — design as a choreography saga:

```
1. order-service: creates Order (status=PENDING) → publishes OrderPlaced event
2. payment-service: consumes OrderPlaced → processes payment
   - SUCCESS → publishes PaymentSucceeded
   - FAILURE → publishes PaymentFailed
3. catalog-service: consumes PaymentSucceeded → decrements inventory
   → publishes InventoryReserved
4. order-service: consumes PaymentSucceeded → updates Order to CONFIRMED
   consumes PaymentFailed → updates Order to FAILED (compensating transaction)
5. notification-service: consumes OrderConfirmed → sends confirmation email
```

**Events:**
```java
public record OrderPlaced(String eventId, Long orderId, String ownerUsername,
    BigDecimal totalAmount, List<OrderLineDto> lines, Instant at) {}

public record PaymentSucceeded(String eventId, Long orderId,
    String transactionId, Instant at) {}

public record PaymentFailed(String eventId, Long orderId,
    String reason, Instant at) {}

public record OrderConfirmed(String eventId, Long orderId,
    String ownerUsername, BigDecimal amount, Instant at) {}
```

---

### Step 7: Caching Strategy

| Data | Cache? | TTL | Why |
|------|--------|-----|-----|
| Product list | ✅ | 5 min | Read-heavy, changes rarely |
| Product detail | ✅ | 10 min | Frequently read same products |
| Cart | ❌ | — | User-specific, changes frequently |
| Order history | ✅ | 30 sec | Read between order placements |
| Inventory count | ❌ | — | Must be accurate for order placement |

---

## Day 6 — Partial Implementation

Implement these core flows (pick 2-3 based on your time):

### Option A: Catalog Service — Products CRUD + Cache

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Page<ProductDto> list(@RequestParam(required = false) String category,
            Pageable pageable) {
        return productService.findAll(category, pageable);
    }

    @GetMapping("/{id}")
    public ProductDto getById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@RequestBody @Valid CreateProductRequest request) {
        return productService.create(request);
    }
}

@Service
public class ProductService {

    @Cacheable(value = "products", key = "#id")
    public ProductDto findById(Long id) {
        return productRepository.findById(id)
            .map(ProductDto::from)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductDto create(CreateProductRequest request) {
        Product saved = productRepository.save(new Product(
            request.name(), request.description(),
            request.price(), request.stockQuantity(), request.category()));
        return ProductDto.from(saved);
    }
}
```

### Option B: Order Service — Place Order + Publish Event

```java
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;

    public OrderDto placeOrder(PlaceOrderRequest request, String username) {
        // Validate cart items are non-empty
        if (request.items().isEmpty()) {
            throw new IllegalArgumentException("Cannot place empty order");
        }

        // Calculate total
        BigDecimal total = request.items().stream()
            .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create order
        Order order = new Order(username, OrderStatus.PENDING, total);
        request.items().forEach(item ->
            order.addLine(item.productId(), item.productName(),
                item.unitPrice(), item.quantity()));
        Order saved = orderRepository.save(order);

        // Publish event — payment-service will react
        eventProducer.publish(new OrderPlaced(
            UUID.randomUUID().toString(), saved.getId(), username,
            total, saved.getLines().stream().map(OrderLineDto::from).toList(),
            Instant.now()
        ));

        return OrderDto.from(saved);
    }

    @KafkaListener(topics = "payment-events")
    public void handlePaymentResult(PaymentResultEvent event) {
        Order order = orderRepository.findById(event.orderId()).orElseThrow();
        if (event.success()) {
            order.confirm();
        } else {
            order.fail(event.reason());
        }
        orderRepository.save(order);
    }
}
```

---

## Completion Checklist

### Design Phase (Day 5):
- [ ] 5 bounded contexts identified with clear responsibilities
- [ ] Architecture diagram sketched (on paper or Mermaid)
- [ ] Data models defined for catalog + order services
- [ ] API endpoints listed for catalog + order services
- [ ] Order placement saga events defined (OrderPlaced, PaymentSucceeded, etc.)
- [ ] Caching strategy table completed

### Implementation Phase (Day 6):
- [ ] At least one service implemented with full REST CRUD
- [ ] At least one Kafka producer/consumer pair working
- [ ] Redis caching applied to product reads
- [ ] Circuit breaker on at least one inter-service call
- [ ] All services registered in Eureka
- [ ] Basic tests pass (`mvn test`)

## What You Practiced

| Concept | Where applied |
|---------|--------------|
| Bounded contexts | 5 services with clear domain ownership |
| DDD aggregates | Order + OrderLines, Product + Category |
| Saga choreography | Order placement spanning 3 services |
| Kafka events | OrderPlaced → PaymentSucceeded → OrderConfirmed |
| Redis caching | Product list + detail with Cache-Aside |
| Circuit breaker | Feign calls between services |
| JWT security | All protected endpoints |
| CQRS | Separate read/write paths in order-service |
| Pagination | Product listing |
| ADR | Document your major decisions |
