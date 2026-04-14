package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.CommentTargetType;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.dto.CommentDto;
import io.droidevs.mclub.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{targetType}/{targetId}")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable String targetType,
                                                       @PathVariable UUID targetId,
                                                       Authentication auth) {
        CommentTargetType type = CommentTargetType.valueOf(targetType.toUpperCase());
        String email = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(commentService.getThread(type, targetId, email));
    }

    /** Fetch all direct replies for a comment (children only). */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentDto>> getReplies(@PathVariable UUID commentId, Authentication auth) {
        String email = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(commentService.getDirectReplies(commentId, email));
    }

    @PostMapping("/{targetType}/{targetId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CommentDto> addComment(@PathVariable String targetType,
                                                 @PathVariable UUID targetId,
                                                 @Valid @RequestBody CommentCreateRequest request,
                                                 Authentication auth) {
        CommentTargetType type = CommentTargetType.valueOf(targetType.toUpperCase());
        return ResponseEntity.ok(commentService.addComment(type, targetId, request, auth.getName()));
    }

    @PostMapping("/{commentId}/like")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> toggleLike(@PathVariable UUID commentId, Authentication auth) {
        commentService.toggleLike(commentId, auth.getName());
        return ResponseEntity.ok().build();
    }

    /**
     * Create a reply to an existing comment.
     *
     * Note: This is functionally equivalent to POST /api/comments/{targetType}/{targetId} with parentId set,
     * but provides a clear, well-named endpoint for clients.
     */
    @PostMapping("/{commentId}/reply")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CommentDto> reply(@PathVariable UUID commentId,
                                           @Valid @RequestBody CommentCreateRequest request,
                                           Authentication auth) {
        return ResponseEntity.ok(commentService.reply(commentId, request, auth.getName()));
    }
}
