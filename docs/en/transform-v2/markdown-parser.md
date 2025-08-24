# MarkdownParser

> MarkdownParser transform plugin

## Description

The `MarkdownParser` plugin enables Seatunnel to parse and chunk markdown documents for downstream processing, such as Retrieval-Augmented Generation (RAG) pipelines or knowledge retrieval scenarios.  
This plugin reads a markdown file, splits the content into chunks with optional overlap, and makes these chunks available for further data processing or retrieval.

`MarkdownParser` extends the abstract `DocumentParser` class and delegates chunking logic to a pluggable `Splittable` implementation (such as sentence-based or character-based splitting).

---

## Options

|    name      |  type   | required | default | Description                                                                                   |
|--------------|---------|----------|---------|-----------------------------------------------------------------------------------------------|
| path         | string  | yes      |         | Absolute or relative path to the markdown file to parse.                                      |
| chunk_size   | int     | yes      |         | The size of each chunk, measured in the unit determined by the internal splitter.             |
| overlap      | int     | no       | 0       | The overlap size between adjacent chunks, in the same unit as the splitter.                   |

### path [string]
Specifies the absolute or relative path to the markdown file to be parsed.  
Example: `/data/sample-markdown.md`

### chunk_size [int]
The size of each chunk, according to the splitting method used internally.
- For example, if the splitter splits by sentence, this is the number of sentences per chunk.
- If the splitter splits by character, this is the number of characters per chunk.

**Required.**

### overlap [int]
The overlap size between adjacent chunks, in the same unit as the splitter.  
Default: `0` (no overlap).

---

## Seatunnel Pipeline Example (Planned Usage)

> ⚠️ **Note:**  
> The following Seatunnel config example demonstrates the **intended usage** of the `MarkdownParser` plugin in the future.  
> **This configuration will NOT work out-of-the-box with the current implementation.**  
> To enable this, you will need to implement an additional Transform/Adapter class that connects these config options (`path`, `chunk_size`, `overlap`, etc.) to your parser logic.

Suppose you have a markdown file named `sample-markdown.md`:

```markdown
# Title

Some introduction.

| Name  | Age |
|-------|-----|
| Alice | 30  |
| Bob   | 28  |
```

You can use `MarkdownParser` in your Seatunnel config as follows:

```groovy
transform {
    MarkdownParser {
        path = "/data/sample-markdown.md"
        chunk_size = 1000
        overlap = 100
    }
}


```

## How to Use Now (Direct Java Usage)

For now, you can use the parser directly in your Java code as follows:

```java
MarkdownParser parser = new MarkdownParser(new SentenceSplitter());
ChunkedDocument doc = parser.parse("/data/sample-markdown.md", 1000, 100);
```

---

## Implementation Details

- The `MarkdownParser` plugin reads the markdown file and delegates chunking to a configurable `Splittable` implementation.
- Currently, chunking can be done by sentence, character, or other logic depending on the configured `Splittable`.
- For example, with a `SentenceSplitter`, `chunk_size` and `overlap` are measured in sentences. With a `CharacterSplitter`, they are measured in characters.
- Future versions may allow users to select the splitting strategy via configuration.

**Custom Splitting Example:**
```java
public class MarkdownParser extends DocumentParser {
    public MarkdownParser(Splittable splittable) {
        super(splittable);
    }
    @Override
    protected String extract(File file) { ... }
}
public interface Splittable {
    List<String> split(String text, int chunkSize, int overlap);
}
```

---

## Output

The transform outputs a list of document chunks, each chunk containing a segment of the original markdown file based on the splitter logic.  
Downstream components can use these chunks for retrieval, embedding, or further processing.