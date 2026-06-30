# Week 2 — Day 4: Concurrency Basics

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Thread Basics (15 min)

A thread is a unit of execution. Java can run multiple threads simultaneously.

```java
// Way 1: Extend Thread
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread: " + Thread.currentThread().getName());
    }
}
new MyThread().start(); // start(), NOT run() — run() would execute on current thread

// Way 2: Implement Runnable (preferred — no inheritance lock-in)
Runnable task = () -> System.out.println("Runnable: " + Thread.currentThread().getName());
Thread t = new Thread(task);
t.start();

// Thread info
System.out.println(Thread.currentThread().getName()); // main
System.out.println(Thread.currentThread().getId());
Thread.sleep(1000); // pause current thread for 1 second (throws InterruptedException)
```

**The problem: race conditions**
```java
// UNSAFE — multiple threads modify shared state
int counter = 0;
Runnable increment = () -> {
    for (int i = 0; i < 1000; i++) counter++; // NOT atomic!
};
// Running 10 threads like this may give different results each run
```

---

### 2. ExecutorService — Thread Pools (20 min)

Creating raw threads is expensive. `ExecutorService` manages a pool of reusable threads.

```java
import java.util.concurrent.*;

// Fixed thread pool — reuses N threads
ExecutorService executor = Executors.newFixedThreadPool(4);

// Submit tasks
executor.submit(() -> System.out.println("Task 1 on " + Thread.currentThread().getName()));
executor.submit(() -> System.out.println("Task 2 on " + Thread.currentThread().getName()));

// Always shut down the executor when done
executor.shutdown();                         // wait for running tasks, then shutdown
executor.awaitTermination(10, TimeUnit.SECONDS); // wait up to 10s

// Get results with Future
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(500);  // simulate work
    return 42;
});

System.out.println("Doing other work...");
Integer result = future.get(); // blocks until result is ready
System.out.println("Result: " + result); // 42

// Other executor types
Executors.newSingleThreadExecutor()       // 1 thread, tasks run sequentially
Executors.newCachedThreadPool()            // grows/shrinks as needed
Executors.newScheduledThreadPool(2)        // for recurring/delayed tasks
```

---

### 3. CompletableFuture — Async Programming (20 min)

`CompletableFuture` is the modern way to write non-blocking, composable async code.

```java
import java.util.concurrent.CompletableFuture;

// Run async (no return value)
CompletableFuture.runAsync(() -> {
    System.out.println("Running in background: " + Thread.currentThread().getName());
});

// Supply async (returns value)
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    // Simulates a slow DB/HTTP call
    return "Hello from async";
});

// thenApply — transform result (like stream's map)
CompletableFuture<Integer> lengthFuture = cf.thenApply(String::length);

// thenAccept — consume result (like Consumer, no return)
cf.thenAccept(s -> System.out.println("Got: " + s));

// thenRun — run something after, ignoring result
cf.thenRun(() -> System.out.println("Done!"));

// Chaining
CompletableFuture<String> pipeline = CompletableFuture
    .supplyAsync(() -> fetchUserFromDb(1))           // async fetch
    .thenApply(user -> user.getName())               // transform
    .thenApply(String::toUpperCase)                  // transform
    .exceptionally(ex -> "Error: " + ex.getMessage()); // error handling

String result = pipeline.get(); // wait for completion
```

**Combining multiple futures:**
```java
// thenCombine — run two in parallel, combine results
CompletableFuture<String> userFuture  = CompletableFuture.supplyAsync(() -> "Alice");
CompletableFuture<Integer> ageFuture  = CompletableFuture.supplyAsync(() -> 30);

CompletableFuture<String> combined = userFuture.thenCombine(
    ageFuture,
    (name, age) -> name + " is " + age
);
System.out.println(combined.get()); // Alice is 30

// allOf — wait for all to complete
CompletableFuture<Void> all = CompletableFuture.allOf(userFuture, ageFuture);
all.get(); // waits until both are done

// anyOf — proceed when the first one completes
CompletableFuture<Object> any = CompletableFuture.anyOf(userFuture, ageFuture);
```

---

### 4. Thread Safety Basics (5 min)

```java
import java.util.concurrent.atomic.AtomicInteger;

// AtomicInteger — thread-safe counter
AtomicInteger counter = new AtomicInteger(0);
Runnable increment = () -> {
    for (int i = 0; i < 1000; i++) counter.incrementAndGet(); // atomic!
};

// synchronized keyword — lock on object
class Counter {
    private int count = 0;

    public synchronized void increment() { count++; } // only one thread at a time
    public synchronized int getCount()   { return count; }
}

// Thread-safe collections
List<String> safeList = Collections.synchronizedList(new ArrayList<>());
Map<String, Integer> safeMap = new ConcurrentHashMap<>(); // better for concurrent access
```

**In Spring Boot**: you generally don't create threads manually — Spring manages concurrency via request threads. But understanding this is essential for background jobs, event processing, and `@Async`.

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between calling `thread.run()` and `thread.start()`?

**Q2.** Why is using a thread pool better than creating a new `Thread` for each task?

**Q3.** What does `future.get()` do? What are the risks of calling it?

**Q4.** What is a race condition? How does `AtomicInteger` help?

**Q5.** What is the difference between `CompletableFuture.runAsync()` and `CompletableFuture.supplyAsync()`?

---

### Part B — Coding Challenge (20 min)

1. Use `ExecutorService` with a fixed pool of 3 threads to process a list of 9 tasks. Each task should print `"Task N processed by thread: [thread-name]"`. Shut down properly after.

2. Write a method `CompletableFuture<String> fetchUser(int id)` that simulates a 200ms delay and returns `"User-" + id`. Then use `thenCombine` to fetch two users in parallel and combine them as `"user1 and user2"`.

3. Write a thread-safe counter class using `AtomicInteger`. Run 5 threads each incrementing it 100 times, then print the final count (should always be 500).

---

### Answers

**A1.** `thread.run()` executes the `run()` method **on the calling thread** — no new thread is created. `thread.start()` creates a **new OS thread** and then calls `run()` on it. Always use `start()` for concurrent execution.

**A2.** Thread creation is expensive (allocates ~1MB stack, makes OS syscall). A thread pool creates threads once and reuses them for many tasks. It also limits max concurrency, preventing resource exhaustion, and handles thread lifecycle management.

**A3.** `future.get()` **blocks** the calling thread until the result is ready. Risks: (1) blocking indefinitely if the task hangs — use `get(timeout, unit)` to limit wait time; (2) it throws `ExecutionException` if the task threw an exception, wrapping the cause.

**A4.** A race condition occurs when multiple threads read and modify shared state simultaneously, producing unpredictable results. `counter++` is not atomic — it's read-modify-write, so threads can interleave. `AtomicInteger.incrementAndGet()` performs this as a single indivisible (atomic) operation using CPU-level compare-and-swap.

**A5.** `runAsync()` takes a `Runnable` — runs asynchronously, returns `CompletableFuture<Void>` (no result). `supplyAsync()` takes a `Supplier<T>` — runs asynchronously and returns a `CompletableFuture<T>` with the computed value.

**Part B Solution:**
```java
// 1. Thread pool with 9 tasks
ExecutorService executor = Executors.newFixedThreadPool(3);
for (int i = 1; i <= 9; i++) {
    final int taskNum = i;
    executor.submit(() ->
        System.out.println("Task " + taskNum + " processed by: " + Thread.currentThread().getName())
    );
}
executor.shutdown();
executor.awaitTermination(5, TimeUnit.SECONDS);

// 2. CompletableFuture combine
CompletableFuture<String> fetchUser(int id) {
    return CompletableFuture.supplyAsync(() -> {
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return "User-" + id;
    });
}

CompletableFuture<String> result = fetchUser(1).thenCombine(
    fetchUser(2),
    (u1, u2) -> u1 + " and " + u2
);
System.out.println(result.get()); // User-1 and User-2

// 3. Thread-safe counter
AtomicInteger counter = new AtomicInteger(0);
ExecutorService exec = Executors.newFixedThreadPool(5);
for (int i = 0; i < 5; i++) {
    exec.submit(() -> {
        for (int j = 0; j < 100; j++) counter.incrementAndGet();
    });
}
exec.shutdown();
exec.awaitTermination(5, TimeUnit.SECONDS);
System.out.println("Final count: " + counter.get()); // always 500
```
