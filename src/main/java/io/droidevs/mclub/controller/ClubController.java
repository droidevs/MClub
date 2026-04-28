package io.droidevs.mclub.controller;
import io.droidevs.mclub.dto.ClubDto;
import io.droidevs.mclub.dto.CreateClubRequest;
import io.droidevs.mclub.dto.UpdateClubDescriptionRequest;
import io.droidevs.mclub.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.droidevs.mclub.dto.ClubSummaryDto;
import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    // ===============================
    // 🔹 CREATE CLUB
    // ===============================
    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ClubDto> createClub(
            @Valid @RequestBody CreateClubRequest request,
            Authentication auth
    ) {
        ClubDto dto = new ClubDto();
        dto.setName(request.name());
        dto.setDescription(request.description());

        return ResponseEntity.ok(
                clubService.createClub(dto, auth.getName())
        );
    }

    // ===============================
    // 🔹 GET ALL (HEAVY)
    // ===============================
    @GetMapping
    public ResponseEntity<Page<ClubDto>> getAllClubs(Pageable pageable) {
        return ResponseEntity.ok(clubService.getAllClubs(pageable));
    }

    // ===============================
    // 🔹 GET SUMMARY (LIGHT - BEST FOR UI)
    // ===============================
    @GetMapping("/summary")
    public ResponseEntity<Page<ClubSummaryDto>> getClubsSummary(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(clubService.getClubsSummary(pageable));
    }

    // ===============================
    // 🔹 GET BY ID
    // ===============================
    @GetMapping("/{id}")
    public ResponseEntity<ClubDto> getClub(@PathVariable UUID id) {
        return ResponseEntity.ok(clubService.getClub(id));
    }

    // ===============================
    // 🔹 MY MANAGED CLUBS (FULL)
    // ===============================
    @GetMapping("/my-managed")
    public ResponseEntity<Page<ClubDto>> getMyManagedClubs(
            Authentication auth,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(
                clubService.getMyManagedClubs(auth.getName(), pageable)
        );
    }

    // ===============================
    // 🔹 MY MANAGED CLUBS (LIGHT)
    // ===============================
    @GetMapping("/my-managed/summary")
    public ResponseEntity<Page<ClubSummaryDto>> getMyManagedClubsSummary(
            Authentication auth,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(
                clubService.getMyManagedClubsSummary(auth.getName(), pageable)
        );
    }

    // ===============================
    // 🔹 SEARCH
    // ===============================
    @GetMapping("/search")
    public ResponseEntity<Page<ClubSummaryDto>> search(
            @RequestParam String query,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                clubService.search(query, pageable)
        );
    }

    // ===============================
    // 🔹 UPDATE DESCRIPTION ONLY
    // ===============================
    @PatchMapping("/{id}/description")
    public ResponseEntity<Void> updateDescription(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClubDescriptionRequest request,
            Authentication auth
    ) {
        clubService.updateClubDescription(id, request.description(), auth.getName());
        return ResponseEntity.noContent().build();
    }
}