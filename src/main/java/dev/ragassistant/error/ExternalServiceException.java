package dev.ragassistant.error;

/** Falha ao chamar o provedor de embeddings ou de LLM. */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
