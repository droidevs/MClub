package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.ApplicationStatus;
import io.droidevs.mclub.domain.ClubApplicationSummary;
import io.droidevs.mclub.domain.ClubUserApplicationSummary;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.ClubApplicationDto;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.service.ClubApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.attribute.UserPrincipal;
import java.util.UUID;

@Controller
@RequestMapping("/club-applications")
@RequiredArgsConstructor
public class WebClubApplicationController {

    private final ClubApplicationService applicationService;
    private final UserRepository userRepository;

    @PostMapping("/submit")
    public String submitApplication(@ModelAttribute ClubApplicationDto dto, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            applicationService.submitApplication(dto, auth.getName());
            redirectAttributes.addFlashAttribute("message", "Club application submitted successfully and is pending approval!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Application failed: " + e.getMessage());
        }
        return "redirect:/clubs";
    }

    @GetMapping({"/apply", "/apply-club"})
    public String showApplicationForm(Model model) {
        model.addAttribute("clubApplicationDto", new ClubApplicationDto());
        return "apply-club";
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public String viewApplications(
            @RequestParam(name = "status", defaultValue = "PENDING") String status,
            @PageableDefault(size = 10) Pageable pageable,
            Model model
    ) {
        // Convert string to Enum safely
        ApplicationStatus currentStatus = ApplicationStatus.valueOf(status.toUpperCase());

        // Fetch data based on status
        model.addAttribute("applicationsPage", applicationService.getApplicationsByStatus(currentStatus, pageable));
        model.addAttribute("stats", applicationService.getDashboardStats());
        model.addAttribute("currentStatus", status.toUpperCase()); // To highlight active tab

        return "club-applications";
    }

    @GetMapping("/my")
    public String viewMyApplications(Model model,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     @PageableDefault(size = 10) Pageable pageable) {

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow();
        Page<ClubApplicationSummary> applications = applicationService.getUserApplications(user.getId(), pageable);

        model.addAttribute("applicationsPage", applications);
        model.addAttribute("userId", user.getId());
        return "my-applications";
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public String approve(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            applicationService.approveApplication(id);
            redirectAttributes.addFlashAttribute("message", "Application approved. Club created and user assigned as Admin.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve: " + e.getMessage());
        }
        return "redirect:/club-applications";
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public String reject(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            applicationService.rejectApplication(id);
            redirectAttributes.addFlashAttribute("message", "Application rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reject: " + e.getMessage());
        }
        return "redirect:/club-applications";
    }
}
