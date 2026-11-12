package dev.ragassistant.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ragassistant.error.ExternalServiceException;
import dev.ragassistant.rag.Answer;
import dev.ragassistant.rag.RagService;
import dev.ragassistant.rag.Source;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RagController.class)
class RagControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean RagService ragService;

    @Test
    void returnsAnswerWithSources() throws Exception {
        when(ragService.answer("Como autenticar?")).thenReturn(new Answer(
                "Use bearer token [1].", List.of(new Source(1, UUID.randomUUID(), "auth.md", 0, 0.91))));

        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Como autenticar?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Use bearer token [1]."))
                .andExpect(jsonPath("$.sources[0].ref").value(1))
                .andExpect(jsonPath("$.sources[0].filename").value("auth.md"));
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ragService);
    }

    @Test
    void rejectsTooLongQuestion() throws Exception {
        String body = "{\"question\":\"" + "a".repeat(2001) + "\"}";

        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsProviderFailureToBadGatewayWithoutLeakingDetails() throws Exception {
        when(ragService.answer("falha")).thenThrow(new ExternalServiceException("401 chave sk-secreta inválida"));

        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"falha\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Falha ao comunicar com o provedor de IA."));
    }
}
