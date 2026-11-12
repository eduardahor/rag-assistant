package dev.ragassistant.integration;

import dev.ragassistant.embedding.EmbeddingClient;
import java.util.List;

/**
 * Embedding determinístico para testes: bag-of-words com hash em 1536 dimensões, normalizado.
 * Textos que compartilham palavras ficam próximos por similaridade de cosseno, sem chamar API externa.
 */
class HashingEmbeddingClient implements EmbeddingClient {

    static final int DIMENSIONS = 1536;

    @Override
    public List<float[]> embed(List<String> texts) {
        return texts.stream().map(HashingEmbeddingClient::vectorize).toList();
    }

    private static float[] vectorize(String text) {
        float[] vector = new float[DIMENSIONS];
        for (String token : text.toLowerCase().split("\\W+")) {
            if (!token.isBlank()) {
                vector[Math.floorMod(token.hashCode(), DIMENSIONS)] += 1f;
            }
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        if (norm == 0) {
            vector[0] = 1f;
            return vector;
        }
        float scale = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= scale;
        }
        return vector;
    }
}
