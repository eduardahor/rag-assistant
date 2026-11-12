package dev.ragassistant.ingestion;

import dev.ragassistant.config.RagProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Divide texto em janelas de até {@code maxChars} caracteres com sobreposição.
 * Prefere cortar em fim de parágrafo, depois quebra de linha, fim de frase e espaço.
 */
@Component
public class TextChunker {

    private static final List<String> SEPARATORS = List.of("\n\n", "\n", ". ", " ");

    private final int maxChars;
    private final int overlapChars;

    @Autowired
    public TextChunker(RagProperties props) {
        this(props.chunking().maxChars(), props.chunking().overlapChars());
    }

    public TextChunker(int maxChars, int overlapChars) {
        if (maxChars <= 0) {
            throw new IllegalArgumentException("maxChars deve ser positivo");
        }
        if (overlapChars < 0 || overlapChars >= maxChars / 2) {
            throw new IllegalArgumentException("overlapChars deve estar em [0, maxChars/2)");
        }
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
    }

    public List<String> chunk(String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").strip();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxChars, normalized.length());
            if (end < normalized.length()) {
                end = findBreak(normalized, start, end);
            }
            String piece = normalized.substring(start, end).strip();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= normalized.length()) {
                break;
            }
            // overlapChars < maxChars/2 e end - start >= maxChars/2, então start sempre avança
            start = Math.max(end - overlapChars, start + 1);
        }
        return chunks;
    }

    private int findBreak(String text, int start, int end) {
        int minEnd = start + maxChars / 2;
        for (String separator : SEPARATORS) {
            int idx = text.lastIndexOf(separator, end - separator.length());
            if (idx >= minEnd) {
                return idx + separator.length();
            }
        }
        return end;
    }
}
