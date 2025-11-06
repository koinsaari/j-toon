# j-toon

Java implementation of **Token-Oriented Object Notation** (TOON) - a compact, human-readable serialization format optimized for LLM input with significantly reduced token usage.

TOON uses indentation-based structure and tabular formatting to represent data more efficiently than JSON while maintaining readability.

## Quick Example

**JSON (verbose):**

```json
{
  "users": [
    { "id": 1, "name": "Alice", "role": "admin" },
    { "id": 2, "name": "Bob", "role": "user" }
  ]
}
```

**TOON (compact):**

```
users[2]{id,name,role}:
  1,Alice,admin
  2,Bob,user
```

**Token savings: ~40%** (varies by tokenizer)

---

## Installation

### For Library Use

Add to your `build.gradle.kts`:

```gradle
dependencies {
    implementation("io.github.koinsaari:j-toon-core:0.1.0")
}
```

Or Maven `pom.xml`:

```xml
<dependency>
    <groupId>io.github.koinsaari</groupId>
    <artifactId>j-toon-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

### For CLI Tool

Download the fat JAR:

```bash
# From releases page
java -jar j-toon-cli-0.1.0.jar data.json -o output.toon
```

---

## Usage

### As a Library

**Encode Java objects to TOON:**

```java
import io.github.koinsaari.jtoon.Toon;

Map<String, Object> data = new LinkedHashMap<>();
data.put("name", "Ada");
data.put("age", 30);
data.put("tags", Arrays.asList("reading", "coding"));

String toon = Toon.encode(data);
System.out.println(toon);
// Output:
// name: Ada
// age: 30
// tags[2]: reading,coding
```

**Decode TOON back to objects:**

```java
String toon = """
    users[2]{id,name}:
      1,Alice
      2,Bob
    """;

Object result = Toon.decode(toon);
// Returns: Map with "users" key containing List of Maps
```

**With custom options:**

```java
ToonOptions options = new ToonOptions(
    2,                              // indent size
    ToonOptions.Delimiter.PIPE,     // delimiter: , | \t
    true,                           // length marker: [#2]
    true                            // strict mode
);

String toon = Toon.encode(data, options);
Object decoded = Toon.decode(toon, options);
```

### Command Line

**Encode JSON to TOON:**

```bash
java -jar j-toon-cli-0.1.0.jar data.json -o output.toon
```

**Decode TOON to JSON:**

```bash
java -jar j-toon-cli-0.1.0.jar data.toon -o output.json
```

**Using stdin/stdout:**

```bash
cat data.json | java -jar j-toon-cli-0.1.0.jar > output.toon
```

**With options:**

```bash
# Use pipe delimiter
java -jar j-toon-cli-0.1.0.jar data.json --delimiter "|"

# Custom indentation
java -jar j-toon-cli-0.1.0.jar data.json --indent 4

# Show token savings
cat data.json | java -jar j-toon-cli-0.1.0.jar --stats

# Add length markers
java -jar j-toon-cli-0.1.0.jar data.json --length-marker

# Disable strict validation
java -jar j-toon-cli-0.1.0.jar data.toon --no-strict
```

**Help:**

```bash
java -jar j-toon-cli-0.1.0.jar --help
```

---

## Format Overview

### Primitives

```
name: Ada
age: 30
active: true
score: 9.5
empty: null
```

### Arrays

```
# Primitive array (inline)
tags[3]: reading,gaming,coding

# Primitive array (multiline)
tags[3]:
  reading,gaming,coding

# List array
items[2]:
  - first item
  - second item

# Object list
users[2]:
  - id: 1
    name: Alice
  - id: 2
    name: Bob
```

### Tabular Arrays (Optimized for Uniform Data)

```
# Most efficient for consistent object arrays
users[2]{id,name,role}:
  1,Alice,admin
  2,Bob,user
```

### Nested Objects

```
user:
  id: 123
  name: Ada
  contact:
    email: ada@example.com
    phone: "555-1234"
```

---

## Key Features

✅ **Token Efficient** - Typically 30-60% fewer tokens than JSON
✅ **Indentation-Based** - Like YAML, uses whitespace for structure
✅ **Tabular Format** - Optimized for arrays of uniform objects
✅ **Multiple Delimiters** - Comma (default), tab, or pipe
✅ **Quoted Keys & Values** - Handles special characters automatically
✅ **LLM-Friendly** - Includes explicit array lengths and field names
✅ **Strict/Lenient Modes** - Choose between strict validation or lenient parsing

---

## Type Normalization

j-toon normalizes Java-specific types to the JSON data model before encoding:

### Numeric Types

- **NaN, Infinity, -Infinity** → `null`
- **-0** → `0`
- **BigInteger** → number (if within Long range), else quoted decimal string
- **BigDecimal** → canonical decimal form (no exponent, no trailing zeros)

### Temporal Types

- **Date, Instant, LocalDateTime** → ISO 8601 string via Jackson serialization

### Collection Types

- **Set** → array (iteration order)
- **Map** → object (string keys required)

### Non-Serializable Types

- **null, undefined** → `null`
- **Functions, lambdas** → `null` (or excluded from output)

All normalization is handled automatically via Jackson's `ObjectMapper`. Custom types implementing Jackson serialization will be normalized accordingly.

---

## Building from Source

### Requirements

- Java 17+
- Gradle 8.0+

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Build CLI JAR

```bash
./gradlew j-toon-cli:jar
# Output: j-toon-cli/build/libs/j-toon-cli-0.1.0.jar
```

---

## How TOON Works

TOON optimizes for LLM input by:

1. **Reducing Punctuation** - No braces, brackets, or quotes unless necessary
2. **Using Indentation** - Nesting shown by whitespace instead of symbols
3. **Declaring Fields Once** - Tabular format lists keys once, then just values
4. **Including Metadata** - Array lengths `[2]` and field names `{id,name}` for validation

### When to Use TOON

- ✅ Passing data to LLMs as context
- ✅ Large arrays of uniform objects
- ✅ Cost-sensitive token counting
- ✅ Human-readable serialization

### When to Stick with JSON

- ✅ APIs and storage
- ✅ Non-uniform nested data
- ✅ Deep nesting
- ✅ Standard tooling needed

---

## Examples

### LLM Prompt Usage

````java
Map<String, Object> userData = /* ... get user data ... */;
String toon = Toon.encode(userData);

String prompt = "Analyze these users:\n\n```toon\n" + toon + "\n```\n\nFind patterns...";
// Send prompt to LLM with fewer tokens
````

### Data Pipeline

```bash
# Convert entire dataset to TOON for efficient LLM processing
cat large-dataset.json | java -jar j-toon-cli-0.1.0.jar --stats | head -1000 > llm-input.toon
```

### Custom Formatting

```java
ToonOptions tabDelimited = new ToonOptions(
    2,
    ToonOptions.Delimiter.TAB,  // Tab is more token-efficient
    true,                        // Include length markers
    true
);

String toon = Toon.encode(data, tabDelimited);
```

---

## License

MIT - See [LICENSE](LICENSE) file

---

## Contributing

Contributions welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines and setup instructions.

---

## See Also

- [TOON Format Repository](https://github.com/toon-format/toon)
