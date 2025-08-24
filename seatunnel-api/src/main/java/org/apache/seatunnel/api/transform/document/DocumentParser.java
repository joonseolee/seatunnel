package org.apache.seatunnel.api.transform.document;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class DocumentParser implements DocumentParsable {

    private final Splittable splittable;

    protected DocumentParser(Splittable splittable) {
        this.splittable = splittable;
    }

    @Override
    public final ChunkedDocument parse(String path, int chunkSize, int overlap) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path must not be null or empty");
        }

        File file = new File(path);

        return parse(file, chunkSize, overlap);
    }

    @Override
    public final ChunkedDocument parse(File file, int chunkSize, int overlap) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null");
        }

        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException("Not a valid file: " + file.getAbsolutePath());
        }

        String raw = extract(file);
        if (splittable == null) {
            return new ChunkedDocument(Collections.singletonList(raw));
        }

        if (chunkSize < 1) {
            throw new IllegalArgumentException("chunkSize must be greater than 0");
        }

        List<String> chunks = splittable.split(raw, chunkSize, overlap);

        return new ChunkedDocument(chunks);
    }

    protected abstract String extract(File file);
}
