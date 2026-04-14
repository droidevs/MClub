package io.droidevs.mclub.controller;

import io.droidevs.mclub.service.AttendanceService;
import io.droidevs.mclub.service.EventService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
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
public class WebEventCheckInController {

    private final EventService eventService;
    private final AttendanceService attendanceService;

    @GetMapping("/{eventId}/check-in")
    @PreAuthorize("hasRole('STUDENT')")
    public String checkInPage(@PathVariable UUID eventId, Model model) {
        model.addAttribute("event", eventService.getEvent(eventId));
        model.addAttribute("form", new CheckInForm());
        return "event-checkin";
    }

    @PostMapping("/{eventId}/check-in")
    @PreAuthorize("hasRole('STUDENT')")
    public String doCheckIn(@PathVariable UUID eventId,
                            @ModelAttribute("form") CheckInForm form,
                            Authentication auth,
                            RedirectAttributes ra) {
        try {
            String token = form.getToken() != null ? form.getToken().trim() : "";
            if (token.isBlank()) {
                ra.addFlashAttribute("error", "Token is required.");
                return "redirect:/events/" + eventId + "/check-in";
            }

            // Reuse the same flow as the REST endpoint (/api/attendance/check-in)
            attendanceService.studentCheckInByEventQr(token, auth.getName());
            ra.addFlashAttribute("message", "Checked in successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Check-in failed: " + e.getMessage());
        }

        return "redirect:/events/" + eventId;
    }

    @Data
    public static class CheckInForm {
        @NotBlank
        private String token;
    }
}
