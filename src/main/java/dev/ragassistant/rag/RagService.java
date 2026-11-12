package dev.ragassistant.rag;

import dev.ragassistant.config.RagProperties;
import dev.ragassistant.embedding.EmbeddingClient;
import dev.ragassistant.llm.LlmClient;
import dev.ragassistant.store.ChunkRepository;
import dev.ragassistant.store.RetrievedChunk;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    static final String NO_CONTEXT_ANSWER =
            "Não encontrei informações suficientes nos documentos para responder a essa pergunta.";

    private final EmbeddingClient embeddingClient;
    private final ChunkRepository repository;
    private final LlmClient llm;
    private final RagProperties.Retrieval retrieval;

    public RagService(EmbeddingClient embeddingClient, ChunkRepository repository, LlmClient llm,
                      RagProperties props) {
        this.embeddingClient = embeddingClient;
        this.repository = repository;
        this.llm = llm;
        this.retrieval = props.retrieval();
    }

    public Answer answer(String question) {
        float[] queryEmbedding = embeddingClient.embedOne(question);

        List<RetrievedChunk> relevant = repository.searchSimilar(queryEmbedding, retrieval.topK()).stream()
                .filter(chunk -> chunk.score() >= retrieval.minScore())
                .toList();

        // Sem contexto relevante: não chama o LLM (evita custo e alucinação)
        if (relevant.isEmpty()) {
            return new Answer(NO_CONTEXT_ANSWER, List.of());
        }

        String text = llm.complete(PromptBuilder.SYSTEM_PROMPT, PromptBuilder.userPrompt(question, relevant));

        List<Source> sources = IntStream.range(0, relevant.size())
                .mapToObj(i -> {
                    RetrievedChunk chunk = relevant.get(i);
                    return new Source(i + 1, chunk.documentId(), chunk.filename(), chunk.chunkIndex(), chunk.score());
                })
                .toList();
        return new Answer(text, sources);
    }
}
