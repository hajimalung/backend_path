# Week 6 — Day 3: Spring Kafka — Producing and Consuming Events

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Setup (5 min)

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**Start Kafka locally with docker-compose:**
```yaml
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
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    depends_on:
      - zookeeper
```

---

### 2. Configuration (10 min)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        spring.json.add.type.headers: false    # don't add Java type info to headers

    consumer:
      group-id: task-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest              # read from beginning if no committed offset
      enable-auto-commit: false                # manual commit — safer
      properties:
        spring.json.trusted.packages: "com.example.*"
```

---

### 3. Producing Events (20 min)

**Event class (record):**
```java
public record TaskEvent(
    String eventType,   // CREATED, UPDATED, COMPLETED
    Long taskId,
    String title,
    String ownerUsername,
    String timestamp
) {}
```

**Topic configuration:**
```java
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic taskEventsTopic() {
        return TopicBuilder.name("task-events")
            .partitions(3)
            .replicas(1)          // 1 for local dev; use 3 in production
            .build();
    }
}
```

**Producer service:**
```java
@Service
public class TaskEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventProducer.class);
    private static final String TOPIC = "task-events";

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;

    public TaskEventProducer(KafkaTemplate<String, TaskEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTaskCreated(Task task) {
        TaskEvent event = new TaskEvent(
            "CREATED",
            task.getId(),
            task.getTitle(),
            task.getOwnerUsername(),
            Instant.now().toString()
        );

        // Key = taskId → ensures all events for same task go to same partition
        kafkaTemplate.send(TOPIC, task.getId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send event: {}", ex.getMessage());
                } else {
                    log.info("Sent TaskEvent to partition {} offset {}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
```

**Publish event from TaskService:**
```java
@Transactional
public TaskResponse create(CreateTaskRequest request, String ownerUsername) {
    Task saved = taskRepository.save(new Task(request.title(), request.description(),
        request.priority(), ownerUsername));
    taskEventProducer.sendTaskCreated(saved);   // publish after successful save
    return TaskResponse.from(saved);
}
```

---

### 4. Consuming Events (20 min)

**Consumer in notification-service:**
```java
@Service
public class TaskEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventConsumer.class);

    @KafkaListener(
        topics = "task-events",
        groupId = "notification-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTaskEvent(
            @Payload TaskEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Received {} event for task {} from partition {} offset {}",
            event.eventType(), event.taskId(), partition, offset);

        try {
            switch (event.eventType()) {
                case "CREATED"   -> sendCreationEmail(event);
                case "COMPLETED" -> sendCompletionEmail(event);
                default          -> log.warn("Unknown event type: {}", event.eventType());
            }
            ack.acknowledge();    // commit offset after successful processing
        } catch (Exception e) {
            log.error("Failed to process event {}: {}", event, e.getMessage());
            // Don't ack — will be retried or sent to DLQ
        }
    }

    private void sendCreationEmail(TaskEvent event) {
        log.info("Sending creation email to {} for task '{}'",
            event.ownerUsername(), event.title());
        // real email logic here
    }

    private void sendCompletionEmail(TaskEvent event) {
        log.info("Sending completion email to {} for task '{}'",
            event.ownerUsername(), event.title());
    }
}
```

**Manual ACK configuration:**
```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TaskEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, TaskEvent> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, TaskEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
```

---

### 5. Testing Kafka (5 min)

Spring provides an embedded Kafka broker for tests:

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "task-events")
class TaskEventProducerTest {

    @Autowired TaskEventProducer producer;

    @Autowired
    @Qualifier("embeddedKafka")
    EmbeddedKafkaBroker embeddedKafka;

    @Test
    void sendTaskCreated_publishesToKafka() throws Exception {
        Task task = new Task("Test Task", null, Priority.HIGH, "alice");
        task.setId(1L);

        producer.sendTaskCreated(task);
        // assert using a test consumer or simply verify no exception is thrown
        // (full consumer test needs @KafkaListener setup in test)
        Thread.sleep(500);  // wait for async send (in production tests use CountDownLatch)
    }
}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is `KafkaTemplate` used for?

**Q2.** What does `auto-offset-reset: earliest` do?

**Q3.** Why do we use `enable-auto-commit: false` and manual acknowledgment?

**Q4.** What is the significance of the event key in `kafkaTemplate.send(TOPIC, key, event)`?

**Q5.** What does `@EmbeddedKafka` provide in tests?

---

### Part B — Hands-on (20 min)

Add Kafka event publishing to your `task-service`:

1. Start Kafka via docker-compose.
2. Configure `spring.kafka.producer` in `application.yml`.
3. Create a `TaskEvent` record.
4. Create a `TaskEventProducer` with `KafkaTemplate<String, TaskEvent>`.
5. Create a `NewTopic` bean named `"task-events"` with 3 partitions.
6. In `TaskService.create()`, call `producer.sendTaskCreated(task)` after saving.
7. Verify: create a task → check logs for "Sent TaskEvent to partition X offset Y".
8. Use `docker exec -it kafka-container kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic task-events --from-beginning` to see events in the console.

---

### Answers

**A1.** `KafkaTemplate<K, V>` is Spring's high-level Kafka producer. It serializes your objects, handles connection management, and sends messages to topics. The generic types `K` (key) and `V` (value) define the types for the Kafka record key and value. It returns a `CompletableFuture<SendResult>` for async completion handling.

**A2.** When a new consumer group starts (no committed offset exists), `earliest` means start reading from the **very beginning** of the topic — read all historical events. The alternative `latest` means start reading only new events produced after the consumer starts. Use `earliest` to process historical data; use `latest` for live processing only.

**A3.** With auto-commit, Kafka commits the offset on a schedule (every 5s by default). This means: if your service crashes after auto-commit but before processing completes, the event is lost (offset advanced but not processed). With manual commit (`ack.acknowledge()`), you commit only after **successful** processing. If the service crashes before `ack.acknowledge()`, Kafka re-delivers the event after restart. This provides at-least-once delivery semantics.

**A4.** The key determines which **partition** the event is sent to (via consistent hashing). All events with the same key always land in the same partition, which guarantees **ordering** for that key. If we key by `taskId.toString()`, all events for Task #5 (created, updated, completed) will be in the same partition and consumed in order by any consumer group.

**A5.** `@EmbeddedKafka` starts an **in-process Kafka broker** for integration tests — no external Docker container needed. It creates topics, accepts real producer/consumer connections, and is destroyed after the test class. This makes Kafka integration tests fast and self-contained, runnable in CI without additional infrastructure.
