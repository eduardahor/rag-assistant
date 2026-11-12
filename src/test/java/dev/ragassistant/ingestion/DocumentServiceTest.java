package dev.ragassistant.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.ragassistant.embedding.EmbeddingClient;
import dev.ragassistant.error.ExternalServiceException;
import dev.ragassistant.error.InvalidDocumentException;
import dev.ragassistant.store.ChunkRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock EmbeddingClient embeddingClient;
    @Mock ChunkRepository repository;

    private DocumentService service() {
        return new DocumentService(new TextChunker(100, 20), embeddingClient, repository);
    }

    @Test
    void embedsChunksAndPersistsThem() {
        UUID id = UUID.randomUUID();
        List<float[]> embeddings = List.of(new float[] {1f, 0f});
        when(embeddingClient.embed(List.of("Texto curto."))).thenReturn(embeddings);
        when(repository.saveDocument("doc.md", List.of("Texto curto."), embeddings)).thenReturn(id);

        IngestionResult result = service().ingest("doc.md", "Texto curto.");

        assertThat(result).isEqualTo(new IngestionResult(id, "doc.md", 1));
    }

    @Test
    void rejectsEmptyDocumentWithoutCallingProvider() {
        assertThatThrownBy(() -> service().ingest("doc.md", "   "))
                .isInstanceOf(InvalidDocumentException.class);

        verifyNoInteractions(embeddingClient, repository);
    }

    @Test
    void rejectsBlankFilename() {
        assertThatThrownBy(() -> service().ingest(" ", "conteúdo"))
                .isInstanceOf(InvalidDocumentException.class);
    }

    @Test
    void failsWhenProviderReturnsWrongNumberOfEmbeddings() {
        when(embeddingClient.embed(anyList())).thenReturn(List.of());

        assertThatThrownBy(() -> service().ingest("doc.md", "Texto curto."))
                .isInstanceOf(ExternalServiceException.class);
    }
}
