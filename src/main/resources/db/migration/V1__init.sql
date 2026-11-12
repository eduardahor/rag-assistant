CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id         UUID PRIMARY KEY,
    filename   TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX documents_filename_idx ON documents (filename);

-- A dimensão (1536) precisa casar com rag.embedding.dimensions
CREATE TABLE chunks (
    id          UUID PRIMARY KEY,
    document_id UUID         NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    chunk_index INT          NOT NULL,
    content     TEXT         NOT NULL,
    embedding   vector(768)  NOT NULL,
    UNIQUE (document_id, chunk_index)
);

CREATE INDEX chunks_embedding_hnsw_idx ON chunks USING hnsw (embedding vector_cosine_ops);
