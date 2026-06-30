# Week 1 — Day 5: Exception Handling

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. The Exception Hierarchy (5 min)

```
Throwable
├── Error           (JVM problems — OutOfMemoryError, StackOverflowError — don't catch these)
└── Exception
    ├── RuntimeException     (unchecked — don't need to declare or catch)
    │   ├── NullPointerException
    │   ├── ArrayIndexOutOfBoundsException
    │   ├── IllegalArgumentException
    │   ├── IllegalStateException
    │   └── NumberFormatException
    └── (checked exceptions)  ← must be caught or declared with `throws`
        ├── IOException
        ├── SQLException
        └── FileNotFoundException
```

**Checked** = compiler forces you to handle them. **Unchecked (Runtime)** = optional to handle.

---

### 2. try / catch / finally (15 min)

```java
public class ExceptionDemo {

    public static int divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return a / b;
    }

    public static void main(String[] args) {

        // Basic try-catch
        try {
            int result = divide(10, 0);
            System.out.println(result); // never reached
        } catch (ArithmeticException e) {
            System.out.println("Error: " + e.getMessage()); // Error: Cannot divide by zero
        }

        // Multiple catch blocks
        try {
            String[] arr = {"a", "b"};
            System.out.println(arr[5]);        // ArrayIndexOutOfBoundsException
            int n = Integer.parseInt("abc");   // NumberFormatException (unreachable)
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Bad index: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Bad number");
        } catch (Exception e) {
            System.out.println("Catch-all: " + e.getMessage()); // most general last!
        } finally {
            System.out.println("This ALWAYS runs (cleanup code goes here)");
        }

        // Multi-catch (Java 7+) — same handler for multiple types
        try {
            riskyOperation();
        } catch (IOException | SQLException e) {
            System.out.println("IO or SQL error: " + e.getMessage());
        }
    }
}
```

---

### 3. try-with-resources (10 min)

For resources that must be closed (files, DB connections, streams). Implements `AutoCloseable`.

```java
import java.io.*;

// Old way — easy to forget close(), especially on exception
FileReader fr = null;
try {
    fr = new FileReader("file.txt");
    // read...
} catch (IOException e) {
    e.printStackTrace();
} finally {
    if (fr != null) try { fr.close(); } catch (IOException e) { }
}

// Modern way — resource auto-closed even if exception occurs
try (FileReader fr = new FileReader("file.txt");
     BufferedReader br = new BufferedReader(fr)) {
    String line;
    while ((line = br.readLine()) != null) {
        System.out.println(line);
    }
} catch (IOException e) {
    System.out.println("File error: " + e.getMessage());
}
// fr and br are automatically closed here
```

---

### 4. Custom Exceptions (15 min)

Creating your own exceptions makes code more expressive and error handling more specific.

```java
// Custom checked exception
public class InsufficientFundsException extends Exception {
    private double amount;

    public InsufficientFundsException(double amount) {
        super("Insufficient funds. Shortfall: $" + amount);
        this.amount = amount;
    }

    public double getAmount() { return amount; }
}

// Custom unchecked exception
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(int id) {
        super("Task with ID " + id + " not found");
    }
}
```

```java
public class BankAccount {
    private double balance;

    public BankAccount(double balance) {
        this.balance = balance;
    }

    // checked exception — caller MUST handle it
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount > balance) {
            throw new InsufficientFundsException(amount - balance);
        }
        balance -= amount;
    }

    public static void main(String[] args) {
        BankAccount acc = new BankAccount(100.0);
        try {
            acc.withdraw(150.0);
        } catch (InsufficientFundsException e) {
            System.out.println(e.getMessage());       // Insufficient funds. Shortfall: $50.0
            System.out.println("Shortfall: " + e.getAmount()); // 50.0
        }
    }
}
```

---

### 5. Best Practices (10 min)

```java
// DO: Be specific with exception types
catch (FileNotFoundException e) { ... }    // Good
catch (Exception e) { ... }               // Too broad — only as last resort

// DO: Include meaningful messages
throw new IllegalArgumentException("Age must be positive, got: " + age);

// DO: Log or rethrow, don't silently swallow
catch (Exception e) {
    logger.error("Failed to process task", e); // log with stack trace
    throw e;                                   // rethrow if caller needs to know
}

// DON'T: Catch and ignore
catch (Exception e) { }  // BAD — exception disappears silently

// DON'T: Use exceptions for flow control
// BAD:
try {
    return list.get(index);
} catch (IndexOutOfBoundsException e) {
    return null;
}
// GOOD:
if (index < list.size()) return list.get(index);
return null;

// DO: Use IllegalArgumentException for bad inputs
public void setAge(int age) {
    if (age < 0) throw new IllegalArgumentException("Age cannot be negative: " + age);
    this.age = age;
}

// DO: Use IllegalStateException for invalid object state
public void start() {
    if (isRunning) throw new IllegalStateException("Already running");
    isRunning = true;
}
```

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What is the difference between a checked exception and an unchecked exception? Give one example of each.

**Q2.** What does `finally` guarantee? Give a real use case for it.

**Q3.** When would you extend `Exception` vs `RuntimeException` for a custom exception?

**Q4.** What is wrong with this code?
```java
try {
    doSomething();
} catch (Exception e) {
    // nothing here
}
```

**Q5.** What is `try-with-resources` and what interface must a resource implement to use it?

---

### Part B — Coding Challenge (20 min)

Create a simple `UserValidator` class:

1. Create a custom unchecked exception `ValidationException` with a message field.

2. Create a class `UserValidator` with a static method `validate(String name, int age, String email)` that:
   - Throws `ValidationException("Name cannot be blank")` if name is null or empty
   - Throws `ValidationException("Age must be between 0 and 120")` if age is out of range
   - Throws `ValidationException("Email must contain @")` if email doesn't contain `@`

3. In `main`:
   - Try to validate a valid user — print "Valid user"
   - Try to validate an invalid user — catch and print the error message
   - Use a loop to validate 3 users (mix of valid and invalid) without crashing

---

### Answers

**A1.** Checked exceptions must be handled (caught or declared with `throws`) — the compiler enforces this. Example: `IOException`. Unchecked (runtime) exceptions don't require handling. Example: `NullPointerException`, `IllegalArgumentException`. Use checked for recoverable situations (file not found), unchecked for programming errors (invalid arguments).

**A2.** `finally` block **always executes** regardless of whether an exception was thrown or caught. Use it for cleanup: closing files, releasing locks, resetting state. Now mostly replaced by try-with-resources for `AutoCloseable` resources.

**A3.** Extend `Exception` (checked) when the caller can reasonably recover and should be forced to handle it — like `InsufficientFundsException`. Extend `RuntimeException` (unchecked) for programming errors or validation failures where the caller may or may not choose to handle it — like `ValidationException` or `TaskNotFoundException`.

**A4.** This silently swallows the exception — the error is lost completely. At minimum, log the exception: `e.printStackTrace()` or `logger.error("...", e)`. This is one of the most dangerous patterns in Java.

**A5.** `try-with-resources` automatically closes resources when the try block exits (normally or via exception). The resource must implement `java.lang.AutoCloseable` (which defines a `close()` method). `Closeable` (for IO) extends `AutoCloseable`.

**Part B Solution:**
```java
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

public class UserValidator {
    public static void validate(String name, int age, String email) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Name cannot be blank");
        }
        if (age < 0 || age > 120) {
            throw new ValidationException("Age must be between 0 and 120");
        }
        if (email == null || !email.contains("@")) {
            throw new ValidationException("Email must contain @");
        }
    }

    public static void main(String[] args) {
        Object[][] users = {
            {"Alice",  30, "alice@example.com"},
            {"",       25, "bob@example.com"},
            {"Charlie", -1, "charlie@example.com"},
        };

        for (Object[] user : users) {
            try {
                validate((String) user[0], (int) user[1], (String) user[2]);
                System.out.println("Valid user: " + user[0]);
            } catch (ValidationException e) {
                System.out.println("Invalid: " + e.getMessage());
            }
        }
    }
}
```
