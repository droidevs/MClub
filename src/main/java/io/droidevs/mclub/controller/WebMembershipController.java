package io.droidevs.mclub.controller;

import io.droidevs.mclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/memberships")
@RequiredArgsConstructor
public class WebMembershipController {

    private final MembershipService membershipService;

    @PostMapping("/club/{clubId}/join")
    public String joinClub(@PathVariable UUID clubId, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            membershipService.joinClub(clubId, auth.getName());
            redirectAttributes.addFlashAttribute("message", "Membership request submitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to join club: " + e.getMessage());
        }
        return "redirect:/clubs";
    }
}

