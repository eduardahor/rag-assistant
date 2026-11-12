package dev.ragassistant.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.ragassistant.TestProps;
import dev.ragassistant.error.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiChatClientTest {

    private static final String URL = "https://api.test/v1/chat/completions";

    @Test
    void sendsSystemAndUserMessagesAndReturnsTrimmedContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiChatClient client = new OpenAiChatClient(builder, TestProps.defaults());

        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("regras"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("pergunta"))
                .andRespond(withSuccess("""
                        {"id":"x","choices":[{"index":0,"finish_reason":"stop",
                          "message":{"role":"assistant","content":"  resposta [1]\\n"}}]}""",
                        MediaType.APPLICATION_JSON));

        assertThat(client.complete("regras", "pergunta")).isEqualTo("resposta [1]");
        server.verify();
    }

    @Test
    void failsOnEmptyChoices() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiChatClient client = new OpenAiChatClient(builder, TestProps.defaults());

        server.expect(requestTo(URL)).andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete("s", "u")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void wrapsHttpErrorsInExternalServiceException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiChatClient client = new OpenAiChatClient(builder, TestProps.defaults());

        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.complete("s", "u")).isInstanceOf(ExternalServiceException.class);
    }
}
