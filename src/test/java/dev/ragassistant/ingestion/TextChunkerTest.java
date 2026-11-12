package dev.ragassistant.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    @Test
    void blankOrNullTextProducesNoChunks() {
        TextChunker chunker = new TextChunker(100, 20);

        assertThat(chunker.chunk(null)).isEmpty();
        assertThat(chunker.chunk("")).isEmpty();
        assertThat(chunker.chunk("  \n\n  ")).isEmpty();
    }

    @Test
    void shortTextBecomesSingleChunk() {
        assertThat(new TextChunker(100, 20).chunk("  Olá, mundo.  ")).containsExactly("Olá, mundo.");
    }

    @Test
    void longTextIsSplitAndNoChunkExceedsMaxChars() {
        String text = IntStream.range(0, 300).mapToObj(i -> "palavra" + i).collect(Collectors.joining(" "));

        List<String> chunks = new TextChunker(100, 20).chunk(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(100));
    }

    @Test
    void consecutiveChunksOverlap() {
        String text = IntStream.range(0, 200).mapToObj(i -> "token" + i).collect(Collectors.joining(" "));

        List<String> chunks = new TextChunker(100, 30).chunk(text);

        for (int i = 0; i < chunks.size() - 1; i++) {
            String head = chunks.get(i + 1).substring(0, 8);
            assertThat(chunks.get(i)).contains(head);
        }
    }

    @Test
    void prefersParagraphBoundaries() {
        String text = "A".repeat(60) + "\n\n" + "B".repeat(60);

        List<String> chunks = new TextChunker(100, 10).chunk(text);

        assertThat(chunks.get(0)).isEqualTo("A".repeat(60));
    }

    @Test
    void textWithoutSeparatorsStillTerminates() {
        List<String> chunks = new TextChunker(100, 20).chunk("x".repeat(10_000));

        assertThat(chunks).hasSizeGreaterThan(100);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(100));
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new TextChunker(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TextChunker(100, 50)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TextChunker(100, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
