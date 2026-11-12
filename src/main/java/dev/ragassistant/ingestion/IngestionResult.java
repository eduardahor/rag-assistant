package dev.ragassistant.ingestion;

import java.util.UUID;

public record IngestionResult(UUID documentId, String filename, int chunks) {}
