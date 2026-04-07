package io.droidevs.mclub.controller;

import io.droidevs.mclub.service.ClubService;
import io.droidevs.mclub.service.EventService;
import io.droidevs.mclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final ClubService clubService;
    private final EventService eventService;
    private final MembershipService membershipService;

    @GetMapping("/")
    public String dashboard(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }

        // Pass info about current user
        model.addAttribute("username", auth.getName());
        model.addAttribute("authorities", auth.getAuthorities());

        // Load partial data for dashboard
        model.addAttribute("recentClubs", clubService.getAllClubs(PageRequest.of(0, 5)).getContent());
        model.addAttribute("recentEvents", eventService.getAllEvents(PageRequest.of(0, 5)).getContent());

        return "dashboard";
    }

    @GetMapping("/clubs")
    public String clubs(Model model, Pageable pageable) {
        model.addAttribute("clubsPage", clubService.getAllClubs(pageable));
        return "clubs";
    }

    @GetMapping("/clubs/{id}")
    public String clubDetail(@PathVariable UUID id, Model model) {
        model.addAttribute("club", clubService.getClub(id));
        model.addAttribute("members", membershipService.getApprovedMembers(id));
        return "club-detail";
    }

    @GetMapping("/events")
    public String events(Model model, Pageable pageable) {
        model.addAttribute("eventsPage", eventService.getAllEvents(pageable));
        return "events";
    }
}
