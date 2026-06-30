# Week 1 — Day 7: Practice Day — LeetCode Problems in Java

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Goal for Today

Today is a hands-on practice day. You've covered: syntax, OOP, collections, generics, exceptions, String API, records, and enums. Now you'll reinforce all of it by solving problems in Java — focusing on **Java fluency**, not algorithm complexity.

---

## Learning Content

### 1. Review: Common Patterns You'll Use Today (15 min)

**String manipulation:**
```java
String s = "hello world";
s.split(" ")          // ["hello", "world"]
s.charAt(0)           // 'h'
s.toCharArray()       // ['h','e','l','l','o',' ','w','o','r','l','d']
new StringBuilder(s).reverse().toString() // "dlrow olleh"
String.valueOf(42)    // "42"
Character.isDigit('3') // true
Character.isLetter('a') // true
```

**Array and List basics:**
```java
int[] arr = {3, 1, 4, 1, 5};
Arrays.sort(arr);                    // [1, 1, 3, 4, 5]
Arrays.toString(arr)                 // "[1, 1, 3, 4, 5]"
int[] copy = Arrays.copyOf(arr, 3);  // [1, 1, 3]

List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 4));
Collections.sort(list);              // [1, 3, 4]
Collections.reverse(list);           // [4, 3, 1]
Collections.max(list)                // 4
```

**HashMap for counting/frequency:**
```java
Map<Character, Integer> freq = new HashMap<>();
for (char c : "aabbbc".toCharArray()) {
    freq.put(c, freq.getOrDefault(c, 0) + 1);
}
// {a=2, b=3, c=1}
```

**Two-pointer pattern:**
```java
int left = 0, right = arr.length - 1;
while (left < right) {
    // process arr[left] and arr[right]
    left++;
    right--;
}
```

---

### 2. Five Practice Problems (45 min — ~9 min each)

Solve each before reading the hint or solution.

---

#### Problem 1: Reverse a String (5 min)

Write a method `String reverse(String s)` that reverses the string without using `StringBuilder.reverse()`.

**Example:** `reverse("hello")` → `"olleh"`

**Hint:** Use a two-pointer approach with `char[]`.

---

#### Problem 2: Check if String is a Palindrome (10 min)

Write `boolean isPalindrome(String s)` that returns true if `s` reads the same forward and backward (ignore case, ignore non-alphanumeric characters).

**Example:** `isPalindrome("A man, a plan, a canal: Panama")` → `true`
**Example:** `isPalindrome("race a car")` → `false`

---

#### Problem 3: Find Most Frequent Character (10 min)

Write `char mostFrequent(String s)` that returns the character that appears most often (ignore spaces). If tie, return the first one found.

**Example:** `mostFrequent("java is great")` → `'a'` (appears 3 times)

---

#### Problem 4: Remove Duplicates from List (10 min)

Write `List<Integer> removeDuplicates(List<Integer> list)` that removes duplicates while **preserving the original order**.

**Example:** `[3, 1, 4, 1, 5, 9, 2, 6, 5]` → `[3, 1, 4, 5, 9, 2, 6]`

**Hint:** Use `LinkedHashSet` (maintains insertion order, no duplicates).

---

#### Problem 5: Group Words by Anagram (10 min)

Write `Map<String, List<String>> groupAnagrams(List<String> words)` that groups words that are anagrams of each other.

**Example:** `["eat", "tea", "tan", "ate", "nat", "bat"]`
Returns: `{"aet": ["eat", "tea", "ate"], "ant": ["tan", "nat"], "abt": ["bat"]}`

**Hint:** Sort each word's characters to create a common key.

---

## Solutions

### Solution 1: Reverse a String

```java
public static String reverse(String s) {
    char[] chars = s.toCharArray();
    int left = 0, right = chars.length - 1;
    while (left < right) {
        char temp = chars[left];
        chars[left] = chars[right];
        chars[right] = temp;
        left++;
        right--;
    }
    return new String(chars);
}
```

---

### Solution 2: Palindrome Check

```java
public static boolean isPalindrome(String s) {
    // Keep only alphanumeric, lowercase
    String cleaned = s.toLowerCase().replaceAll("[^a-z0-9]", "");
    int left = 0, right = cleaned.length() - 1;
    while (left < right) {
        if (cleaned.charAt(left) != cleaned.charAt(right)) return false;
        left++;
        right--;
    }
    return true;
}
```

---

### Solution 3: Most Frequent Character

```java
public static char mostFrequent(String s) {
    Map<Character, Integer> freq = new HashMap<>();
    char best = s.charAt(0);
    int maxCount = 0;
    for (char c : s.toCharArray()) {
        if (c == ' ') continue;
        int count = freq.merge(c, 1, Integer::sum);
        if (count > maxCount) {
            maxCount = count;
            best = c;
        }
    }
    return best;
}
```

---

### Solution 4: Remove Duplicates (Preserve Order)

```java
public static List<Integer> removeDuplicates(List<Integer> list) {
    return new ArrayList<>(new LinkedHashSet<>(list));
}
```

---

### Solution 5: Group Anagrams

```java
public static Map<String, List<String>> groupAnagrams(List<String> words) {
    Map<String, List<String>> groups = new HashMap<>();
    for (String word : words) {
        char[] sorted = word.toCharArray();
        Arrays.sort(sorted);
        String key = new String(sorted);
        groups.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
    }
    return groups;
}
```

---

## Today's Test

> Maximum 30 minutes.

### Quick Self-Assessment Quiz (10 min)

Answer these without running code:

**Q1.** What is the output?
```java
List<Integer> list = Arrays.asList(3, 1, 2);
Collections.sort(list);
System.out.println(list.get(1));
```

**Q2.** How does `LinkedHashSet` differ from `HashSet`?

**Q3.** What does `map.computeIfAbsent(key, k -> new ArrayList<>())` do?

**Q4.** What does `replaceAll("[^a-z0-9]", "")` remove from a String?

**Q5.** What does `Arrays.asList()` return — is it modifiable?

---

### Bonus Coding Challenge (20 min)

Write a method `Map<Character, Long> charFrequency(String s)` using the **Streams API** (preview of tomorrow's topic):
```java
return s.chars()
        .mapToObj(c -> (char) c)
        .filter(c -> c != ' ')
        .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
```

Then print the top 3 most frequent characters in descending order of frequency.

---

### Answers

**A1.** `2` — after sorting `[3,1,2]` becomes `[1,2,3]`, and index 1 is `2`.

**A2.** `HashSet` has no guaranteed ordering. `LinkedHashSet` maintains insertion order — elements come out in the order they were added.

**A3.** If the key exists, returns the existing value. If not, computes the value using the function (`new ArrayList<>()`), stores it in the map, and returns it. It's a cleaner alternative to an if-null check.

**A4.** It removes every character that is NOT (`^`) a lowercase letter `a-z` or digit `0-9` — effectively stripping punctuation, spaces, and special characters.

**A5.** `Arrays.asList()` returns a **fixed-size** `List` backed by the array — you can `set()` elements but cannot `add()` or `remove()`. To get a fully mutable list: `new ArrayList<>(Arrays.asList(...))`.
