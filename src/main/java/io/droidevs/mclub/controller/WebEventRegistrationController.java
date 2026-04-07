package io.droidevs.mclub.controller;

import io.droidevs.mclub.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/events")
@RequiredArgsConstructor
public class WebEventRegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/{eventId}/register")
    public String registerForEvent(@PathVariable UUID eventId, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            registrationService.register(eventId, auth.getName());
            redirectAttributes.addFlashAttribute("message", "Registered for event successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
        }
        return "redirect:/events";
    }
}
