package dev.ragassistant.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ragassistant.embedding.EmbeddingClient;
import dev.ragassistant.ingestion.DocumentService;
import dev.ragassistant.ingestion.IngestionResult;
import dev.ragassistant.llm.LlmClient;
import dev.ragassistant.rag.Answer;
import dev.ragassistant.rag.RagService;
import dev.ragassistant.store.DocumentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Pipeline completo contra um PostgreSQL + pgvector real (Testcontainers; requer Docker).
 * Embeddings são determinísticos e o LLM é mockado: valida migration, busca vetorial e montagem do prompt.
 */
@SpringBootTest(properties = "rag.retrieval.min-score=0.0")
@Testcontainers
class RagPipelineIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @TestConfiguration
    static class FakeEmbeddingConfig {
        @Bean
        @Primary
        EmbeddingClient hashingEmbeddingClient() {
            return new HashingEmbeddingClient();
        }
    }

    @MockitoBean LlmClient llm;

    @Autowired DocumentService documentService;
    @Autowired RagService ragService;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM documents");
    }

    @Test
    void retrievesTheMostRelevantDocumentAndPassesItToTheLlm() {
        documentService.ingest("auth.md",
                "Authentication uses bearer tokens. Send the Authorization header with a JWT token.");
        documentService.ingest("limits.md",
                "Rate limiting returns HTTP 429 when you exceed 100 requests per minute.");
        when(llm.complete(anyString(), anyString())).thenReturn("Use um bearer token [1].");

        Answer answer = ragService.answer("How does bearer token authentication work?");

        assertThat(answer.answer()).isEqualTo("Use um bearer token [1].");
        assertThat(answer.sources()).isNotEmpty();
        assertThat(answer.sources().get(0).filename()).isEqualTo("auth.md");

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(llm).complete(anyString(), userPrompt.capture());
        assertThat(userPrompt.getValue()).contains("bearer tokens").contains("Question: How does bearer");
    }

    @Test
    void reuploadingSameFilenameReplacesPreviousVersion() {
        documentService.ingest("api.md", "Versão antiga da documentação.");
        IngestionResult second = documentService.ingest("api.md", "Versão nova da documentação.");

        assertThat(documentService.list()).extracting(DocumentInfo::id).containsExactly(second.documentId());
    }

    @Test
    void deletingDocumentRemovesItsChunks() {
        IngestionResult result = documentService.ingest("tmp.md", "Conteúdo temporário.");

        assertThat(documentService.delete(result.documentId())).isTrue();

        Integer remaining = jdbc.queryForObject("SELECT COUNT(*) FROM chunks", Integer.class);
        assertThat(remaining).isZero();
    }
}
