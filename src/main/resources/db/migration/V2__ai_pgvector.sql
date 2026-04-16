-- Enable pgvector extension (must be installed on the Postgres image)
CREATE EXTENSION IF NOT EXISTS vector;

-- Store embeddings for RAG retrieval
CREATE TABLE IF NOT EXISTS ai_embedding_document (
    id UUID PRIMARY KEY,
    doc_type VARCHAR(32) NOT NULL,
    source_id UUID NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ai_embedding_document_doc_type_idx ON ai_embedding_document(doc_type);
CREATE INDEX IF NOT EXISTS ai_embedding_document_source_id_idx ON ai_embedding_document(source_id);

-- Approx nearest neighbor search index
CREATE INDEX IF NOT EXISTS ai_embedding_document_embedding_hnsw
    ON ai_embedding_document USING hnsw (embedding vector_cosine_ops);

