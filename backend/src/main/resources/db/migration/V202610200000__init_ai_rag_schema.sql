CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE rag_knowledge_documents (
                                         id BIGSERIAL PRIMARY KEY,
                                         title VARCHAR(255) NOT NULL,
                                         category VARCHAR(50) NOT NULL,
                                         source_url VARCHAR(500),
                                         allowed_roles TEXT[] NOT NULL,
                                         version VARCHAR(20) DEFAULT '1.2',
                                         is_active BOOLEAN DEFAULT TRUE,
                                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rag_knowledge_embeddings (
                                          id BIGSERIAL PRIMARY KEY,
                                          document_id BIGINT NOT NULL REFERENCES rag_knowledge_documents(id) ON DELETE CASCADE,
                                          chunk_index INT NOT NULL,
                                          content TEXT NOT NULL,
                                          metadata JSONB NOT NULL,
                                          embedding vector(1536) NOT NULL,
                                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rag_embeddings_hnsw ON rag_knowledge_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_rag_embeddings_metadata ON rag_knowledge_embeddings USING gin (metadata);
CREATE INDEX idx_rag_embeddings_doc_id ON rag_knowledge_embeddings (document_id);