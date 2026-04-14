package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.dto.CommentDto;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    private final EventRepository eventRepository;
    private final ActivityRepository activityRepository;

    @Transactional(readOnly = true)
    public List<CommentDto> getThread(CommentTargetType targetType, UUID targetId, String currentUserEmail) {
        User me = currentUserEmail != null ? userRepository.findByEmail(currentUserEmail).orElse(null) : null;
        UUID myId = me != null ? me.getId() : null;

        // Fetch all comments for target with author eagerly loaded
        List<Comment> all = commentRepository.findThreadWithAuthor(targetType, targetId);

        // Precompute like counts + likedByMe (simple approach via per-comment lookups; OK for small sizes).
        // If you need scale, we can replace with aggregate queries.
        Map<UUID, Long> likeCounts = new HashMap<>();
        Set<UUID> likedByMe = new HashSet<>();

        for (Comment c : all) {
            likeCounts.put(c.getId(), commentLikeRepository.countByCommentId(c.getId()));
            if (myId != null && commentLikeRepository.findByCommentIdAndUserId(c.getId(), myId).isPresent()) {
                likedByMe.add(c.getId());
            }
        }

        Map<UUID, CommentDto> dtoById = all.stream()
                .collect(Collectors.toMap(Comment::getId, c -> toDto(c, likeCounts.getOrDefault(c.getId(), 0L), likedByMe.contains(c.getId()))));

        // Root list
        List<CommentDto> roots = new ArrayList<>();
        for (Comment c : all) {
            CommentDto dto = dtoById.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(dto);
            } else {
                CommentDto parent = dtoById.get(c.getParentId());
                if (parent != null) {
                    parent.getReplies().add(dto);
                } else {
                    // Orphan reply: treat as root
                    roots.add(dto);
                }
            }
        }
        return roots;
    }

    @Transactional
    public CommentDto addComment(CommentTargetType targetType, UUID targetId, CommentCreateRequest request, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow();

        // Minimal guard: only STUDENT should post comments (controller also enforces)
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can comment");
        }

        // Validate target exists
        if (targetType == CommentTargetType.EVENT) {
            eventRepository.findById(targetId).orElseThrow();
        } else {
            activityRepository.findById(targetId).orElseThrow();
        }

        UUID parentId = request.getParentId();
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId).orElseThrow();
            if (parent.getTargetType() != targetType || !parent.getTargetId().equals(targetId)) {
                throw new ForbiddenException("Parent comment does not belong to this target");
            }
        }

        Comment saved = commentRepository.save(Comment.builder()
                .targetType(targetType)
                .targetId(targetId)
                .parentId(parentId)
                .author(student)
                .content(request.getContent().trim())
                .deleted(false)
                .build());

        return toDto(saved, 0L, false);
    }

    @Transactional
    public void toggleLike(UUID commentId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can like comments");
        }

        Comment comment = commentRepository.findById(commentId).orElseThrow();

        Optional<CommentLike> existing = commentLikeRepository.findByCommentIdAndUserId(commentId, student.getId());
        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
        } else {
            commentLikeRepository.save(CommentLike.builder().comment(comment).user(student).build());
        }
    }

    private CommentDto toDto(Comment c, long likeCount, boolean likedByMe) {
        CommentDto dto = new CommentDto();
        dto.setId(c.getId());
        dto.setParentId(c.getParentId());
        dto.setAuthorId(c.getAuthor() != null ? c.getAuthor().getId() : null);
        dto.setAuthorFullName(c.getAuthor() != null ? c.getAuthor().getFullName() : "");
        dto.setCreatedAt(c.getCreatedAt());
        dto.setLikeCount(likeCount);
        dto.setLikedByMe(likedByMe);

        if (c.isDeleted()) {
            dto.setContent("[deleted]");
        } else {
            dto.setContent(c.getContent());
        }
        return dto;
    }
}
