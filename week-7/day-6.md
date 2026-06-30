# Week 7 — Day 6: Redis Caching with Spring

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Why Caching? (5 min)

Some data is expensive to compute or fetch but doesn't change often:
- Database queries with many joins
- External API calls
- Complex calculations

Instead of repeating the expensive operation, **store the result** in a fast in-memory store and return it on subsequent requests.

```
Without cache:         With cache:
Request → DB (50ms)    Request → Cache HIT (0.1ms) ← served from Redis
Request → DB (50ms)    Request → Cache HIT (0.1ms)
Request → DB (50ms)    Cache MISS → DB (50ms) → stored in cache
```

**Redis**: in-memory data store. Operates on RAM — microsecond latency. Supports: strings, hashes, lists, sets, sorted sets, TTL (expiry), pub/sub.

---

### 2. Setup (5 min)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms

  cache:
    type: redis
    redis:
      time-to-live: 600000     # 10 minutes default TTL (milliseconds)
      cache-null-values: false  # don't cache null results
```

```java
@SpringBootApplication
@EnableCaching                  // activates caching support
public class TaskServiceApplication { ... }
```

**Start Redis:**
```yaml
# docker-compose.yml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
```

---

### 3. Spring Cache Annotations (20 min)

**`@Cacheable` — cache the result:**
```java
@Service
public class TaskQueryService {

    @Cacheable(
        value = "tasks",                          // cache name
        key = "#id",                              // cache key (SpEL)
        condition = "#id != null",                // only cache if condition is true
        unless = "#result == null"                // don't cache null results
    )
    public TaskDetailDto findById(Long id) {
        log.info("Loading task {} from database", id);  // only logged on cache miss
        return taskRepository.findById(id)
            .map(TaskDetailDto::from)
            .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Cacheable(
        value = "user-tasks",
        key = "#username + '-' + #pageable.pageNumber"  // composite key
    )
    public Page<TaskListItemDto> findByOwner(String username, Pageable pageable) {
        return taskRepository.findByOwnerUsername(username, pageable)
            .map(TaskListItemDto::from);
    }
}
```

**`@CacheEvict` — remove from cache when data changes:**
```java
@Service
public class TaskCommandService {

    @CacheEvict(value = "tasks", key = "#command.taskId")
    @Transactional
    public void complete(CompleteTaskCommand command) {
        Task task = taskRepository.findById(command.taskId()).orElseThrow();
        task.complete();
        taskRepository.save(task);
        // Cache for this task is evicted — next read will hit DB
    }

    // Evict all entries in a cache
    @CacheEvict(value = "user-tasks", allEntries = true)
    @Transactional
    public Long create(CreateTaskCommand command) {
        Task saved = taskRepository.save(new Task(...));
        return saved.getId();
    }
}
```

**`@CachePut` — update cache after write (without evicting):**
```java
@CachePut(value = "tasks", key = "#result.id")  // result = return value
@Transactional
public TaskDetailDto updateTitle(Long id, String newTitle) {
    Task task = taskRepository.findById(id).orElseThrow();
    task.setTitle(newTitle);
    Task saved = taskRepository.save(task);
    return TaskDetailDto.from(saved);
    // Cache is updated with new value — no need to evict
}
```

---

### 4. Redis Cache Configuration (15 min)

**Custom TTL per cache:**
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> configs = Map.of(
            "tasks",      defaultConfig.entryTtl(Duration.ofMinutes(30)),  // long TTL
            "user-tasks", defaultConfig.entryTtl(Duration.ofMinutes(5)),   // shorter TTL
            "stats",      defaultConfig.entryTtl(Duration.ofHours(1))      // hourly stats
        );

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(configs)
            .build();
    }
}
```

---

### 5. Cache-Aside Pattern (10 min)

The most common caching pattern — application manages the cache:

```
Read:
1. Check cache
2. Cache HIT → return cached value
3. Cache MISS → read from DB → store in cache → return value

Write:
1. Write to DB
2. Invalidate cache entry (evict)
```

This is what `@Cacheable` + `@CacheEvict` implements automatically.

**Cache pitfalls to avoid:**
- **Cache stampede**: many requests hit an expired key simultaneously, all go to DB. Fix: use locking or staggered TTLs.
- **Stale data**: cache has old data after update. Fix: `@CacheEvict` on writes.
- **Cache poisoning**: caching null/error results. Fix: `unless = "#result == null"`.
- **Over-caching**: caching data that changes every second. The cache adds complexity — only use it for data that's read much more often than it changes.

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between `@Cacheable` and `@CachePut`?

**Q2.** What does TTL stand for and why is it important for cache correctness?

**Q3.** What is the Cache-Aside pattern?

**Q4.** Why should you NOT cache data that changes frequently?

**Q5.** What does `allEntries = true` do in `@CacheEvict`?

---

### Part B — Hands-on (20 min)

Add Redis caching to your `task-service`:

1. Add Redis + Cache starters to `pom.xml`.
2. Configure `spring.redis.host/port` and `spring.cache.type: redis`.
3. Add `@EnableCaching` to your application class.
4. Add `@Cacheable("tasks")` to `TaskQueryService.findById(Long id)`.
5. Add `@CacheEvict(value = "tasks", key = "#id")` to the delete/complete methods.
6. Test:
   - `GET /api/tasks/1` → check logs: "Loading task 1 from database" appears
   - `GET /api/tasks/1` again → log does NOT appear (served from cache)
   - Complete the task → cache evicted
   - `GET /api/tasks/1` again → log appears again (cache miss after evict)
7. Connect to Redis with `redis-cli` and run `KEYS *` to see cached keys.

---

### Answers

**A1.** `@Cacheable`: **reads** from cache first; only executes the method on a cache miss (then stores result). Skips execution on cache hit. `@CachePut`: **always** executes the method, then **updates** the cache with the result. Never reads from cache — always runs and always writes. Use `@Cacheable` for read operations; use `@CachePut` for write operations that should update the cache without evicting it.

**A2.** TTL (Time-To-Live) is the duration after which a cache entry expires and is automatically removed. It's crucial for correctness: without TTL, stale data lives in cache forever. With TTL, even if you forget to evict on every write, the cache will eventually self-correct. Choose TTL based on how often data changes and how stale data would be acceptable to users.

**A3.** Cache-Aside (also called lazy loading): the application manages cache population. On read: check cache first → hit returns cached value, miss fetches from DB and stores in cache. On write: update DB → evict or update cache. The cache is only populated when data is actually requested (lazy). Most common pattern because it's simple and handles cache failures gracefully (fall back to DB).

**A4.** Caching data that changes frequently: (1) increases complexity — you need to evict on every write, (2) serves stale data between writes (even briefly), (3) wastes Redis memory on data that'll be evicted almost immediately, (4) adds overhead for writes (evict + possibly re-cache). Cache is most valuable for data with high read:write ratio (read 100x, write 1x). Frequently changing data has low ratio — caching doesn't help much.

**A5.** `allEntries = true` clears **all entries** in the specified cache, not just one key. Useful when a write operation could affect multiple cached values — for example, creating a new task could affect any user's task list cache. Rather than calculating all affected keys, you evict everything and let the cache repopulate on next read. Trade-off: more cache misses after the operation vs. correctness of all cached values.
