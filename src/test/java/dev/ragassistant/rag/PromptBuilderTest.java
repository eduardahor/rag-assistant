package dev.ragassistant.rag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ragassistant.store.RetrievedChunk;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

    @Test
    void numbersExcerptsAndAppendsQuestion() {
        List<RetrievedChunk> chunks = List.of(
                new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), "auth.md", 0, "Use bearer tokens.", 0.9),
                new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), "limits.md", 3, "Limite de 100 req/min.", 0.7));

        String prompt = PromptBuilder.userPrompt("Como autenticar?", chunks);

        assertThat(prompt)
                .contains("[1] (auth.md, chunk 0)", "Use bearer tokens.")
                .contains("[2] (limits.md, chunk 3)", "Limite de 100 req/min.")
                .endsWith("Question: Como autenticar?");
    }

    @Test
    void systemPromptRestrictsAnswersToContext() {
        assertThat(PromptBuilder.SYSTEM_PROMPT).contains("ONLY").contains("untrusted");
    }
}
