package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.Comment;
import io.droidevs.mclub.domain.CommentTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // Fetch author eagerly for UI DTO mapping (prevents LazyInitializationException)
    @Query("select c from Comment c join fetch c.author where c.targetType = :targetType and c.targetId = :targetId order by c.createdAt asc")
    List<Comment> findThreadWithAuthor(@Param("targetType") CommentTargetType targetType, @Param("targetId") UUID targetId);

    /** Fetch direct replies (children) for a given parent comment with author eagerly loaded. */
    @Query("select c from Comment c join fetch c.author where c.parentId = :parentId order by c.createdAt asc")
    List<Comment> findRepliesWithAuthor(@Param("parentId") UUID parentId);

    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(CommentTargetType targetType, UUID targetId);

    List<Comment> findByParentIdOrderByCreatedAtAsc(UUID parentId);
}
