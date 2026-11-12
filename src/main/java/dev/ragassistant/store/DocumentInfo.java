package dev.ragassistant.store;

import java.time.Instant;
import java.util.UUID;

public record DocumentInfo(UUID id, String filename, Instant createdAt, int chunks) {}
