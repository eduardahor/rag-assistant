package dev.ragassistant.embedding;

import java.util.List;

public interface EmbeddingClient {

    /** Retorna um vetor por texto, na mesma ordem da entrada. */
    List<float[]> embed(List<String> texts);

    default float[] embedOne(String text) {
        return embed(List.of(text)).get(0);
    }
}
