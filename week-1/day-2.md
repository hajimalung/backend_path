# Week 1 — Day 2: OOP in Java

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Classes and Objects (10 min)

A class is a blueprint; an object is an instance of that blueprint.

```java
// Define a class
public class Task {

    // Fields (instance variables)
    private int id;
    private String title;
    private String status;  // "TODO", "IN_PROGRESS", "DONE"

    // Constructor — called when creating an object with `new`
    public Task(int id, String title) {
        this.id = id;          // `this` refers to the current object
        this.title = title;
        this.status = "TODO";  // default value
    }

    // Getters (read fields)
    public int getId()       { return id; }
    public String getTitle() { return title; }
    public String getStatus(){ return status; }

    // Setter (modify field)
    public void setStatus(String status) {
        this.status = status;
    }

    // Behaviour method
    public void complete() {
        this.status = "DONE";
        System.out.println("Task '" + title + "' marked as DONE.");
    }

    // toString — called automatically when printing the object
    @Override
    public String toString() {
        return "[" + id + "] " + title + " — " + status;
    }
}
```

```java
// Create and use objects
Task t1 = new Task(1, "Learn Java OOP");
Task t2 = new Task(2, "Build REST API");

t1.complete();
System.out.println(t1);  // [1] Learn Java OOP — DONE
System.out.println(t2);  // [2] Build REST API — TODO
```

---

### 2. Access Modifiers (5 min)

| Modifier | Accessible from |
|----------|----------------|
| `public` | Anywhere |
| `private` | Only within the same class |
| `protected` | Same class + subclasses + same package |
| *(none)* | Same package only (package-private) |

**Rule of thumb**: fields should be `private`, methods that form the public API should be `public`.

---

### 3. Inheritance (15 min)

```java
// Base class (superclass / parent)
public class Animal {
    protected String name;

    public Animal(String name) {
        this.name = name;
    }

    public void makeSound() {
        System.out.println(name + " makes a sound");
    }

    public String getName() { return name; }
}

// Subclass (child class)
public class Dog extends Animal {

    private String breed;

    public Dog(String name, String breed) {
        super(name);   // call parent constructor FIRST
        this.breed = breed;
    }

    // Method overriding — replace parent behaviour
    @Override
    public void makeSound() {
        System.out.println(name + " barks!");
    }

    public String getBreed() { return breed; }
}
```

```java
Dog dog = new Dog("Rex", "Labrador");
dog.makeSound();            // Rex barks!   (overridden)
System.out.println(dog.getName()); // Rex (inherited from Animal)

// Polymorphism — a Dog IS an Animal
Animal a = new Dog("Buddy", "Poodle");
a.makeSound();              // Buddy barks! (runtime dispatch picks Dog's version)
```

---

### 4. Interfaces vs Abstract Classes (15 min)

```java
// Interface — defines a contract (what, not how)
public interface Printable {
    void print();                      // abstract by default
    default String format() {          // default implementation (Java 8+)
        return "Default format";
    }
}

// Abstract class — partial implementation, cannot be instantiated
public abstract class Shape {
    protected String color;

    public Shape(String color) {
        this.color = color;
    }

    // Abstract method — subclass MUST implement
    public abstract double area();

    // Concrete method — inherited as-is
    public void describe() {
        System.out.println("A " + color + " shape with area " + area());
    }
}

// Concrete class — extends abstract + implements interface
public class Circle extends Shape implements Printable {
    private double radius;

    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public void print() {
        System.out.println("Circle: radius=" + radius);
    }
}
```

**Key differences:**

| | Interface | Abstract Class |
|-|-----------|---------------|
| Can have fields | Only `static final` | Yes |
| Can have constructors | No | Yes |
| Supports multiple | `implements A, B, C` | Only one `extends` |
| Use when | Defining a capability | Sharing code between related classes |

---

### 5. Method Overloading (5 min)

Same method name, different parameter signature — resolved at **compile time**.

```java
public class Printer {
    public void print(String s)      { System.out.println(s); }
    public void print(int n)         { System.out.println(n); }
    public void print(String s, int n){ System.out.println(s + ": " + n); }
}
```

Do not confuse with **overriding** (same signature, different class in hierarchy — resolved at **runtime**).

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What keyword is used to call a parent class constructor from a child class? Where must it appear?

**Q2.** Can a class implement multiple interfaces? Can it extend multiple classes? Explain why.

**Q3.** What is the difference between method overloading and method overriding?

**Q4.** What does `@Override` do? Is it required?

**Q5.** Why should class fields typically be `private` rather than `public`?

---

### Part B — Coding Challenge (20 min)

Design a small class hierarchy for a library system:

1. Create an abstract class `LibraryItem` with:
   - Fields: `title` (String), `id` (int)
   - Constructor taking both fields
   - Abstract method `getType()` returning a String
   - Concrete method `display()` that prints `"[ID] Title — Type"`

2. Create interface `Borrowable` with:
   - Method `borrow(String borrowerName)` (print who borrowed it)
   - Method `returnItem()` (print it was returned)

3. Create class `Book` that extends `LibraryItem` and implements `Borrowable`:
   - Extra field: `author` (String)
   - Implement all required methods

4. In `main`: create a `Book`, display it, borrow it, and return it.

---

### Answers

**A1.** `super(...)` calls the parent constructor. It **must be the first statement** in the child constructor.

**A2.** A class can implement **multiple interfaces** (`implements A, B, C`) but can only extend **one class** (`extends X`). Java uses single inheritance for classes to avoid the "diamond problem" — ambiguity when multiple parents define the same method.

**A3.** **Overloading** = same name, different parameters, same class, resolved at compile time. **Overriding** = same name, same parameters, subclass redefines parent method, resolved at runtime (dynamic dispatch).

**A4.** `@Override` tells the compiler "this method is intentionally overriding a parent method." It's not required but is strongly recommended — the compiler will error if the method doesn't actually override anything (catches typos).

**A5.** Encapsulation — hiding fields prevents external code from putting objects into invalid states. You control access through getters/setters, where you can add validation.

**Part B Solution:**
```java
public abstract class LibraryItem {
    protected int id;
    protected String title;

    public LibraryItem(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public abstract String getType();

    public void display() {
        System.out.println("[" + id + "] " + title + " — " + getType());
    }
}

public interface Borrowable {
    void borrow(String borrowerName);
    void returnItem();
}

public class Book extends LibraryItem implements Borrowable {
    private String author;

    public Book(int id, String title, String author) {
        super(id, title);
        this.author = author;
    }

    @Override
    public String getType() { return "Book by " + author; }

    @Override
    public void borrow(String borrowerName) {
        System.out.println(title + " borrowed by " + borrowerName);
    }

    @Override
    public void returnItem() {
        System.out.println(title + " has been returned.");
    }

    public static void main(String[] args) {
        Book b = new Book(1, "Effective Java", "Joshua Bloch");
        b.display();          // [1] Effective Java — Book by Joshua Bloch
        b.borrow("Alice");    // Effective Java borrowed by Alice
        b.returnItem();       // Effective Java has been returned.
    }
}
```
