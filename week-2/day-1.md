# Week 2 — Day 1: Lambda Expressions + Functional Interfaces

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. What is a Lambda? (10 min)

A lambda is an **anonymous function** — a block of code you can pass around as a value.

```java
// Before lambdas (Java 7 and earlier) — anonymous inner class
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println("Running!");
    }
};

// With lambda (Java 8+) — much shorter
Runnable r = () -> System.out.println("Running!");

r.run(); // Running!
```

Lambda syntax: `(parameters) -> expression` or `(parameters) -> { statements; }`

```java
// No parameters
() -> System.out.println("hello")

// One parameter (parens optional)
name -> System.out.println("Hello " + name)
(name) -> System.out.println("Hello " + name)

// Multiple parameters
(a, b) -> a + b

// Multi-line body
(a, b) -> {
    int sum = a + b;
    System.out.println("Sum: " + sum);
    return sum;
}
```

---

### 2. Functional Interfaces (15 min)

A **functional interface** has exactly one abstract method. Lambdas implement them.

`java.util.function` package has the most common ones:

```java
import java.util.function.*;

// Function<T, R> — takes T, returns R
Function<String, Integer> length = s -> s.length();
System.out.println(length.apply("hello")); // 5

Function<Integer, Integer> doubler = n -> n * 2;
Function<Integer, Integer> addTen  = n -> n + 10;
// Compose: apply addTen first, then doubler
Function<Integer, Integer> combined = doubler.compose(addTen);
System.out.println(combined.apply(5)); // (5+10)*2 = 30

// Predicate<T> — takes T, returns boolean
Predicate<String> isLong  = s -> s.length() > 5;
Predicate<String> isUpper = s -> s.equals(s.toUpperCase());
System.out.println(isLong.test("hello"));           // false
System.out.println(isLong.and(isUpper).test("LONGSTR")); // true (both conditions)

// Consumer<T> — takes T, returns nothing (side effect)
Consumer<String> printer  = s -> System.out.println(s);
Consumer<String> logger   = s -> System.out.println("[LOG] " + s);
printer.accept("Hello");                             // Hello
printer.andThen(logger).accept("Hello");             // Hello \n [LOG] Hello

// Supplier<T> — no input, returns T (lazy evaluation)
Supplier<String> greeting = () -> "Hello, World!";
System.out.println(greeting.get()); // Hello, World!

// BiFunction<T, U, R> — takes two inputs, returns R
BiFunction<String, Integer, String> repeat = (s, n) -> s.repeat(n);
System.out.println(repeat.apply("ab", 3)); // ababab

// UnaryOperator<T> — Function where T = R (same in/out type)
UnaryOperator<String> shout = s -> s.toUpperCase() + "!";
System.out.println(shout.apply("hello")); // HELLO!

// BinaryOperator<T> — BiFunction where all types are T
BinaryOperator<Integer> max = (a, b) -> a > b ? a : b;
System.out.println(max.apply(3, 7)); // 7
```

---

### 3. Using Lambdas with Collections (15 min)

```java
import java.util.*;

List<String> names = new ArrayList<>(Arrays.asList("Charlie", "Alice", "Bob", "David"));

// Sort with lambda comparator
names.sort((a, b) -> a.compareTo(b));           // alphabetical
names.sort((a, b) -> b.compareTo(a));           // reverse alphabetical
names.sort((a, b) -> a.length() - b.length()); // by length

// forEach with lambda
names.forEach(name -> System.out.println(name));

// removeIf — remove elements matching predicate
names.removeIf(name -> name.length() < 4); // removes "Bob"

// replaceAll — transform each element in place
names.replaceAll(String::toUpperCase); // method reference (tomorrow)

System.out.println(names); // [ALICE, CHARLIE, DAVID]
```

---

### 4. Custom Functional Interfaces (15 min)

Use `@FunctionalInterface` to define your own:

```java
@FunctionalInterface
public interface Validator<T> {
    boolean validate(T value);

    // Default methods are allowed in functional interfaces
    default Validator<T> and(Validator<T> other) {
        return value -> this.validate(value) && other.validate(value);
    }
}
```

```java
Validator<String> notEmpty = s -> !s.isBlank();
Validator<String> notTooLong = s -> s.length() <= 100;
Validator<String> combined = notEmpty.and(notTooLong);

System.out.println(combined.validate("Hello"));   // true
System.out.println(combined.validate(""));        // false
```

```java
// Another example — TriFunction (not in Java standard library)
@FunctionalInterface
interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}

TriFunction<String, Integer, Boolean, String> format =
    (name, age, active) -> name + " (" + age + ") — " + (active ? "active" : "inactive");

System.out.println(format.apply("Alice", 30, true)); // Alice (30) — active
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is a functional interface? Can it have more than one abstract method?

**Q2.** What is the difference between `Function<T,R>` and `Consumer<T>`?

**Q3.** What does `predicate.and(other)` return?

**Q4.** Without running it, what does this print?
```java
Function<Integer, Integer> f = x -> x * 3;
Function<Integer, Integer> g = x -> x + 2;
System.out.println(f.andThen(g).apply(4));
```

**Q5.** What does `Supplier<T>` represent? Give a real use case.

---

### Part B — Coding Challenge (20 min)

1. Write a method `List<String> filter(List<String> items, Predicate<String> predicate)` that returns a new list containing only items that pass the predicate.

2. Write a method `<T, R> List<R> transform(List<T> items, Function<T, R> mapper)` that returns a new list with each element transformed by the function.

3. In `main`, use these methods to:
   - Filter a list of names: keep only names with more than 4 characters
   - Transform the filtered list: convert each name to uppercase
   - Print the result

4. Write a `Validator<Integer>` that validates that an integer is between 1 and 100 (inclusive). Chain two validators using `.and()`.

---

### Answers

**A1.** A functional interface has **exactly one abstract method** (SAM — Single Abstract Method). It can have default and static methods, but only one abstract method. If you try to define two abstract methods, `@FunctionalInterface` causes a compile error.

**A2.** `Function<T,R>` takes input of type T and **returns** a result of type R (called with `.apply()`). `Consumer<T>` takes input of type T but **returns nothing** — used for side effects like printing, saving, logging (called with `.accept()`).

**A3.** `predicate.and(other)` returns a **new** `Predicate` that is true only if **both** predicates are true. There are also `.or()` (either) and `.negate()` (invert).

**A4.** `14` — `f.andThen(g)` means: apply `f` first (`4 * 3 = 12`), then apply `g` (`12 + 2 = 14`).

**A5.** `Supplier<T>` represents a factory or lazy value — it takes no input and produces a T when `.get()` is called. Real uses: lazy initialization, default value factories, test data generators. Example: `Supplier<List<String>> listFactory = ArrayList::new;`

**Part B Solution:**
```java
import java.util.*;
import java.util.function.*;

public class FunctionalDemo {

    public static List<String> filter(List<String> items, Predicate<String> predicate) {
        List<String> result = new ArrayList<>();
        for (String item : items) {
            if (predicate.test(item)) result.add(item);
        }
        return result;
    }

    public static <T, R> List<R> transform(List<T> items, Function<T, R> mapper) {
        List<R> result = new ArrayList<>();
        for (T item : items) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    public static void main(String[] args) {
        List<String> names = List.of("Alice", "Bob", "Charlie", "Dan", "Eve", "Frank");

        List<String> longNames = filter(names, name -> name.length() > 4);
        List<String> upper     = transform(longNames, String::toUpperCase);
        System.out.println(upper); // [ALICE, CHARLIE, FRANK]

        Validator<Integer> positive  = n -> n >= 1;
        Validator<Integer> notTooBig = n -> n <= 100;
        Validator<Integer> inRange   = positive.and(notTooBig);

        System.out.println(inRange.validate(50));  // true
        System.out.println(inRange.validate(0));   // false
        System.out.println(inRange.validate(101)); // false
    }
}
```
