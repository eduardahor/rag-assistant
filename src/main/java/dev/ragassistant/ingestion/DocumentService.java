package dev.ragassistant.ingestion;

import dev.ragassistant.embedding.EmbeddingClient;
import dev.ragassistant.error.ExternalServiceException;
import dev.ragassistant.error.InvalidDocumentException;
import dev.ragassistant.store.ChunkRepository;
import dev.ragassistant.store.DocumentInfo;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final TextChunker chunker;
    private final EmbeddingClient embeddingClient;
    private final ChunkRepository repository;

    public DocumentService(TextChunker chunker, EmbeddingClient embeddingClient, ChunkRepository repository) {
        this.chunker = chunker;
        this.embeddingClient = embeddingClient;
        this.repository = repository;
    }

    /**
     * Gera os embeddings antes de abrir a transação no banco, para não segurar
     * uma conexão do pool durante chamadas de rede.
     */
    public IngestionResult ingest(String filename, String content) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidDocumentException("O nome do arquivo é obrigatório.");
        }
        List<String> chunks = chunker.chunk(content);
        if (chunks.isEmpty()) {
            throw new InvalidDocumentException("O documento está vazio.");
        }
        List<float[]> embeddings = embeddingClient.embed(chunks);
        if (embeddings.size() != chunks.size()) {
            throw new ExternalServiceException("O provedor retornou um número inesperado de embeddings");
        }
        UUID id = repository.saveDocument(filename, chunks, embeddings);
        log.info("Documento '{}' indexado: {} chunks", filename, chunks.size());
        return new IngestionResult(id, filename, chunks.size());
    }

    public List<DocumentInfo> list() {
        return repository.listDocuments();
    }

    public boolean delete(UUID id) {
        return repository.deleteDocument(id);
    }
}
