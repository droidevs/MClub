package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.Comment;
import io.droidevs.mclub.domain.CommentTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(CommentTargetType targetType, UUID targetId);

    List<Comment> findByParentIdOrderByCreatedAtAsc(UUID parentId);
}

