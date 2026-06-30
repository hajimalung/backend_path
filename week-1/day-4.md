# Week 1 — Day 4: Generics

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. Why Generics? (10 min)

Before generics (Java < 5), collections stored `Object` — requiring casts and risking runtime errors:

```java
// Without generics (old way — BAD)
List list = new ArrayList();
list.add("hello");
list.add(42);            // no compile error!
String s = (String) list.get(1); // ClassCastException at RUNTIME

// With generics (modern way — GOOD)
List<String> names = new ArrayList<>();
names.add("hello");
// names.add(42);        // compile error — caught early!
String name = names.get(0); // no cast needed
```

Generics provide **compile-time type safety** — catch type errors at compile time, not runtime.

---

### 2. Generic Classes (15 min)

```java
// A generic Box that can hold any type T
public class Box<T> {
    private T value;

    public Box(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Box[" + value + "]";
    }
}
```

```java
Box<String> stringBox = new Box<>("Hello");
Box<Integer> intBox   = new Box<>(42);
Box<Double>  dblBox   = new Box<>(3.14);

System.out.println(stringBox.getValue().toUpperCase()); // HELLO
System.out.println(intBox.getValue() * 2);              // 84
```

**Multiple type parameters:**
```java
public class Pair<A, B> {
    private A first;
    private B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst()  { return first; }
    public B getSecond() { return second; }
}

Pair<String, Integer> pair = new Pair<>("Alice", 30);
System.out.println(pair.getFirst() + " is " + pair.getSecond()); // Alice is 30
```

---

### 3. Generic Methods (10 min)

```java
public class Utils {

    // Generic method — <T> declared before return type
    public static <T> void swap(T[] arr, int i, int j) {
        T temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // Return the larger of two Comparable values
    public static <T extends Comparable<T>> T max(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
```

```java
String[] names = {"Alice", "Bob", "Charlie"};
Utils.swap(names, 0, 2);
System.out.println(names[0]); // Charlie

System.out.println(Utils.max(10, 25));        // 25
System.out.println(Utils.max("apple", "mango")); // mango (alphabetical)
```

---

### 4. Bounded Type Parameters (10 min)

Restrict what types `T` can be:

```java
// T must extend Number (so Integer, Double, Long etc. work)
public class NumberBox<T extends Number> {
    private T value;

    public NumberBox(T value) { this.value = value; }

    public double doubleValue() {
        return value.doubleValue(); // safe because T is a Number
    }
}

NumberBox<Integer> nb = new NumberBox<>(42);
System.out.println(nb.doubleValue()); // 42.0

// NumberBox<String> sb = new NumberBox<>("hi"); // compile error — String is not a Number
```

**Multiple bounds:**
```java
public <T extends Comparable<T> & Cloneable> T findMin(T a, T b) {
    return a.compareTo(b) <= 0 ? a : b;
}
```

---

### 5. Wildcards (10 min)

Use wildcards when you don't care about the exact type:

```java
import java.util.List;

// ? — unknown type (unbounded wildcard)
public static void printAll(List<?> list) {
    for (Object item : list) {
        System.out.println(item);
    }
}

// ? extends T — upper bounded (read-only, T or any subtype)
public static double sumList(List<? extends Number> list) {
    double sum = 0;
    for (Number n : list) sum += n.doubleValue();
    return sum;
}

// ? super T — lower bounded (write-only, T or any supertype)
public static void addNumbers(List<? super Integer> list) {
    list.add(1);
    list.add(2);
    list.add(3);
}
```

```java
printAll(List.of("a", "b", "c"));          // works with any list
printAll(List.of(1, 2, 3));               // works with any list

System.out.println(sumList(List.of(1, 2, 3)));          // 6.0
System.out.println(sumList(List.of(1.5, 2.5)));         // 4.0
```

**Producer Extends, Consumer Super (PECS):**
- If you only **read** from a collection: use `? extends T`
- If you only **write** to a collection: use `? super T`

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What problem do generics solve? Give one concrete example without generics that shows the issue.

**Q2.** What does `<T extends Comparable<T>>` mean? Why would you use it?

**Q3.** What is the difference between `List<?>` and `List<Object>`?

**Q4.** Can you create an instance of a generic type directly like `new T()`? Why or why not?

**Q5.** What does this method signature mean: `public static <T> List<T> repeat(T item, int times)`?

---

### Part B — Coding Challenge (20 min)

1. Create a generic class `Stack<T>` that supports:
   - `push(T item)` — adds item to the top
   - `T pop()` — removes and returns the top item (throw `RuntimeException` if empty)
   - `T peek()` — returns top without removing (throw `RuntimeException` if empty)
   - `boolean isEmpty()` — returns true if stack is empty
   - `int size()` — number of items

   Use `ArrayList<T>` internally.

2. Create a generic method `<T extends Comparable<T>> T findMax(List<T> list)` that returns the largest element in a list.

3. In `main`, test both with `Integer` and `String`.

---

### Answers

**A1.** Without generics, collections used `Object` — requiring explicit casts and allowing mismatched types to be added without compile errors. For example:
```java
List list = new ArrayList();
list.add("text");
Integer n = (Integer) list.get(0); // compiles but crashes at runtime
```
With generics: `List<String>` makes this a compile error.

**A2.** It means T must be a type that implements `Comparable<T>` (i.e., it can be compared to another T using `compareTo`). This lets you safely call `a.compareTo(b)` inside the method — useful for sorting or finding min/max.

**A3.** `List<?>` accepts a list of any type (including `List<String>`, `List<Integer>`) but you can only read `Object` from it and cannot add elements. `List<Object>` only accepts a `List<Object>` — you can't pass a `List<String>` to it (generics are not covariant).

**A4.** No. Due to **type erasure**, generic type information is removed at runtime. The JVM doesn't know what `T` is at runtime, so `new T()` is impossible. Workaround: pass a `Class<T>` and use reflection.

**A5.** It's a static generic method that takes any item of type `T` and an integer, and returns a `List<T>`. The `<T>` before the return type declares the type parameter for this method.

**Part B Solution:**
```java
import java.util.*;

public class Stack<T> {
    private List<T> items = new ArrayList<>();

    public void push(T item)  { items.add(item); }

    public T pop() {
        if (isEmpty()) throw new RuntimeException("Stack is empty");
        return items.remove(items.size() - 1);
    }

    public T peek() {
        if (isEmpty()) throw new RuntimeException("Stack is empty");
        return items.get(items.size() - 1);
    }

    public boolean isEmpty() { return items.isEmpty(); }
    public int size()        { return items.size(); }

    public static <T extends Comparable<T>> T findMax(List<T> list) {
        if (list.isEmpty()) throw new RuntimeException("List is empty");
        T max = list.get(0);
        for (T item : list) {
            if (item.compareTo(max) > 0) max = item;
        }
        return max;
    }

    public static void main(String[] args) {
        Stack<Integer> intStack = new Stack<>();
        intStack.push(1); intStack.push(3); intStack.push(2);
        System.out.println(intStack.pop());   // 2
        System.out.println(intStack.peek());  // 3

        System.out.println(findMax(List.of(5, 1, 8, 3)));        // 8
        System.out.println(findMax(List.of("apple", "mango", "berry"))); // mango
    }
}
```
