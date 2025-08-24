package org.apache.seatunnel.transform.document;

import org.apache.seatunnel.api.transform.document.DocumentParser;
import org.apache.seatunnel.api.transform.document.Splittable;
import org.apache.seatunnel.transform.exception.TransformDocumentErrorCode;
import org.apache.seatunnel.transform.exception.TransformDocumentException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class MarkdownParser extends DocumentParser {

    public MarkdownParser(Splittable splittable) {
        super(splittable);
    }

    @Override
    protected String extract(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TransformDocumentException(TransformDocumentErrorCode.DOCUMENT_PARSING_ERROR, e);
        }
    }
}
