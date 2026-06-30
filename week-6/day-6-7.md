# Week 6 — Day 6 & 7: Mini Project 3 Extension — Kafka + Resilience + Tracing

> Build time: 2 days (~1 hour each) | No separate test — this IS the test

---

## Goal

Extend your microservices system with:
1. **Kafka events** from task-service (publish `TaskCreated`, `TaskCompleted`)
2. **notification-service** — new service that consumes task events and logs notifications
3. **Resilience4j** circuit breaker on Feign calls from task-service to user-service
4. **Distributed tracing** visible in Zipkin across all services

---

## Architecture (Complete Week 6 System)

```
Client
  ↓
api-gateway:8080
  │
  ├──→ user-service:8081 (auth, user management)
  │       ↑ Feign + CircuitBreaker
  ├──→ task-service:8082 ──publish──→ [Kafka: task-events]
  │                                          │
  └──→ notification-service:8083 ←──consume──┘

Eureka:8761 ← all services register
Zipkin:9411 ← all services send traces
```

---

## Day 6 — Kafka Integration + Resilience

### Step 1: Add Kafka to task-service

```yaml
# task-service application.yml additions
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
```

```java
// Task event
public record TaskEvent(
    String eventId,           // UUID for idempotency
    String eventType,
    Long taskId,
    String title,
    String ownerUsername,
    String priority,
    String occurredAt
) {}
```

```java
// Topic config
@Bean
public NewTopic taskEventsTopic() {
    return TopicBuilder.name("task-events").partitions(3).replicas(1).build();
}
```

```java
// Publish from TaskService
@Transactional
public TaskResponse create(CreateTaskRequest request, String username) {
    Task saved = taskRepository.save(new Task(request.title(), request.description(),
        request.priority(), username));
    taskEventProducer.send(new TaskEvent(
        UUID.randomUUID().toString(), "TASK_CREATED",
        saved.getId(), saved.getTitle(), username,
        saved.getPriority().name(), Instant.now().toString()
    ));
    return TaskResponse.from(saved);
}

public TaskResponse markComplete(Long id, String username) {
    Task task = taskRepository.findByIdAndOwnerUsername(id, username)
        .orElseThrow(() -> new TaskNotFoundException(id));
    task.setStatus(TaskStatus.DONE);
    Task saved = taskRepository.save(task);
    taskEventProducer.send(new TaskEvent(
        UUID.randomUUID().toString(), "TASK_COMPLETED",
        saved.getId(), saved.getTitle(), username,
        saved.getPriority().name(), Instant.now().toString()
    ));
    return TaskResponse.from(saved);
}
```

### Step 2: Add Circuit Breaker to Feign calls

```yaml
# task-service application.yml additions
resilience4j:
  circuit-breaker:
    instances:
      user-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 15s
        permitted-number-of-calls-in-half-open-state: 3
  retry:
    instances:
      user-service:
        max-attempts: 2
        wait-duration: 200ms
```

```java
@Service
public class UserServiceAdapter {

    private final UserServiceClient userClient;

    @Retry(name = "user-service", fallbackMethod = "getFallback")
    @CircuitBreaker(name = "user-service", fallbackMethod = "getFallback")
    public UserDto getUser(String username) {
        return userClient.getByUsername(username);
    }

    private UserDto getFallback(String username, Throwable ex) {
        log.warn("Fallback for user {}: {}", username, ex.getMessage());
        return new UserDto(null, username);   // graceful degradation
    }
}
```

---

## Day 7 — notification-service + Distributed Tracing

### Step 1: Create notification-service

**New Spring Boot project:**
```xml
<dependencies>
    <dependency>spring-boot-starter-web</dependency>
    <dependency>spring-kafka</dependency>
    <dependency>spring-cloud-starter-netflix-eureka-client</dependency>
    <dependency>micrometer-tracing-bridge-brave</dependency>
    <dependency>zipkin-reporter-brave</dependency>
    <dependency>spring-boot-starter-actuator</dependency>
</dependencies>
```

```yaml
spring:
  application:
    name: notification-service
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-group
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.example.*"
        spring.json.value.default.type: com.example.notification.dto.TaskEvent

server:
  port: 8083

management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

```java
@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet(); // idempotency

    @KafkaListener(topics = "task-events", containerFactory = "kafkaListenerContainerFactory")
    public void handle(TaskEvent event, Acknowledgment ack) {
        // Idempotency check
        if (!processedEventIds.add(event.eventId())) {
            log.info("Duplicate event {}, skipping", event.eventId());
            ack.acknowledge();
            return;
        }

        log.info("Processing {} event for task '{}' owned by {}",
            event.eventType(), event.title(), event.ownerUsername());

        switch (event.eventType()) {
            case "TASK_CREATED" ->
                log.info("[EMAIL] To: {} | Subject: Task '{}' created!", 
                    event.ownerUsername(), event.title());
            case "TASK_COMPLETED" ->
                log.info("[EMAIL] To: {} | Subject: Congratulations! Task '{}' done!",
                    event.ownerUsername(), event.title());
            default ->
                log.warn("Unknown event type: {}", event.eventType());
        }

        ack.acknowledge();
    }
}
```

### Step 2: Add Tracing to All Services

Add to **every** service's pom.xml:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

Add to **every** service's `application.yml`:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]"
```

### Step 3: Full docker-compose.yml

```yaml
version: "3.9"
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    depends_on: [zookeeper]

  zipkin:
    image: openzipkin/zipkin:3
    ports:
      - "9411:9411"

  userdb:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: userdb
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: devpass
    ports:
      - "5433:5432"

  taskdb:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: taskdb
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: devpass
    ports:
      - "5432:5432"

  eureka-server:
    build: ./eureka-server
    ports: ["8761:8761"]

  user-service:
    build: ./user-service
    ports: ["8081:8081"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://userdb:5432/userdb
      SPRING_DATASOURCE_USERNAME: dev
      SPRING_DATASOURCE_PASSWORD: devpass
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka/
      MANAGEMENT_ZIPKIN_TRACING_ENDPOINT: http://zipkin:9411/api/v2/spans

  task-service:
    build: ./task-service
    ports: ["8082:8082"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://taskdb:5432/taskdb
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka/
      MANAGEMENT_ZIPKIN_TRACING_ENDPOINT: http://zipkin:9411/api/v2/spans

  notification-service:
    build: ./notification-service
    ports: ["8083:8083"]
    environment:
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka/
      MANAGEMENT_ZIPKIN_TRACING_ENDPOINT: http://zipkin:9411/api/v2/spans

  api-gateway:
    build: ./api-gateway
    ports: ["8080:8080"]
    environment:
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka/
      MANAGEMENT_ZIPKIN_TRACING_ENDPOINT: http://zipkin:9411/api/v2/spans
```

---

## End-to-End Verification

```bash
# 1. Start everything
docker compose up --build -d

# 2. Create a task (auth first, then create)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123"}' | jq -r .token)

curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Exercise","priority":"HIGH"}'

# 3. Check notification-service logs
docker compose logs notification-service
# Expected: "[EMAIL] To: alice | Subject: Task 'Exercise' created!"

# 4. Check Zipkin
open http://localhost:9411
# Expected: trace showing gateway → task-service → user-service (Feign) spans

# 5. Test circuit breaker
docker compose stop user-service
curl -X POST http://localhost:8080/api/tasks ...
# task-service should use fallback (no 500 error)

# 6. Check Eureka
open http://localhost:8761
# Expected: 4 services registered
```

---

## Completion Checklist

- [ ] `task-service` publishes `TASK_CREATED` event when task is created
- [ ] `task-service` publishes `TASK_COMPLETED` event when task is marked done
- [ ] `notification-service` consumes events and logs email simulations
- [ ] Duplicate events (same `eventId`) are ignored (idempotency)
- [ ] `@CircuitBreaker` protects Feign call to `user-service`
- [ ] When `user-service` is down, task creation still works with fallback
- [ ] All services export traces to Zipkin (`http://localhost:9411`)
- [ ] Trace ID appears in logs of multiple services for the same request
- [ ] `docker compose up` starts the complete system

## What You Practiced

| Concept | Where used |
|---------|-----------|
| Kafka producer | task-service `TaskEventProducer` |
| Kafka consumer | notification-service `NotificationConsumer` |
| Idempotent consumer | `processedEventIds` set check |
| Fat events | `TaskEvent` with all notification data |
| Circuit breaker | `UserServiceAdapter` protecting Feign |
| Graceful degradation | Fallback returns partial `UserDto` |
| Distributed tracing | All services → Zipkin |
| Trace propagation | Feign + Kafka headers auto-injected |
| docker-compose orchestration | Full system startup with one command |
