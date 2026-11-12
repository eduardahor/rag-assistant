package dev.ragassistant.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import dev.ragassistant.config.RagProperties;
import dev.ragassistant.error.ExternalServiceException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente do endpoint {@code POST /chat/completions} (formato OpenAI). */
@Component
public class OpenAiChatClient implements LlmClient {

    private final RestClient client;
    private final String model;
    private final Double temperature;

    public OpenAiChatClient(RestClient.Builder builder, RagProperties props) {
        RagProperties.Llm cfg = props.llm();
        RestClient.Builder configured = builder.baseUrl(cfg.baseUrl());
        if (cfg.apiKey() != null && !cfg.apiKey().isBlank()) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.apiKey());
        }
        this.client = configured.build();
        this.model = cfg.model();
        this.temperature = cfg.temperature();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        Request request = new Request(model, temperature,
                List.of(new Message("system", systemPrompt), new Message("user", userPrompt)));
        Response response;
        try {
            response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Response.class);
        } catch (RestClientException e) {
            throw new ExternalServiceException("Falha ao chamar o LLM: " + e.getMessage(), e);
        }
        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null
                || response.choices().get(0).message().content() == null) {
            throw new ExternalServiceException("Resposta do LLM inválida");
        }
        return response.choices().get(0).message().content().strip();
    }

    public record Message(String role, String content) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(String model, Double temperature, List<Message> messages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(List<Choice> choices) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Choice(Message message) {}
    }
}
