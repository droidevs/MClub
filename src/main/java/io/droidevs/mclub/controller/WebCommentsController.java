package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.Comment;
import io.droidevs.mclub.domain.CommentTargetType;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.dto.CommentDto;
import io.droidevs.mclub.service.CommentService;
import io.droidevs.mclub.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/comments")
public class WebCommentsController {

    private final CommentService commentService;
    private final EventService eventService;

    @GetMapping("/{targetType}/{targetId}")
    public String comments(@PathVariable String targetType,
                           @PathVariable UUID targetId,
                           Model model,
                           @PageableDefault Pageable pageable,
                           Authentication auth) {

        CommentTargetType type = CommentTargetType.valueOf(targetType.toUpperCase());

        String userEmail = (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName()
                : null;

        if (type == CommentTargetType.EVENT) {
            Event event = eventService.getEvent(targetId);
            model.addAttribute("event", event);
            model.addAttribute("backUrl", "/events/" + event.getId());
            model.addAttribute("pageTitle", "Comments");
            model.addAttribute("pageSubtitle", event.getTitle());
        } else {
            model.addAttribute("backUrl", "/");
            model.addAttribute("pageTitle", "Comments");
            model.addAttribute("pageSubtitle", "Activity");
        }

        model.addAttribute("targetType", type.name());
        model.addAttribute("targetId", targetId);

        model.addAttribute(
                "commentsPage",
                commentService.getThread(type, targetId, pageable, userEmail)
        );

        model.addAttribute("form", new CommentCreateRequest());

        // 👇 UI flag (important UX improvement)
        model.addAttribute("isAuthenticated", userEmail != null);

        return "comments";
    }

    @GetMapping("/comments/thread/{commentId}")
    public String commentThread(@PathVariable UUID commentId,
                                Model model,
                                @PageableDefault(size = 10) Pageable pageable,
                                Authentication auth) {

        String userEmail = (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName()
                : null;

        CommentDto rootComment = commentService.getComment(commentId);

        Comment comment = commentService.getCommentEntity(commentId);

        Page<CommentDto> repliesPage =
                commentService.getDirectReplies(commentId, pageable, userEmail);

        model.addAttribute("targetType", comment.getTargetType());
        model.addAttribute("targetId", comment.getTargetId());
        model.addAttribute("rootComment", rootComment);
        model.addAttribute("repliesPage", repliesPage);
        model.addAttribute("commentId", commentId);
        model.addAttribute("isAuthenticated", userEmail != null);

        return "comment-thread";
    }
}
