package org.apache.seatunnel.api.transform.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTest {

    @TempDir
    private Path tempDir;

    private final DocumentParser parserWithoutSplittable = new DocumentParser(null) {
        @Override
        protected String extract(File file) {
            return "This is a test document. It has multiple sentences.";
        }
    };

    private DocumentParser parser;

    @BeforeEach
    void beforeEach() {
        parser = new DocumentParser((text, chunkSize, overlap) -> Collections.emptyList()) {
            @Override
            protected String extract(File file) {
                return "This is a test document. It has multiple sentences.";
            }
        };
    }

    @Test
    void testNullFileThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parserWithoutSplittable.parse(null, 10, 0));

        assertEquals("File must not be null", ex.getMessage());
    }

    @Test
    void testNonexistentFileThrowsIllegalArgumentException() {
        File file = new File(tempDir.toFile(), "nonexistent.txt");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parserWithoutSplittable.parse(file, 10, 0));

        assertTrue(ex.getMessage().contains("File does not exist"));
    }

    @Test
    void testDirectoryInsteadOfFileThrowsIllegalArgumentException() {
        File directory = tempDir.toFile();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parserWithoutSplittable.parse(directory, 10, 0));

        assertTrue(ex.getMessage().contains("Not a valid file"));
    }

    @Test
    void testSplittableIsNullReturnsRawAsSingleChunk() throws IOException {
        File file = new File(tempDir.toFile(), "sample.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("This is a test document. It has multiple sentences.");
        }

        ChunkedDocument result = parserWithoutSplittable.parse(file, 10, 0);

        assertEquals(1, result.getChunks().size());
        assertEquals("This is a test document. It has multiple sentences.", result.getChunks().get(0));
    }

    @Test
    void testChunkSizeLessThanOneThrowsIllegalArgumentException() throws IOException {
        File file = new File(tempDir.toFile(), "sample.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("This is a test document. It has multiple sentences.");
        }

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parser.parse(file, 0, 0));

        assertEquals("chunkSize must be greater than 0", ex.getMessage());
    }
}