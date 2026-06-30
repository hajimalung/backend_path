# Week 2 — Day 5: Maven — Build Tool Fundamentals

> Study time: ~1 hour | Test time: ≤30 minutes

---

## Learning Content

### 1. What is Maven and Why Use It? (10 min)

Maven is a **build automation and dependency management** tool for Java. It:
- Downloads and manages library dependencies (like npm/pip for Java)
- Defines a standard project structure
- Automates compilation, testing, and packaging

**Setup verification:**
```bash
mvn --version
# Apache Maven 3.9.x
# Java version: 21
```

**Standard Maven project structure:**
```
my-project/
├── pom.xml                        ← Project Object Model — the main config file
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/       ← your source code
    │   └── resources/
    │       └── application.yml    ← config files
    └── test/
        ├── java/
        │   └── com/example/       ← test code (mirrors main)
        └── resources/
```

---

### 2. The pom.xml File (20 min)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Project coordinates — uniquely identify your project -->
    <groupId>com.example</groupId>        <!-- company/org reverse domain -->
    <artifactId>task-manager</artifactId> <!-- project name -->
    <version>1.0.0-SNAPSHOT</version>     <!-- SNAPSHOT = in development -->
    <packaging>jar</packaging>            <!-- jar, war, pom -->

    <name>Task Manager</name>
    <description>CLI Task Manager project</description>

    <!-- Java version -->
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <!-- Dependencies — libraries your project uses -->
    <dependencies>

        <!-- JUnit 5 for testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.1</version>
            <scope>test</scope>           <!-- only available in test code -->
        </dependency>

        <!-- Lombok — reduces boilerplate (getters, setters, etc.) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.30</version>
            <scope>provided</scope>       <!-- compile-time only, not in final jar -->
        </dependency>

        <!-- Jackson — JSON serialization -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.16.0</version>
            <!-- default scope = compile — available everywhere -->
        </dependency>

    </dependencies>

    <!-- Build plugins -->
    <build>
        <plugins>
            <!-- Maven Surefire — runs tests -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.2</version>
            </plugin>
        </plugins>
    </build>

</project>
```

**Dependency scopes:**

| Scope | Available at | Included in JAR |
|-------|-------------|-----------------|
| `compile` (default) | compile + runtime + test | Yes |
| `test` | test only | No |
| `provided` | compile + test only | No (provided by runtime env) |
| `runtime` | runtime + test only | Yes |

---

### 3. Maven Lifecycle and Commands (15 min)

Maven's default lifecycle has phases that run in order:

```
validate → compile → test → package → verify → install → deploy
```

Each phase runs all previous phases first:

```bash
# Clean up target/ directory
mvn clean

# Compile source code
mvn compile

# Run tests
mvn test

# Compile + test + create jar in target/
mvn package

# Like package, but also installs to local ~/.m2/repository
mvn install

# Clean then package (most common)
mvn clean package

# Skip tests (faster — use sparingly)
mvn clean package -DskipTests

# Run a specific test class
mvn test -Dtest=MyServiceTest

# Show dependency tree
mvn dependency:tree

# Download all dependencies
mvn dependency:resolve
```

**Running your app after packaging:**
```bash
mvn package
java -jar target/task-manager-1.0.0-SNAPSHOT.jar
```

---

### 4. Finding Dependencies on Maven Central (10 min)

Go to https://mvnrepository.com — search for any library and copy the XML snippet.

**How dependencies are stored locally:**
```
~/.m2/repository/
  com/fasterxml/jackson/core/jackson-databind/2.16.0/
    jackson-databind-2.16.0.jar
    jackson-databind-2.16.0.pom
```

Maven downloads once and caches locally. Delete `~/.m2` to force re-download.

**Dependency version management — properties block:**
```xml
<properties>
    <jackson.version>2.16.0</jackson.version>
</properties>

<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>${jackson.version}</version>  <!-- reference property -->
</dependency>
```

---

### 5. Create a Maven Project from Scratch (5 min)

```bash
# Generate from archetype (template)
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=task-manager \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DarchetypeVersion=1.4 \
  -DinteractiveMode=false

cd task-manager
mvn compile
mvn test
```

Or in IntelliJ IDEA: `File → New → Project → Maven Archetype`.

---

## Today's Test

> Maximum 30 minutes. Attempt all questions before checking answers.

### Part A — Theory (10 min)

**Q1.** What are the three Maven coordinates that uniquely identify a dependency? Give an example.

**Q2.** What is the difference between `mvn package` and `mvn install`?

**Q3.** What does the `test` scope mean for a dependency? Give a concrete example of when you'd use it.

**Q4.** Where does Maven store downloaded dependencies on your local machine?

**Q5.** What is a `SNAPSHOT` version? When would you use it vs a release version like `1.0.0`?

---

### Part B — Practical Task (20 min)

Do this hands-on:

1. Create a new Maven project (via IntelliJ or `mvn archetype:generate`)

2. Add these dependencies to `pom.xml`:
   - JUnit 5 (`junit-jupiter` version 5.10.1, scope `test`)
   - Jackson databind (`jackson-databind` version 2.16.0)

3. Create a class `JsonUtils` with a static method `String toJson(Object obj)` that uses Jackson's `ObjectMapper` to serialize an object to JSON. Handle `JsonProcessingException` by throwing a `RuntimeException`.

4. Create a test class `JsonUtilsTest` that:
   - Creates a `record Person(String name, int age) {}`
   - Tests that `toJson(new Person("Alice", 30))` returns a String containing `"Alice"`

5. Run `mvn test` and confirm it passes.

---

### Answers

**A1.** `groupId` (organization, e.g. `com.fasterxml.jackson.core`), `artifactId` (library name, e.g. `jackson-databind`), and `version` (e.g. `2.16.0`). Together they're called GAV coordinates and uniquely identify any artifact.

**A2.** `mvn package` compiles, tests, and creates the JAR in the `target/` folder. `mvn install` does everything `package` does, then **copies the JAR to your local Maven repository** (`~/.m2/`), making it available as a dependency to other local projects.

**A3.** A `test`-scoped dependency is available only in the `src/test/java` directory and is not included in the final JAR. Use it for testing libraries like JUnit, Mockito, or AssertJ that you don't need at runtime.

**A4.** `~/.m2/repository/` — organized by groupId/artifactId/version. For example, Jackson 2.16.0 would be at `~/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.16.0/`.

**A5.** `SNAPSHOT` (e.g. `1.0.0-SNAPSHOT`) means the project is **in active development** — the artifact may change between downloads. Maven always re-checks for updates. A release version (e.g. `1.0.0`) is immutable — once published, it should never change. Use SNAPSHOT during development; release when publishing to others.

**Part B Solution — JsonUtils.java:**
```java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}
```

**JsonUtilsTest.java:**
```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {
    record Person(String name, int age) {}

    @Test
    void testToJson() {
        String json = JsonUtils.toJson(new Person("Alice", 30));
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("30"));
    }
}
```
