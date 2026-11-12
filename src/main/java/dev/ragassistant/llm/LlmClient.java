package dev.ragassistant.llm;

public interface LlmClient {

    String complete(String systemPrompt, String userPrompt);
}
