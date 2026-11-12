package dev.ragassistant.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.ragassistant.TestProps;
import dev.ragassistant.embedding.EmbeddingClient;
import dev.ragassistant.llm.LlmClient;
import dev.ragassistant.store.ChunkRepository;
import dev.ragassistant.store.RetrievedChunk;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    private static final float[] QUERY_VECTOR = {1f, 0f};

    @Mock EmbeddingClient embeddingClient;
    @Mock ChunkRepository repository;
    @Mock LlmClient llm;

    private RagService service;

    @BeforeEach
    void setUp() {
        service = new RagService(embeddingClient, repository, llm, TestProps.defaults());
        when(embeddingClient.embedOne("pergunta")).thenReturn(QUERY_VECTOR);
    }

    private static RetrievedChunk chunk(String filename, int index, double score) {
        return new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), filename, index, "conteúdo de " + filename, score);
    }

    @Test
    void returnsFallbackWithoutCallingLlmWhenNothingIsRetrieved() {
        when(repository.searchSimilar(QUERY_VECTOR, 5)).thenReturn(List.of());

        Answer answer = service.answer("pergunta");

        assertThat(answer.answer()).isEqualTo(RagService.NO_CONTEXT_ANSWER);
        assertThat(answer.sources()).isEmpty();
        verifyNoInteractions(llm);
    }

    @Test
    void ignoresChunksBelowMinScore() {
        when(repository.searchSimilar(QUERY_VECTOR, 5)).thenReturn(List.of(chunk("ruido.md", 0, 0.05)));

        Answer answer = service.answer("pergunta");

        assertThat(answer.sources()).isEmpty();
        verifyNoInteractions(llm);
    }

    @Test
    void sendsRelevantChunksToLlmAndReturnsNumberedSources() {
        RetrievedChunk relevant = chunk("auth.md", 2, 0.85);
        RetrievedChunk noise = chunk("ruido.md", 0, 0.05);
        when(repository.searchSimilar(QUERY_VECTOR, 5)).thenReturn(List.of(relevant, noise));
        when(llm.complete(eq(PromptBuilder.SYSTEM_PROMPT), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("Use um bearer token [1].");

        Answer answer = service.answer("pergunta");

        assertThat(answer.answer()).isEqualTo("Use um bearer token [1].");
        assertThat(answer.sources()).containsExactly(
                new Source(1, relevant.documentId(), "auth.md", 2, 0.85));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(llm).complete(eq(PromptBuilder.SYSTEM_PROMPT), prompt.capture());
        assertThat(prompt.getValue()).contains("conteúdo de auth.md").doesNotContain("ruido.md");
    }
}
