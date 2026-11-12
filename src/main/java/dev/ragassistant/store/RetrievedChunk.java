package dev.ragassistant.store;

import java.util.UUID;

public record RetrievedChunk(UUID chunkId, UUID documentId, String filename, int chunkIndex, String content,
                             double score) {}
