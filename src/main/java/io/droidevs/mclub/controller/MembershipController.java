package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {
    private final MembershipService membershipService;

    @PostMapping("/club/{clubId}/join")
    public ResponseEntity<MembershipDto> joinClub(@PathVariable UUID clubId, Authentication auth) {
        return ResponseEntity.ok(membershipService.joinClub(clubId, auth.getName()));
    }

    @PutMapping("/{membershipId}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<MembershipDto> updateStatus(@PathVariable UUID membershipId, @RequestParam String status) {
        return ResponseEntity.ok(membershipService.updateStatus(membershipId, status));
    }

    @PutMapping("/{membershipId}/role")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<MembershipDto> updateRole(@PathVariable UUID membershipId, @RequestParam String role) {
        return ResponseEntity.ok(membershipService.updateRole(membershipId, role));
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<MembershipDto>> getMembers(@PathVariable UUID clubId) {
        return ResponseEntity.ok(membershipService.getMembers(clubId));
    }
}
