package io.droidevs.mclub.ai.retrieval.vector;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Vector DB access using native SQL against pgvector.
 *
 * <p>Uses cosine distance operator (<=>). Score is derived as (1 - distance).
 */
@Service
@RequiredArgsConstructor
public class VectorDbService {

    private final NamedParameterJdbcTemplate jdbc;

    public List<VectorSearchResult> search(String embeddingLiteral, int topK) {
        // embeddingLiteral must be like: [0.1,0.2,...]
        String sql = """
                select id, doc_type, source_id, content,
                       (1 - (embedding <=> cast(:q as vector))) as score
                from ai_embedding_document
                order by embedding <=> cast(:q as vector)
                limit :k
                """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("q", embeddingLiteral);
        params.addValue("k", topK);

        return jdbc.query(sql, params, (rs, rowNum) -> new VectorSearchResult(
                UUID.fromString(rs.getString("id")),
                rs.getString("doc_type"),
                rs.getString("source_id") == null ? null : UUID.fromString(rs.getString("source_id")),
                rs.getString("content"),
                rs.getDouble("score")
        ));
    }
}

