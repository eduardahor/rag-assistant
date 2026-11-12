package dev.ragassistant.store;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Acesso ao PostgreSQL + pgvector via JDBC puro (sem ORM, para a query vetorial ficar explícita). */
@Repository
public class ChunkRepository {

    private static final String INSERT_CHUNK =
            "INSERT INTO chunks (id, document_id, chunk_index, content, embedding) VALUES (?, ?, ?, ?, ?::vector)";

    // <=> é a distância de cosseno; score = 1 - distância (1.0 = idêntico)
    private static final String SEARCH = """
            SELECT c.id, c.document_id, d.filename, c.chunk_index, c.content,
                   1 - (c.embedding <=> ?::vector) AS score
            FROM chunks c
            JOIN documents d ON d.id = c.document_id
            ORDER BY c.embedding <=> ?::vector
            LIMIT ?
            """;

    private static final String LIST = """
            SELECT d.id, d.filename, d.created_at, COUNT(c.id) AS chunks
            FROM documents d
            LEFT JOIN chunks c ON c.document_id = d.id
            GROUP BY d.id, d.filename, d.created_at
            ORDER BY d.created_at DESC
            """;

    private final JdbcTemplate jdbc;

    public ChunkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Salva (ou substitui, se o nome já existir) um documento e seus chunks numa única transação. */
    @Transactional
    public UUID saveDocument(String filename, List<String> contents, List<float[]> embeddings) {
        if (contents.size() != embeddings.size()) {
            throw new IllegalArgumentException("Número de chunks e de embeddings diferente");
        }
        jdbc.update("DELETE FROM documents WHERE filename = ?", filename);

        UUID documentId = UUID.randomUUID();
        jdbc.update("INSERT INTO documents (id, filename) VALUES (?, ?)", documentId, filename);

        List<Object[]> batch = new ArrayList<>(contents.size());
        for (int i = 0; i < contents.size(); i++) {
            batch.add(new Object[] {UUID.randomUUID(), documentId, i, contents.get(i), toVectorLiteral(embeddings.get(i))});
        }
        jdbc.batchUpdate(INSERT_CHUNK, batch);
        return documentId;
    }

    public List<RetrievedChunk> searchSimilar(float[] queryEmbedding, int topK) {
        String vector = toVectorLiteral(queryEmbedding);
        return jdbc.query(SEARCH, (rs, rowNum) -> new RetrievedChunk(
                rs.getObject("id", UUID.class),
                rs.getObject("document_id", UUID.class),
                rs.getString("filename"),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getDouble("score")), vector, vector, topK);
    }

    public List<DocumentInfo> listDocuments() {
        return jdbc.query(LIST, (rs, rowNum) -> new DocumentInfo(
                rs.getObject("id", UUID.class),
                rs.getString("filename"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getInt("chunks")));
    }

    public boolean deleteDocument(UUID id) {
        return jdbc.update("DELETE FROM documents WHERE id = ?", id) > 0;
    }

    static String toVectorLiteral(float[] values) {
        StringBuilder sb = new StringBuilder(values.length * 10).append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(values[i]);
        }
        return sb.append(']').toString();
    }
}
