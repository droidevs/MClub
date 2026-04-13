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
}

