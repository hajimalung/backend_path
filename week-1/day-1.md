# Week 1 — Day 1: JDK Setup + Java Syntax Crash Course

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. The Java Ecosystem (5 min)

| Term | What it means |
|------|--------------|
| **JDK** | Java Development Kit — compiler + tools. What you install to write Java. |
| **JRE** | Java Runtime — just enough to *run* compiled Java apps |
| **JVM** | Java Virtual Machine — executes bytecode on any OS |
| **Bytecode** | `.class` files compiled from `.java` — portable across platforms |

**Setup (do this first):**
1. Install JDK 21 from https://adoptium.net
2. Run `java --version` and `javac --version` to verify
3. Install IntelliJ IDEA Community (free) from https://jetbrains.com/idea/download

---

### 2. Your First Java Program (10 min)

```java
// File: HelloWorld.java
public class HelloWorld {                    // class name MUST match filename
    public static void main(String[] args) { // program entry point — always this
        System.out.println("Hello, World!"); // print + newline
        System.out.print("No newline");      // print without newline
    }
}
```

**Key rules coming from Python/JS:**
- Every file has one `public class` matching the filename
- All code lives inside classes; `main` is the entry point
- Statements end with `;`
- Java is **statically typed** — declare the type of every variable upfront

---

### 3. Data Types (15 min)

```java
// --- Primitive types (8 total, stored on the stack) ---
int     age     = 25;
long    pop     = 8_000_000_000L;  // L suffix for long literals
double  price   = 19.99;
float   temp    = 36.6f;           // f suffix for float
boolean active  = true;
char    grade   = 'A';             // single quotes for char
byte    b       = 127;
short   s       = 32000;

// --- Reference types (objects, stored on heap) ---
String  name    = "Alice";         // double quotes for String
int[]   nums    = {1, 2, 3};

// Autoboxing: Java wraps primitives into objects automatically
Integer boxed   = 42;   // int -> Integer (object wrapper)
int     prim    = boxed; // Integer -> int (unboxing)
```

**Critical rule — comparing Strings:**
```java
String a = new String("hello");
String b = new String("hello");

a == b        // FALSE — compares memory addresses
a.equals(b)   // TRUE  — compares values  ← always use this for objects
```

---

### 4. Control Flow (15 min)

```java
// if / else if / else
int score = 85;
if (score >= 90) {
    System.out.println("A");
} else if (score >= 80) {
    System.out.println("B");
} else {
    System.out.println("F");
}

// Switch expression (Java 14+ — prefer this modern form)
String result = switch (score / 10) {
    case 10, 9 -> "A";
    case 8     -> "B";
    case 7     -> "C";
    default    -> "F";
};

// Classic for loop
for (int i = 0; i < 5; i++) {
    System.out.print(i + " "); // 0 1 2 3 4
}

// while loop
int x = 0;
while (x < 3) {
    x++;
}

// Enhanced for-each loop
int[] values = {10, 20, 30};
for (int v : values) {
    System.out.println(v);
}
```

---

### 5. Methods (10 min)

```java
public class MathUtils {

    // Static method — call without an object instance
    public static int add(int a, int b) {
        return a + b;
    }

    // Instance method — need to create an object first
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        // Calling static method
        System.out.println(MathUtils.add(3, 4));  // 7

        // Calling instance method
        MathUtils util = new MathUtils();
        System.out.println(util.greet("Alice"));  // Hello, Alice!

        // var — type inference (Java 10+)
        var message = util.greet("Bob");          // type inferred as String
        System.out.println(message);
    }
}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between JDK and JRE? Which do you install to write Java?

**Q2.** What is wrong with this comparison, and how do you fix it?
```java
String x = "hello";
String y = new String("hello");
if (x == y) System.out.println("same");
```

**Q3.** What will this print? Why?
```java
int i = 0;
while (i < 4) {
    System.out.print(i + " ");
    i += 2;
}
```

**Q4.** What is autoboxing in Java? Give one example.

**Q5.** What does `static` mean on a method? How is calling it different from an instance method?

---

### Part B — Coding Challenge (20 min)

Write a program `FizzBuzz.java` that:
- Loops from 1 to 30
- Prints `"FizzBuzz"` if divisible by both 3 and 5
- Prints `"Fizz"` if divisible by 3 only
- Prints `"Buzz"` if divisible by 5 only
- Prints the number otherwise

Then extend it: store all FizzBuzz results in a `String[]` array and print the array length at the end.

---

### Answers

**A1.** JDK includes the compiler (`javac`), debugger, and all dev tools. JRE only runs pre-compiled apps. As a developer you always install the JDK — it includes the JRE.

**A2.** `==` compares memory references, not values. Since `y` is created with `new`, it's a different object. Fix: `x.equals(y)`.

**A3.** `0 2 ` — starts at 0, increments by 2 each iteration, stops when `i >= 4` (so prints 0 and 2).

**A4.** Autoboxing is Java's automatic conversion between primitives (`int`, `double`) and their wrapper classes (`Integer`, `Double`). Example: `Integer i = 5;` — Java automatically boxes `5` into an `Integer` object.

**A5.** `static` means the method belongs to the class, not an instance. You call it as `ClassName.method()`. An instance method requires creating an object first: `obj.method()`.

**Part B Solution:**
```java
public class FizzBuzz {
    public static void main(String[] args) {
        String[] results = new String[30];
        for (int i = 1; i <= 30; i++) {
            if (i % 15 == 0)     results[i-1] = "FizzBuzz";
            else if (i % 3 == 0) results[i-1] = "Fizz";
            else if (i % 5 == 0) results[i-1] = "Buzz";
            else                 results[i-1] = String.valueOf(i);

            System.out.println(results[i-1]);
        }
        System.out.println("Array length: " + results.length);
    }
}
```
