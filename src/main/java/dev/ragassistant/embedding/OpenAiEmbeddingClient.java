package dev.ragassistant.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.ragassistant.config.RagProperties;
import dev.ragassistant.error.ExternalServiceException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente do endpoint {@code POST /embeddings} (formato OpenAI, compatível com Ollama, vLLM etc.). */
@Component
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private final RestClient client;
    private final String model;
    private final int dimensions;
    private final int batchSize;

    public OpenAiEmbeddingClient(RestClient.Builder builder, RagProperties props) {
        RagProperties.Embedding cfg = props.embedding();
        RestClient.Builder configured = builder.baseUrl(cfg.baseUrl());
        if (cfg.apiKey() != null && !cfg.apiKey().isBlank()) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.apiKey());
        }
        this.client = configured.build();
        this.model = cfg.model();
        this.dimensions = cfg.dimensions();
        this.batchSize = Math.max(1, cfg.batchSize());
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += batchSize) {
            result.addAll(embedBatch(texts.subList(i, Math.min(i + batchSize, texts.size()))));
        }
        return result;
    }

    private List<float[]> embedBatch(List<String> batch) {
        Response response;
        try {
            response = client.post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Request(model, batch))
                    .retrieve()
                    .body(Response.class);
        } catch (RestClientException e) {
            throw new ExternalServiceException("Falha ao gerar embeddings: " + e.getMessage(), e);
        }
        if (response == null || response.data() == null || response.data().size() != batch.size()) {
            throw new ExternalServiceException("Resposta de embeddings inválida");
        }
        List<float[]> embeddings = response.data().stream()
                .sorted(Comparator.comparingInt(Response.Item::index))
                .map(Response.Item::embedding)
                .toList();
        embeddings.forEach(this::checkDimensions);
        return embeddings;
    }

    private void checkDimensions(float[] embedding) {
        if (embedding == null || embedding.length != dimensions) {
            int actual = embedding == null ? 0 : embedding.length;
            throw new ExternalServiceException("Embedding com " + actual + " dimensões, esperado " + dimensions
                    + ". Ajuste rag.embedding.dimensions e a coluna vector(...) na migration.");
        }
    }

    public record Request(String model, List<String> input) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(List<Item> data) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Item(int index, float[] embedding) {}
    }
}
