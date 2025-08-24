package org.apache.seatunnel.api.transform.document;

import java.io.File;

public interface DocumentParsable {

    ChunkedDocument parse(String path, int chunkSize, int overlap);

    ChunkedDocument parse(File file, int chunkSize, int overlap);
}
