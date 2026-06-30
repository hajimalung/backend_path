# Week 1 — Day 3: Collections Framework

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Why Collections? (5 min)

Arrays have fixed size and lack utilities. The Collections Framework provides:
- Dynamic-size data structures
- Built-in search, sort, and iteration
- Consistent API across all data structures

The main interfaces: `List`, `Set`, `Map`, `Queue`. Each has multiple implementations with different performance tradeoffs.

---

### 2. List — Ordered, Allows Duplicates (15 min)

```java
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

// ArrayList — best for random access (get by index)
List<String> names = new ArrayList<>();
names.add("Alice");
names.add("Bob");
names.add("Alice");       // duplicates allowed
names.add(0, "Charlie"); // insert at index 0

System.out.println(names);       // [Charlie, Alice, Bob, Alice]
System.out.println(names.get(1)); // Alice
System.out.println(names.size()); // 4

names.remove("Bob");             // remove by value
names.remove(0);                 // remove by index

// Iterate
for (String name : names) {
    System.out.println(name);
}

// Sort (natural order for Strings = alphabetical)
Collections.sort(names);

// List.of() — immutable list (Java 9+)
List<String> fixed = List.of("x", "y", "z");
// fixed.add("w"); // throws UnsupportedOperationException
```

**ArrayList vs LinkedList:**
- `ArrayList`: O(1) get by index, O(n) insert/delete in middle → use for most cases
- `LinkedList`: O(n) get by index, O(1) insert/delete at head/tail → use as a Queue/Deque

---

### 3. Set — No Duplicates (10 min)

```java
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeSet;
import java.util.Set;

// HashSet — fastest, no ordering guaranteed
Set<String> fruits = new HashSet<>();
fruits.add("Apple");
fruits.add("Banana");
fruits.add("Apple");     // duplicate, silently ignored
System.out.println(fruits.size()); // 2

boolean has = fruits.contains("Apple"); // true

// LinkedHashSet — insertion order preserved
Set<String> ordered = new LinkedHashSet<>();
ordered.add("C"); ordered.add("A"); ordered.add("B");
System.out.println(ordered); // [C, A, B]

// TreeSet — sorted order (alphabetical for String)
Set<String> sorted = new TreeSet<>();
sorted.add("C"); sorted.add("A"); sorted.add("B");
System.out.println(sorted); // [A, B, C]
```

**When to use Set:** when you need uniqueness (e.g., tracking visited URLs, unique user IDs).

---

### 4. Map — Key-Value Pairs (15 min)

```java
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Map;

// HashMap — fastest, no ordering
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 95);
scores.put("Bob", 80);
scores.put("Alice", 98);   // replaces previous value for "Alice"

System.out.println(scores.get("Alice"));         // 98
System.out.println(scores.getOrDefault("Eve", 0)); // 0 (key not found)
System.out.println(scores.containsKey("Bob"));  // true
System.out.println(scores.size());              // 2

// Iterating a Map
for (Map.Entry<String, Integer> entry : scores.entrySet()) {
    System.out.println(entry.getKey() + " = " + entry.getValue());
}

// putIfAbsent — only adds if key doesn't exist
scores.putIfAbsent("Charlie", 75);

// Map.of() — immutable (Java 9+)
Map<String, Integer> fixed = Map.of("x", 1, "y", 2);
```

**HashMap vs TreeMap:**
- `HashMap`: O(1) get/put, no order → use for most cases
- `TreeMap`: O(log n) get/put, keys in sorted order → use when sorted keys matter

---

### 5. Queue — FIFO Processing (10 min)

```java
import java.util.Queue;
import java.util.LinkedList;
import java.util.PriorityQueue;

// LinkedList as Queue (FIFO)
Queue<String> queue = new LinkedList<>();
queue.offer("Task1");  // add to tail (prefer offer over add — no exception on fail)
queue.offer("Task2");
queue.offer("Task3");

System.out.println(queue.peek());  // "Task1" — look at head, don't remove
System.out.println(queue.poll());  // "Task1" — remove and return head
System.out.println(queue.size());  // 2

// PriorityQueue — min-heap, lowest value processed first
Queue<Integer> pq = new PriorityQueue<>();
pq.offer(5); pq.offer(1); pq.offer(3);
System.out.println(pq.poll()); // 1 (smallest first)
System.out.println(pq.poll()); // 3
```

---

### 6. Choosing the Right Collection (5 min)

| Need | Use |
|------|-----|
| Ordered list, duplicates OK | `ArrayList` |
| Frequent insert/delete at ends | `LinkedList` |
| Unique items, order doesn't matter | `HashSet` |
| Unique items, insertion order | `LinkedHashSet` |
| Unique items, sorted | `TreeSet` |
| Key-value lookup | `HashMap` |
| Key-value, sorted keys | `TreeMap` |
| FIFO processing | `LinkedList as Queue` |
| Priority-based processing | `PriorityQueue` |

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between `List` and `Set` in Java?

**Q2.** What will this print and why?
```java
Set<String> s = new HashSet<>();
s.add("B"); s.add("A"); s.add("C"); s.add("A");
System.out.println(s.size());
```

**Q3.** What does `map.getOrDefault("key", 0)` do?

**Q4.** What is the difference between `ArrayList` and `LinkedList`? When would you choose `LinkedList`?

**Q5.** What does `queue.poll()` return if the queue is empty?

---

### Part B — Coding Challenge (20 min)

Write a method `wordCount(String text)` that:
1. Splits the input text into words (use `text.split("\\s+")`)
2. Counts how many times each word appears (case-insensitive — lowercase everything)
3. Returns the result as a `Map<String, Integer>`
4. In `main`, print each word and its count, sorted alphabetically by word

Example input: `"Java is great and java is fun"`
Expected output:
```
and: 1
fun: 1
great: 1
is: 2
java: 2
```

---

### Answers

**A1.** `List` maintains insertion order and allows duplicates. `Set` does not allow duplicates; whether it maintains order depends on the implementation (`HashSet` doesn't, `LinkedHashSet` does, `TreeSet` sorts).

**A2.** `3`. `HashSet` ignores the second `"A"` (duplicate). Size is 3 (A, B, C). Note: the order of printing is not guaranteed with `HashSet`.

**A3.** Returns the value for that key if it exists, otherwise returns the default value (0 in this case). It's a safe alternative to `map.get(key)` which returns `null` for missing keys.

**A4.** `ArrayList` uses a dynamic array — O(1) index access, O(n) insert/delete in the middle. `LinkedList` uses doubly-linked nodes — O(n) index access, O(1) insert/delete at the front/back. Choose `LinkedList` when you frequently add/remove from the beginning or need a `Deque`.

**A5.** `null` — `poll()` returns `null` if the queue is empty (unlike `remove()` which throws an exception).

**Part B Solution:**
```java
import java.util.*;

public class WordCounter {
    public static Map<String, Integer> wordCount(String text) {
        Map<String, Integer> counts = new HashMap<>();
        for (String word : text.split("\\s+")) {
            String lower = word.toLowerCase();
            counts.put(lower, counts.getOrDefault(lower, 0) + 1);
        }
        return counts;
    }

    public static void main(String[] args) {
        Map<String, Integer> result = wordCount("Java is great and java is fun");
        new TreeMap<>(result).forEach((k, v) -> System.out.println(k + ": " + v));
    }
}
```
