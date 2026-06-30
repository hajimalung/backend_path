# Week 5 — Day 3: Service Discovery with Spring Cloud Eureka

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. The Problem: Service Discovery (10 min)

In a microservices system, services need to talk to each other. But how does Service A know where Service B is running?

**Hardcoding doesn't work:**
```yaml
# BAD — hardcoded URL
task-service:
  user-service-url: http://192.168.1.101:8081  # breaks if IP changes
```

**The solution**: A **service registry** — a central directory where:
1. Each service **registers** itself on startup (name + host + port)
2. Other services **query** the registry to find the current address
3. Services **deregister** on shutdown (or the registry detects failure via heartbeat)

```
user-service ──register──> [Eureka Server]
task-service ──register──> [Eureka Server]

task-service ──"where is user-service?"──> [Eureka Server]
             <──"http://user-service:8081"──
task-service ──────────────────────────────────> user-service
```

---

### 2. Eureka Server Setup (15 min)

**New Spring Boot project — Eureka Server:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

```xml
<!-- Spring Cloud BOM in <dependencyManagement> -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```yaml
# application.yml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false     # server doesn't register with itself
    fetch-registry: false           # server doesn't fetch from itself
  server:
    enable-self-preservation: false # useful in dev — don't keep dead instances
```

Open `http://localhost:8761` — Eureka dashboard shows registered services.

---

### 3. Registering a Service (Eureka Client) (20 min)

Add to your existing services:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableDiscoveryClient              // marks this as a Eureka client
public class TaskServiceApplication { ... }
```

```yaml
# task-service/application.yml
spring:
  application:
    name: task-service              # THIS is the name registered in Eureka

server:
  port: 8082

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true         # register with IP, not hostname
    instance-id: ${spring.application.name}:${server.port}
```

**On startup**, the service registers:
```
TASK-SERVICE  → http://192.168.1.100:8082
```

**Health check**: Eureka expects a heartbeat every 30s (default). If missed 3 times, the instance is evicted.

---

### 4. Discovering Services (RestTemplate + Load Balancing) (15 min)

Once registered, use `@LoadBalanced RestTemplate` to call by **service name**:

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced                   // enables client-side load balancing
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

```java
@Service
public class TaskService {
    private final RestTemplate restTemplate;

    public TaskService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserDto getTaskOwner(Long userId) {
        // "user-service" is resolved via Eureka — NO hardcoded URL!
        return restTemplate.getForObject(
            "http://user-service/api/users/{id}",
            UserDto.class,
            userId
        );
    }
}
```

With `@LoadBalanced`, Spring intercepts the call, resolves `user-service` to real IPs via Eureka, and distributes load if multiple instances are running.

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What problem does service discovery solve? Why can't we just hardcode service URLs?

**Q2.** What does `@EnableEurekaServer` do? What does `@EnableDiscoveryClient` do?

**Q3.** What is `spring.application.name` used for in a Eureka client?

**Q4.** What happens if a service doesn't send heartbeats to Eureka?

**Q5.** What does `@LoadBalanced` on a `RestTemplate` enable?

---

### Part B — Hands-on (20 min)

1. Create a new Spring Boot project `eureka-server` with `spring-cloud-starter-netflix-eureka-server`.
2. Enable it with `@EnableEurekaServer` and configure `application.yml` (port 8761, no self-registration).
3. Add `spring-cloud-starter-netflix-eureka-client` to your `task-service`.
4. Configure `spring.application.name=task-service` and `eureka.client.service-url`.
5. Start both, open `http://localhost:8761`, and verify `TASK-SERVICE` appears in the dashboard.

---

### Answers

**A1.** In dynamic environments (cloud, containers), service IPs and ports change constantly — containers restart, scale up/down. Hardcoding breaks whenever the target service moves. Service discovery externalizes this lookup — each service registers its current location, and callers look it up dynamically at call time.

**A2.** `@EnableEurekaServer` turns the Spring Boot app into a Eureka server — it starts the registry, serves the Eureka REST API, and renders the dashboard. `@EnableDiscoveryClient` registers the app as a client with the configured Eureka server and enables service lookup.

**A3.** `spring.application.name` is the **logical name** the service is registered under in Eureka. Other services use this name to discover it (e.g., `http://task-service/api/tasks`). All instances of the same service register under the same name, enabling load balancing.

**A4.** Eureka tracks heartbeats (default: every 30s). If a service misses heartbeats for longer than the eviction threshold (default: 90s), Eureka removes it from the registry. Clients calling that service will no longer receive its address. Self-preservation mode (can be disabled in dev) keeps instances around even with missed heartbeats in case of network partitions.

**A5.** `@LoadBalanced` intercepts `RestTemplate` calls and replaces service names (e.g., `user-service`) with actual IP:port combinations fetched from Eureka. If multiple instances are registered, it distributes requests using a load balancing algorithm (round-robin by default via Spring Cloud LoadBalancer).
