package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    long countByCommentId(UUID commentId);

    Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);

    @Query("""
        SELECT c.id, COUNT(l)
        FROM Comment c
        LEFT JOIN CommentLike l ON l.comment.id = c.id
        WHERE c.id IN :commentIds
        GROUP BY c.id
    """)
    List<Object[]> countLikesBulk(@Param("commentIds") List<UUID> ids);

    @Query("""
        SELECT l.comment.id
        FROM CommentLike l
        WHERE l.user.id = :userId AND l.comment.id IN :commentIds
    """)
    Set<UUID> findLikedCommentIds(@Param("userId") UUID userId,
                                   @Param("commentIds") Set<UUID> ids);
}

