package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.Comment;
import io.droidevs.mclub.domain.CommentTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("""
    SELECT COUNT(c) FROM Comment c
    WHERE c.targetType = :targetType
    AND c.targetId = :targetId
""")
    long countAllByTarget(CommentTargetType targetType, UUID targetId);


    @Query("""
    SELECT COUNT(c) FROM Comment c
    WHERE c.targetType = :targetType
    AND c.targetId = :targetId
    AND c.parentId IS NOT NULL
""")
    long countRepliesByTarget(CommentTargetType targetType, UUID targetId);


    @Query("""
    SELECT c FROM Comment c
    JOIN FETCH c.author
    WHERE c.targetType = :targetType
    AND c.targetId = :targetId
    ORDER BY c.createdAt DESC
""")
    Page<Comment> findLatestComments(CommentTargetType targetType, UUID targetId, Pageable pageable);


    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.author
        WHERE c.parentId IN :parentIds
        ORDER BY c.createdAt ASC
    """)
    List<Comment> findRepliesByParentIds(List<UUID> parentIds);

    @Query("""
        SELECT c FROM Comment c
        WHERE c.parentId = :parentId
        ORDER BY c.createdAt ASC
    """)
    @EntityGraph(attributePaths = "author")
    Page<Comment> findReplies(@Param("parentId") UUID parentId, Pageable pageable);

    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.author
        WHERE c.parentId = :parentId
        ORDER BY c.createdAt ASC
        LIMIT :limit
    """)
    List<Comment> findRepliesLimit(@Param("parentId") UUID parentId, @Param("limit") int limit);

    // Fetch author eagerly for UI DTO mapping (prevents LazyInitializationException)
    @Query("select c from Comment c join fetch c.author where c.targetType = :targetType and c.targetId = :targetId order by c.createdAt asc")
    List<Comment> findThreadWithAuthor(@Param("targetType") CommentTargetType targetType, @Param("targetId") UUID targetId);


    @Query("""
        SELECT COUNT(c) FROM Comment c
        WHERE c.parentId = :parentId
    """)
    int countCommentByParentId(@Param("parentId") UUID id);
}
