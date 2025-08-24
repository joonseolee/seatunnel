package org.apache.seatunnel.api.transform.document;

import java.util.List;

public interface Splittable {

    /**
     * Splits the given text into chunks of specified size with an overlap.
     *
     * @param text      the text to be split
     * @param chunkSize the size of each chunk
     * @param overlap   the number of characters to overlap between chunks
     * @return a list of text chunks
     */
    List<String> split(String text, int chunkSize, int overlap);
}
