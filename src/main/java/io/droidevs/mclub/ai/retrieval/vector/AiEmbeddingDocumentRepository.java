package io.droidevs.mclub.ai.retrieval.vector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiEmbeddingDocumentRepository extends JpaRepository<AiEmbeddingDocument, UUID> {
}

