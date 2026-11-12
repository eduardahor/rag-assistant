package dev.ragassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagProperties(Llm llm, Embedding embedding, Chunking chunking, Retrieval retrieval) {

    /** temperature é opcional: alguns modelos só aceitam o valor padrão. Nulo = parâmetro omitido. */
    public record Llm(String baseUrl, String apiKey, String model, Double temperature) {}

    public record Embedding(String baseUrl, String apiKey, String model, int dimensions, int batchSize) {}

    public record Chunking(int maxChars, int overlapChars) {}

    public record Retrieval(int topK, double minScore) {}
}
