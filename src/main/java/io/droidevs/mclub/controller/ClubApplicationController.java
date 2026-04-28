package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.ClubApplicationDto;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.service.ClubApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ClubApplicationController {

    private final ClubApplicationService applicationService;
    private final UserRepository userRepository;

    @GetMapping("/my-applications")
    public ResponseEntity<Page<ClubApplicationSummary>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal, // Assuming Spring Security
            Pageable pageable) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(applicationService.getUserApplications(user.getId(), pageable));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<ClubApplicationDto>> getPendingApplications(Pageable pageable) {
        return ResponseEntity.ok(applicationService.getPendingApplications(pageable));
    }

    @GetMapping("/approved")
    public ResponseEntity<Page<ClubApplicationDto>> getApprovedApplications(Pageable pageable) {
        return ResponseEntity.ok(applicationService.getApprovedApplications(pageable));
    }

    @GetMapping("/rejected")
    public ResponseEntity<Page<ClubApplicationDto>> getRejectedApplications(Pageable pageable) {
        return ResponseEntity.ok(applicationService.getRejectedApplications(pageable));
    }


    /**
     * GET /api/admin/applications/{id}
     * Returns the full entity including relations via @EntityGraph
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClubApplication> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getApplicationDetail(id));
    }

    /**
     * GET /api/admin/applications/stats
     * Returns counts for the dashboard cards
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(applicationService.getDashboardStats());
    }

    /**
     * GET /api/admin/applications/count?status=PENDING
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getCountByStatus(@RequestParam ApplicationStatus status) {
        return ResponseEntity.ok(applicationService.countByStatus(status));
    }
}