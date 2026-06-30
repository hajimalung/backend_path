# Week 2 — Day 3: Optional, Method References, and Comparator

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Optional — Eliminating NullPointerException (20 min)

`Optional<T>` is a container that may or may not hold a value. It forces you to explicitly handle the "no value" case.

```java
import java.util.Optional;

// Creating Optional
Optional<String> present = Optional.of("Hello");          // value is present
Optional<String> empty   = Optional.empty();               // no value
Optional<String> maybe   = Optional.ofNullable(null);     // null becomes empty
Optional<String> maybe2  = Optional.ofNullable("value");  // non-null becomes present
```

**Reading values:**
```java
Optional<String> name = Optional.of("Alice");

// isPresent / isEmpty
name.isPresent()   // true
name.isEmpty()     // false (Java 11+)

// get() — ONLY call after checking isPresent()
if (name.isPresent()) {
    System.out.println(name.get()); // Alice
}

// orElse — return default if empty
String result = name.orElse("Unknown");   // "Alice"
Optional.empty().orElse("Unknown");       // "Unknown"

// orElseGet — compute default lazily (prefer over orElse for expensive defaults)
String result = name.orElseGet(() -> fetchDefaultName()); // only called if empty

// orElseThrow — throw exception if empty
String result = name.orElseThrow(() -> new RuntimeException("Name not found"));

// ifPresent — consume value if present (no return value)
name.ifPresent(n -> System.out.println("Hello, " + n));

// ifPresentOrElse (Java 9+)
name.ifPresentOrElse(
    n -> System.out.println("Hello, " + n),
    () -> System.out.println("No name found")
);
```

**Transforming Optional (like streams):**
```java
Optional<String> name = Optional.of("  alice  ");

// map — transform value if present
Optional<String> upper = name.map(String::trim).map(String::toUpperCase);
System.out.println(upper.get()); // "ALICE"

// flatMap — when mapper returns Optional (avoids Optional<Optional<T>>)
Optional<String> email = findUser("alice")
    .flatMap(user -> findEmail(user.getId()));

// filter — keep value only if it matches predicate
Optional<Integer> age = Optional.of(25);
age.filter(a -> a >= 18).ifPresent(a -> System.out.println("Adult")); // Adult
Optional.of(10).filter(a -> a >= 18).ifPresent(a -> System.out.println("Adult")); // nothing
```

**Real use case — service layer:**
```java
// Repository returns Optional
public Optional<User> findById(int id) {
    return Optional.ofNullable(database.get(id));
}

// Service uses Optional safely
public String getUserEmail(int id) {
    return findById(id)
        .map(User::getEmail)
        .orElseThrow(() -> new UserNotFoundException(id));
}
```

---

### 2. Method References (15 min)

Method references are shorthand for lambdas that just call a method. Syntax: `ClassName::method`

```java
// 4 types of method references

// 1. Static method reference
// Lambda:          n -> Integer.parseInt(n)
// Method ref:
Function<String, Integer> parse = Integer::parseInt;
System.out.println(parse.apply("42")); // 42

// 2. Instance method reference on a specific object
String prefix = "Hello";
// Lambda:          s -> prefix.concat(s)
// Method ref:
Function<String, String> greet = prefix::concat;
System.out.println(greet.apply(", World")); // Hello, World

// 3. Instance method reference on an arbitrary instance
// Lambda:          s -> s.toUpperCase()
// Method ref:
Function<String, String> upper = String::toUpperCase;
List.of("a","b","c").stream().map(String::toUpperCase).forEach(System.out::println);

// 4. Constructor reference
// Lambda:          () -> new ArrayList<>()
// Method ref:
Supplier<List<String>> listFactory = ArrayList::new;
List<String> newList = listFactory.get();

// With argument
// Lambda:          s -> new StringBuilder(s)
Function<String, StringBuilder> sbFactory = StringBuilder::new;
```

**Method references in practice:**
```java
List<String> names = List.of("Charlie", "Alice", "Bob");

// All equivalent:
names.forEach(s -> System.out.println(s));  // lambda
names.forEach(System.out::println);         // method reference (cleaner)

// Sort by length
names.stream()
    .sorted(Comparator.comparingInt(String::length)) // method ref
    .forEach(System.out::println);

// Collect to map: name -> length
Map<String, Integer> nameLengths = names.stream()
    .collect(Collectors.toMap(s -> s, String::length));
```

---

### 3. Comparator Chaining (15 min)

```java
import java.util.*;

record Person(String name, int age, String city) {}

List<Person> people = new ArrayList<>(List.of(
    new Person("Alice", 30, "London"),
    new Person("Bob",   25, "Paris"),
    new Person("Charlie", 30, "London"),
    new Person("Diana", 25, "Berlin")
));

// Simple sort by age
people.sort(Comparator.comparingInt(Person::age));

// Reversed
people.sort(Comparator.comparingInt(Person::age).reversed());

// Chain sorts: by age, then by name within same age
people.sort(Comparator.comparingInt(Person::age)
    .thenComparing(Person::name));

// Multiple chains: by city, then age desc, then name
people.sort(Comparator.comparing(Person::city)
    .thenComparingInt(Person::age).reversed()
    .thenComparing(Person::name));

// naturalOrder and reverseOrder (for String, Integer etc.)
List<String> names = Arrays.asList("Charlie", "Alice", "Bob");
names.sort(Comparator.naturalOrder());       // [Alice, Bob, Charlie]
names.sort(Comparator.reverseOrder());       // [Charlie, Bob, Alice]

// nullsFirst / nullsLast
List<String> withNulls = Arrays.asList("Bob", null, "Alice");
withNulls.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
// [null, Alice, Bob]

// Stream with custom Comparator
Optional<Person> youngest = people.stream()
    .min(Comparator.comparingInt(Person::age));
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between `Optional.orElse()` and `Optional.orElseGet()`? When does it matter?

**Q2.** What are the 4 types of method references? Give a one-line example of each.

**Q3.** What does this print?
```java
Optional.of("hello")
    .filter(s -> s.length() > 10)
    .map(String::toUpperCase)
    .orElse("too short");
```

**Q4.** How does `Comparator.thenComparing()` work?

**Q5.** What will happen if you call `Optional.get()` on an empty Optional?

---

### Part B — Coding Challenge (20 min)

Given this record: `record Product(String name, String category, double price, int stock)`

1. Write a method that takes `Optional<Product>` and returns a formatted String `"name ($price)"` or `"Product not found"` if empty — use only Optional methods, no if/else.

2. Sort a list of products: first by category (alphabetical), then by price (ascending) within the same category.

3. Find the most expensive in-stock product (stock > 0) using streams — return as `Optional<Product>`.

4. Use method references wherever you can replace lambdas.

---

### Answers

**A1.** `orElse(default)` **always evaluates** the default value, even if the Optional has a value. `orElseGet(supplier)` only evaluates the supplier **if the Optional is empty**. When the default involves an expensive computation (DB call, network request), use `orElseGet` to avoid unnecessary work.

**A2.**
- Static: `Integer::parseInt` (replaces `s -> Integer.parseInt(s)`)
- Specific instance: `myString::startsWith` (replaces `s -> myString.startsWith(s)`)
- Arbitrary instance: `String::toUpperCase` (replaces `s -> s.toUpperCase()`)
- Constructor: `ArrayList::new` (replaces `() -> new ArrayList<>()`)

**A3.** `"too short"` — `"hello"` has length 5, which is not > 10, so `filter` makes the Optional empty. `map` on empty returns empty. `orElse` returns the default `"too short"`.

**A4.** `thenComparing()` adds a secondary sort key. If two elements compare as equal by the primary comparator, the secondary one is used to break the tie. You can chain as many as needed: `byAge.thenComparing(byName).thenComparing(byCity)`.

**A5.** It throws `NoSuchElementException`. Always check `isPresent()` first, or better, use `orElse` / `orElseThrow` / `ifPresent` instead of `get()`.

**Part B Solution:**
```java
record Product(String name, String category, double price, int stock) {}

// 1. Format Optional<Product>
public static String formatProduct(Optional<Product> product) {
    return product
        .map(p -> p.name() + " ($" + p.price() + ")")
        .orElse("Product not found");
}

// 2. Sort by category then price
products.sort(Comparator.comparing(Product::category)
    .thenComparingDouble(Product::price));

// 3. Most expensive in-stock
Optional<Product> mostExpensive = products.stream()
    .filter(p -> p.stock() > 0)
    .max(Comparator.comparingDouble(Product::price));

// 4. Usage in main
List<Product> products = new ArrayList<>(List.of(
    new Product("Laptop",  "Electronics", 999.99, 5),
    new Product("Phone",   "Electronics", 699.99, 0),
    new Product("Desk",    "Furniture",   299.99, 3),
    new Product("Chair",   "Furniture",   199.99, 10)
));

System.out.println(formatProduct(Optional.empty()));
System.out.println(formatProduct(Optional.of(products.get(0))));

products.sort(Comparator.comparing(Product::category).thenComparingDouble(Product::price));
products.forEach(p -> System.out.println(p.name() + " - " + p.category()));

mostExpensive = products.stream()
    .filter(p -> p.stock() > 0)
    .max(Comparator.comparingDouble(Product::price));
mostExpensive.ifPresent(p -> System.out.println("Most expensive: " + p.name()));
```
