package org.apache.seatunnel.api.transform.document;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ChunkedDocument {

    private final Map<String, Object> metadata;
    private final List<String> chunks;

    public ChunkedDocument(List<String> chunks) {
        this.metadata = new HashMap<>();
        this.chunks = chunks;
    }
}
