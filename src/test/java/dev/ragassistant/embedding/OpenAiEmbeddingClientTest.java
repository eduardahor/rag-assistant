package dev.ragassistant.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.ragassistant.TestProps;
import dev.ragassistant.error.ExternalServiceException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiEmbeddingClientTest {

    private static final String URL = "https://api.test/v1/embeddings";

    @Test
    void sendsModelAndAuthHeaderAndReordersResultsByIndex() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(builder, TestProps.withEmbedding(2, 64));

        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("test-embedding"))
                .andExpect(jsonPath("$.input[1]").value("segundo"))
                .andRespond(withSuccess("""
                        {"object":"list","data":[
                          {"object":"embedding","index":1,"embedding":[0.0,1.0]},
                          {"object":"embedding","index":0,"embedding":[1.0,0.0]}],
                         "usage":{"prompt_tokens":4}}""", MediaType.APPLICATION_JSON));

        List<float[]> result = client.embed(List.of("primeiro", "segundo"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly(1f, 0f);
        assertThat(result.get(1)).containsExactly(0f, 1f);
        server.verify();
    }

    @Test
    void splitsInputIntoBatches() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(builder, TestProps.withEmbedding(2, 2));

        server.expect(requestTo(URL)).andExpect(jsonPath("$.input", hasSize(2)))
                .andRespond(withSuccess("""
                        {"data":[{"index":0,"embedding":[1,0]},{"index":1,"embedding":[1,0]}]}""",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andExpect(jsonPath("$.input", hasSize(1)))
                .andRespond(withSuccess("""
                        {"data":[{"index":0,"embedding":[0,1]}]}""", MediaType.APPLICATION_JSON));

        List<float[]> result = client.embed(List.of("a", "b", "c"));

        assertThat(result).hasSize(3);
        assertThat(result.get(2)).containsExactly(0f, 1f);
        server.verify();
    }

    @Test
    void wrapsHttpErrorsInExternalServiceException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(builder, TestProps.defaults());

        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.embed(List.of("x"))).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void failsWhenEmbeddingDimensionDiffersFromConfiguration() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(builder, TestProps.withEmbedding(3, 64));

        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"data":[{"index":0,"embedding":[1,0]}]}""", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed(List.of("x")))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("dimensões");
    }
}
