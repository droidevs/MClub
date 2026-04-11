package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.EventRatingDto;
import io.droidevs.mclub.dto.EventRatingRequest;
import io.droidevs.mclub.dto.EventRatingSummaryDto;
import io.droidevs.mclub.service.EventRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/ratings")
@RequiredArgsConstructor
public class EventRatingController {

    private final EventRatingService eventRatingService;

    // Student creates/updates their rating
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EventRatingDto> rate(@PathVariable UUID eventId,
                                              @Valid @RequestBody EventRatingRequest request,
                                              Authentication auth) {
        return ResponseEntity.ok(eventRatingService.rateEvent(eventId, request, auth.getName()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EventRatingDto> myRating(@PathVariable UUID eventId, Authentication auth) {
        return ResponseEntity.ok(eventRatingService.getMyRating(eventId, auth.getName()));
    }

    @GetMapping("/summary")
    public ResponseEntity<EventRatingSummaryDto> summary(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventRatingService.getSummary(eventId));
    }

    // Full list: platform admin only for now (club-scoped admin/staff can be added next)
    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<List<EventRatingDto>> list(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventRatingService.getAllRatingsForEvent(eventId));
    }
}

