package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.CommentTargetType;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.service.CommentService;
import io.droidevs.mclub.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/events")
public class WebEventCommentsController {

    private final EventService eventService;
    private final CommentService commentService;

    @GetMapping("/{eventId}/comments")
    public String eventComments(@PathVariable UUID eventId, Model model, Authentication auth) {
        model.addAttribute("event", eventService.getEvent(eventId));
        model.addAttribute("comments", commentService.getThread(CommentTargetType.EVENT, eventId, auth != null ? auth.getName() : null));
        model.addAttribute("form", new CommentCreateRequest());
        return "event-comments";
    }

    @PostMapping("/{eventId}/comments")
    @PreAuthorize("hasRole('STUDENT')")
    public String postEventComment(@PathVariable UUID eventId,
                                   @Valid CommentCreateRequest form,
                                   Authentication auth,
                                   RedirectAttributes ra) {
        try {
            // Top-level comment
            form.setParentId(null);
            commentService.addComment(CommentTargetType.EVENT, eventId, form, auth.getName());
            ra.addFlashAttribute("message", "Comment posted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not post comment: " + e.getMessage());
        }
        return "redirect:/events/" + eventId + "/comments";
    }

    @PostMapping("/{eventId}/comments/{parentId}/reply")
    @PreAuthorize("hasRole('STUDENT')")
    public String replyToEventComment(@PathVariable UUID eventId,
                                      @PathVariable UUID parentId,
                                      @Valid CommentCreateRequest form,
                                      Authentication auth,
                                      RedirectAttributes ra) {
        try {
            form.setParentId(parentId);
            commentService.addComment(CommentTargetType.EVENT, eventId, form, auth.getName());
            ra.addFlashAttribute("message", "Reply posted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not post reply: " + e.getMessage());
        }
        return "redirect:/events/" + eventId + "/comments";
    }
}
