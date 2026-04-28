package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.MembershipStatus;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<MembershipDto> updateStatus(
            @PathVariable UUID membershipId,
            @RequestParam String status,
            @AuthenticationPrincipal Authentication auth
    ) {
        return ResponseEntity.ok(membershipService.updateStatus(membershipId, MembershipStatus.valueOf(status),auth.getName()));
    }

    @PostMapping("/{membershipId}/approve")
    public ResponseEntity<MembershipDto> approve(
            @PathVariable UUID membershipId,
            @AuthenticationPrincipal Authentication auth
    ) {
        return ResponseEntity.ok(
                membershipService.approve(membershipId, auth.getName())
        );
    }

    @PostMapping("/{membershipId}/reject")
    public ResponseEntity<MembershipDto> reject(
            @PathVariable UUID membershipId,
            @AuthenticationPrincipal Authentication auth
    ) {
        return ResponseEntity.ok(
                membershipService.reject(membershipId, auth.getName())
        );
    }

    @PostMapping("/{membershipId}/kick")
    public ResponseEntity<MembershipDto> kick(
            @PathVariable UUID membershipId,
            @AuthenticationPrincipal Authentication auth
    ) {
        return ResponseEntity.ok(
                membershipService.kick(membershipId, auth.getName())
        );
    }

    @PutMapping("/{membershipId}/role")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<MembershipDto> updateRole(
            @PathVariable UUID membershipId,
            @RequestParam String role,
            Authentication auth) {
        return ResponseEntity.ok(membershipService.updateRole(membershipId, role,auth.getName()));
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<Page<MembershipDto>> getMembers(
            @PathVariable UUID clubId,
            @PageableDefault Pageable pageable,
            Authentication auth) {
        return ResponseEntity.ok(membershipService.getMembers(clubId,pageable, auth.getName()));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<MembershipDto>> searchMembers(
            @RequestParam UUID clubId,
            @RequestParam String q,
            @PageableDefault Pageable pageable,
            Authentication auth) {
        return ResponseEntity.ok(membershipService.searchMembers(clubId, q, pageable, auth.getName()));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<MembershipDto>> getMembershipHistory(
            @RequestParam UUID clubId,
            @PageableDefault Pageable pageable,
            Authentication auth) {
        return ResponseEntity.ok(membershipService.getMembershipHistory(clubId, pageable, auth.getName()));
    }
}
