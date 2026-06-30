# Week 1 — Day 6: String API, Records, and Enums

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. String API Deep Dive (20 min)

Strings are **immutable** in Java — every operation creates a new String.

```java
String s = "  Hello, World!  ";

// Case
s.toUpperCase()         // "  HELLO, WORLD!  "
s.toLowerCase()         // "  hello, world!  "

// Trim / Strip
s.trim()                // "Hello, World!" (removes ASCII whitespace)
s.strip()               // "Hello, World!" (Unicode-aware, prefer this)
s.stripLeading()        // "Hello, World!  "
s.stripTrailing()       // "  Hello, World!"

// Search
s.contains("World")     // true
s.startsWith("  He")    // true
s.endsWith("!  ")       // true
s.indexOf("o")          // 5 (first occurrence)
s.lastIndexOf("o")      // 9

// Extract
s.substring(2, 7)       // "Hello"
s.charAt(2)             // 'H'

// Replace
s.replace("World", "Java")       // "  Hello, Java!  "
s.replaceAll("\\s+", "_")        // replace all whitespace runs with _

// Split
"a,b,c".split(",")               // ["a", "b", "c"]
"one  two   three".split("\\s+") // ["one", "two", "three"]

// Join (Java 8+)
String.join(", ", "A", "B", "C") // "A, B, C"
String.join("-", List.of("x","y","z")) // "x-y-z"

// Check empty/blank
"".isEmpty()            // true
"  ".isEmpty()          // false (has spaces)
"  ".isBlank()          // true  (blank = only whitespace)

// Convert
String.valueOf(42)      // "42"
Integer.parseInt("42")  // 42
Double.parseDouble("3.14") // 3.14

// Formatted strings (Java 15+ — prefer over String.format)
String name = "Alice";
int age = 30;
String msg = "Name: %s, Age: %d".formatted(name, age); // "Name: Alice, Age: 30"
```

**StringBuilder — for building strings in loops:**
```java
// BAD — creates many String objects
String result = "";
for (int i = 0; i < 1000; i++) {
    result += i;  // O(n²) — very slow
}

// GOOD — mutable, efficient
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i);
}
String result = sb.toString(); // convert to String once at the end
```

---

### 2. Records (Java 16+) (15 min)

`record` is a concise way to create immutable data classes. The compiler auto-generates constructor, getters, `equals`, `hashCode`, and `toString`.

```java
// Old way — boilerplate heavy
public class Point {
    private final int x;
    private final int y;

    public Point(int x, int y) { this.x = x; this.y = y; }
    public int x() { return x; }
    public int y() { return y; }

    @Override
    public boolean equals(Object o) { ... }
    @Override
    public int hashCode()           { ... }
    @Override
    public String toString()        { ... }
}

// Modern way — record does all of the above in one line!
public record Point(int x, int y) { }
```

```java
Point p1 = new Point(3, 4);
Point p2 = new Point(3, 4);

System.out.println(p1.x());       // 3  (getter — note: no "get" prefix)
System.out.println(p1.y());       // 4
System.out.println(p1);           // Point[x=3, y=4]  (auto toString)
System.out.println(p1.equals(p2)); // true  (auto equals — value-based)
```

**Adding behaviour to records:**
```java
public record Circle(double radius) {

    // Compact constructor — add validation
    public Circle {
        if (radius <= 0) throw new IllegalArgumentException("Radius must be positive");
    }

    // Custom methods are allowed
    public double area() {
        return Math.PI * radius * radius;
    }

    public double perimeter() {
        return 2 * Math.PI * radius;
    }
}

Circle c = new Circle(5.0);
System.out.println(c.area());      // ~78.5
System.out.println(c.perimeter()); // ~31.4
// c.radius = 10; // compile error — records are immutable
```

**Use records for:** DTOs, value objects, config holders, response/request objects in Spring.

---

### 3. Enums (20 min)

Enums are a special class with a fixed set of named constants. Much more powerful than string constants.

```java
// Basic enum
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED
}

TaskStatus status = TaskStatus.IN_PROGRESS;
System.out.println(status);       // IN_PROGRESS
System.out.println(status.name()); // "IN_PROGRESS" (String)
System.out.println(status.ordinal()); // 1 (0-based index)

// All values
for (TaskStatus s : TaskStatus.values()) {
    System.out.println(s);
}

// Switch on enum
String label = switch (status) {
    case TODO        -> "Not started";
    case IN_PROGRESS -> "In progress";
    case DONE        -> "Completed";
    case CANCELLED   -> "Cancelled";
};
```

**Enums with fields and methods:**
```java
public enum Priority {
    LOW(1), MEDIUM(5), HIGH(10), CRITICAL(100);

    private final int score;

    Priority(int score) {         // private constructor
        this.score = score;
    }

    public int getScore() { return score; }

    public boolean isUrgent() { return score >= 10; }
}
```

```java
Priority p = Priority.HIGH;
System.out.println(p.getScore());  // 10
System.out.println(p.isUrgent());  // true

Priority.valueOf("LOW").getScore() // 1 (convert from String to enum)
```

**Enum in a real model:**
```java
public record Task(int id, String title, TaskStatus status, Priority priority) { }

Task t = new Task(1, "Fix bug", TaskStatus.IN_PROGRESS, Priority.HIGH);
System.out.println(t);
// Task[id=1, title=Fix bug, status=IN_PROGRESS, priority=HIGH]
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What does `String.isBlank()` return that `String.isEmpty()` doesn't cover?

**Q2.** Why is concatenating strings inside a loop with `+` a performance problem? What should you use instead?

**Q3.** What does a Java `record` auto-generate for you? Name at least 4 things.

**Q4.** Can you add instance methods to a `record`? Can you add mutable fields?

**Q5.** What is `Priority.valueOf("HIGH")` and what happens if you pass `"high"` (lowercase)?

---

### Part B — Coding Challenge (20 min)

1. Create an enum `DayOfWeek` with values MON through SUN. Add:
   - A boolean method `isWeekend()` that returns true for SAT and SUN
   - A String method `abbreviation()` that returns the first 3 characters of the name

2. Create a `record` named `Employee` with fields: `name` (String), `id` (int), `department` (String), `salary` (double). Add:
   - A method `annualSalary()` that returns salary × 12
   - Validation in the compact constructor that salary must be > 0

3. In `main`:
   - Print all weekdays (MON–FRI) using a loop + `isWeekend()`
   - Create an `Employee` and print their name and annual salary
   - Try creating an `Employee` with salary = -500 and catch the exception

---

### Answers

**A1.** `isEmpty()` returns `true` only if the String has length 0 (empty string `""`). `isBlank()` returns `true` if the String is empty OR contains only whitespace characters (spaces, tabs, newlines). `"  ".isEmpty()` is `false`; `"  ".isBlank()` is `true`.

**A2.** Strings are immutable, so `str += x` creates a new String object every iteration, resulting in O(n²) time and memory allocation. Use `StringBuilder.append()` in loops and call `.toString()` once at the end.

**A3.** Records auto-generate: **constructor** (taking all fields), **accessor methods** (e.g., `name()`, `age()`), **`equals()`** (value-based), **`hashCode()`**, and **`toString()`**.

**A4.** Yes, you can add instance methods (like `area()`, custom logic). No, you cannot add mutable (non-final) instance fields — all record fields are `private final` by design. Records are immutable by definition.

**A5.** `Priority.valueOf("HIGH")` looks up the enum constant by exact name and returns `Priority.HIGH`. If you pass `"high"` (lowercase), it throws `IllegalArgumentException` because enum names are case-sensitive.

**Part B Solution:**
```java
public enum DayOfWeek {
    MON, TUE, WED, THU, FRI, SAT, SUN;

    public boolean isWeekend() {
        return this == SAT || this == SUN;
    }

    public String abbreviation() {
        return name().substring(0, 3);
    }
}

public record Employee(String name, int id, String department, double salary) {
    public Employee {
        if (salary <= 0) throw new IllegalArgumentException("Salary must be positive");
    }

    public double annualSalary() { return salary * 12; }
}

// main:
for (DayOfWeek day : DayOfWeek.values()) {
    if (!day.isWeekend()) System.out.println(day.abbreviation());
}

Employee emp = new Employee("Alice", 1, "Engineering", 5000.0);
System.out.println(emp.name() + " earns " + emp.annualSalary() + " per year");

try {
    new Employee("Bob", 2, "HR", -500);
} catch (IllegalArgumentException e) {
    System.out.println("Error: " + e.getMessage());
}
```
