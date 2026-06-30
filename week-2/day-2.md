# Week 2 — Day 2: Streams API

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. What is a Stream? (10 min)

A `Stream` is a sequence of elements that you can process with a **pipeline of operations**. Streams don't store data — they process it lazily.

```
Source ──> Intermediate ops (lazy) ──> Terminal op (triggers execution)

List.stream() ──> filter() ──> map() ──> collect()
```

Key properties:
- **Lazy**: intermediate operations run only when a terminal op is called
- **Single-use**: a stream can only be consumed once
- **Non-mutating**: original source is unchanged

```java
import java.util.*;
import java.util.stream.*;

List<String> names = List.of("Alice", "Bob", "Charlie", "Anna", "Dave");

// Without streams
List<String> result = new ArrayList<>();
for (String name : names) {
    if (name.startsWith("A")) {
        result.add(name.toUpperCase());
    }
}

// With streams — same logic, more readable
List<String> result = names.stream()
    .filter(name -> name.startsWith("A"))
    .map(String::toUpperCase)
    .collect(Collectors.toList());

System.out.println(result); // [ALICE, ANNA]
```

---

### 2. Creating Streams (5 min)

```java
// From a collection
List.of(1, 2, 3).stream()

// From an array
Arrays.stream(new int[]{1, 2, 3})

// From values directly
Stream.of("a", "b", "c")

// Infinite stream (use with limit!)
Stream.iterate(0, n -> n + 2).limit(5)  // 0, 2, 4, 6, 8
Stream.generate(Math::random).limit(3)  // 3 random doubles

// Range (IntStream)
IntStream.range(1, 5)        // 1, 2, 3, 4 (exclusive end)
IntStream.rangeClosed(1, 5)  // 1, 2, 3, 4, 5 (inclusive end)
```

---

### 3. Intermediate Operations (Lazy) (20 min)

Each returns a new Stream — chain them together.

```java
List<Integer> numbers = List.of(5, 3, 8, 1, 9, 2, 7, 4, 6);

// filter — keep elements matching predicate
numbers.stream()
    .filter(n -> n > 4)
    .forEach(System.out::println); // 5, 8, 9, 7, 6

// map — transform each element
numbers.stream()
    .map(n -> n * n)         // square each number
    .collect(Collectors.toList()); // [25, 9, 64, 1, 81, 4, 49, 16, 36]

// sorted — natural order by default
numbers.stream()
    .sorted()                // [1, 2, 3, 4, 5, 6, 7, 8, 9]
    .collect(Collectors.toList());

numbers.stream()
    .sorted(Comparator.reverseOrder()) // [9, 8, 7, 6, 5, 4, 3, 2, 1]

// distinct — remove duplicates
List.of(1, 2, 2, 3, 3, 3).stream()
    .distinct()              // [1, 2, 3]

// limit / skip
numbers.stream()
    .limit(3)                // first 3 elements: [5, 3, 8]

numbers.stream()
    .skip(6)                 // skip first 6: [7, 4, 6]

// flatMap — flatten nested collections
List<List<Integer>> nested = List.of(List.of(1,2), List.of(3,4), List.of(5));
nested.stream()
    .flatMap(Collection::stream) // [1, 2, 3, 4, 5]

// peek — for debugging (does not change elements)
numbers.stream()
    .filter(n -> n > 5)
    .peek(n -> System.out.println("After filter: " + n))
    .map(n -> n * 2)
    .collect(Collectors.toList());
```

---

### 4. Terminal Operations (Trigger Execution) (15 min)

```java
List<String> words = List.of("apple", "banana", "cherry", "avocado", "blueberry");

// collect — gather results
List<String> list = words.stream().collect(Collectors.toList());
Set<String>  set  = words.stream().collect(Collectors.toSet());
String joined = words.stream().collect(Collectors.joining(", ", "[", "]"));
// "[apple, banana, cherry, avocado, blueberry]"

// count
long count = words.stream().filter(w -> w.startsWith("a")).count(); // 2

// findFirst / findAny
Optional<String> first = words.stream().filter(w -> w.length() > 6).findFirst();
first.ifPresent(System.out::println); // banana

// anyMatch / allMatch / noneMatch
boolean any  = words.stream().anyMatch(w -> w.startsWith("z"));  // false
boolean all  = words.stream().allMatch(w -> w.length() > 3);      // true
boolean none = words.stream().noneMatch(w -> w.isEmpty());        // true

// reduce — fold into single value
int sum = List.of(1,2,3,4,5).stream()
    .reduce(0, Integer::sum); // 15

// min / max
Optional<String> shortest = words.stream()
    .min(Comparator.comparingInt(String::length));
System.out.println(shortest.get()); // apple

// forEach
words.stream().forEach(System.out::println);

// toArray
String[] arr = words.stream().toArray(String[]::new);
```

---

### 5. Collectors (10 min)

```java
List<String> words = List.of("apple", "banana", "cherry", "avocado", "blueberry");

// groupingBy — group elements by a classifier
Map<Integer, List<String>> byLength = words.stream()
    .collect(Collectors.groupingBy(String::length));
// {5=[apple], 6=[banana, cherry], 7=[avocado], 9=[blueberry]}

// counting per group
Map<Integer, Long> countByLength = words.stream()
    .collect(Collectors.groupingBy(String::length, Collectors.counting()));

// partitioningBy — split into two groups (true/false)
Map<Boolean, List<String>> partition = words.stream()
    .collect(Collectors.partitioningBy(w -> w.startsWith("a")));
// {true=[apple, avocado], false=[banana, cherry, blueberry]}

// toMap
Map<String, Integer> wordLengths = words.stream()
    .collect(Collectors.toMap(w -> w, String::length));
// {apple=5, banana=6, ...}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between an intermediate and a terminal stream operation? Give one example of each.

**Q2.** What does `flatMap` do that `map` cannot? Give a use case.

**Q3.** What will this print?
```java
long count = Stream.of(1, 2, 3, 4, 5)
    .filter(n -> n % 2 == 0)
    .map(n -> n * 10)
    .count();
System.out.println(count);
```

**Q4.** What does `Collectors.groupingBy` return?

**Q5.** Can you reuse a stream after calling a terminal operation? Why or why not?

---

### Part B — Coding Challenge (20 min)

Given a list of `Employee` records (use yesterday's `record Employee(String name, int id, String department, double salary)`):

1. Find all employees in "Engineering" department
2. Get their names as an uppercase list, sorted alphabetically
3. Calculate the average salary of all employees
4. Find the highest-paid employee's name
5. Group all employees by department (`Map<String, List<Employee>>`)

Use a single stream pipeline for each operation. Create at least 6 employees in `main` to test.

---

### Answers

**A1.** **Intermediate operations** return a new Stream and are lazy (don't execute until a terminal op triggers them). Example: `filter()`, `map()`, `sorted()`. **Terminal operations** trigger execution and return a non-Stream result. Example: `collect()`, `count()`, `forEach()`, `findFirst()`.

**A2.** `map` transforms each element to exactly one output — the result is `Stream<Stream<T>>` if the mapper returns a collection. `flatMap` transforms each element to zero or more elements and **flattens** the nested structure into a single stream. Use case: converting `List<List<String>>` to `List<String>`, or splitting sentences into words.

**A3.** `2` — only `2` and `4` pass `filter(n % 2 == 0)`. Then `map` transforms them (doesn't change count). `count()` counts resulting elements.

**A4.** `Map<K, List<T>>` — a map from the classifier value (key) to a list of elements matching that key. The value type can be customized with a downstream collector.

**A5.** No. A stream is **single-use** — once a terminal operation is called, the stream is closed. Attempting to reuse it throws `IllegalStateException`. Create a new stream from the source each time.

**Part B Solution:**
```java
record Employee(String name, int id, String department, double salary) {}

List<Employee> employees = List.of(
    new Employee("Alice",   1, "Engineering", 90000),
    new Employee("Bob",     2, "Engineering", 85000),
    new Employee("Charlie", 3, "Marketing",   70000),
    new Employee("Diana",   4, "Engineering", 95000),
    new Employee("Eve",     5, "Marketing",   72000),
    new Employee("Frank",   6, "HR",          65000)
);

// 1. Engineering names, uppercase, sorted
List<String> engNames = employees.stream()
    .filter(e -> e.department().equals("Engineering"))
    .map(e -> e.name().toUpperCase())
    .sorted()
    .collect(Collectors.toList());
System.out.println(engNames); // [ALICE, BOB, DIANA]

// 2. Average salary
OptionalDouble avg = employees.stream()
    .mapToDouble(Employee::salary)
    .average();
System.out.printf("Avg salary: %.2f%n", avg.getAsDouble());

// 3. Highest paid
employees.stream()
    .max(Comparator.comparingDouble(Employee::salary))
    .map(Employee::name)
    .ifPresent(name -> System.out.println("Highest paid: " + name)); // Diana

// 4. Group by department
Map<String, List<Employee>> byDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::department));
byDept.forEach((dept, emps) -> System.out.println(dept + ": " + emps.size()));
```
