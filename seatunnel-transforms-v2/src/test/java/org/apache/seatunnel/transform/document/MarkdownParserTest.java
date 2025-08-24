package org.apache.seatunnel.transform.document;

import org.apache.seatunnel.api.transform.document.ChunkedDocument;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;


class MarkdownParserTest {

    @Test
    void testParseMarkdown() throws URISyntaxException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("document/sample-markdown.md");
        File file = new File(resource.toURI());
        MarkdownParser markdownParser = new MarkdownParser(null);

        ChunkedDocument chunkedDocument = markdownParser.parse(file, 100, 0);

        assertEquals(1, chunkedDocument.getChunks().size());
    }
}