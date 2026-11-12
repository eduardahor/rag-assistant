package dev.ragassistant.rag;

import java.util.UUID;

/** Trecho usado como contexto; {@code ref} corresponde ao [n] citado na resposta. */
public record Source(int ref, UUID documentId, String filename, int chunkIndex, double score) {}
