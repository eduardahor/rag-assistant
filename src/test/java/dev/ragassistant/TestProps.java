package dev.ragassistant;

import dev.ragassistant.config.RagProperties;

public final class TestProps {

    private TestProps() {}

    public static RagProperties defaults() {
        return withEmbedding(2, 64);
    }

    public static RagProperties withEmbedding(int dimensions, int batchSize) {
        return new RagProperties(
                new RagProperties.Llm("https://api.test/v1", "test-key", "test-model", 0.1),
                new RagProperties.Embedding("https://api.test/v1", "test-key", "test-embedding", dimensions, batchSize),
                new RagProperties.Chunking(1000, 150),
                new RagProperties.Retrieval(5, 0.2));
    }
}
