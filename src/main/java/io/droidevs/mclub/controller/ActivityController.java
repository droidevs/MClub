package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.ActivityCreateRequest;
import io.droidevs.mclub.dto.ActivityDto;
import io.droidevs.mclub.dto.ActivitySummary;
import io.droidevs.mclub.mapper.ActivityMapper;
import io.droidevs.mclub.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    public ResponseEntity<ActivityDto> create(@Valid @RequestBody ActivityCreateRequest request, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(activityService.create(request, auth.getName()));
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<Page<ActivitySummary>> getByClub(@PathVariable UUID clubId, @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(activityService.getSummaryByClub(clubId, pageable));
    }

    // ===============================
    // 🔹 GET DETAILS
    // ===============================
    @GetMapping("/activities/{activityId}")
    public ActivityDto getById(@PathVariable UUID activityId) {
        return activityService.getById(activityId);
    }

    // ===============================
    // 🔹 UPDATE
    // ===============================
    @PutMapping("/activities/{activityId}")
    public ActivityDto update(
            @PathVariable UUID activityId,
            @RequestBody ActivityDto dto,
            Authentication auth
    ) {
        return activityService.update(activityId, dto, auth.getName());
    }

    // ===============================
    // 🔹 DELETE
    // ===============================
    @DeleteMapping("/activities/{activityId}")
    public void delete(
            @PathVariable UUID activityId,
            Authentication auth
    ) {
        activityService.delete(activityId, auth.getName());
    }

    // ===============================
    // 🔹 RECENT
    // ===============================
    @GetMapping("/clubs/{clubId}/activities/recent")
    public List<ActivitySummary> recent(@PathVariable UUID clubId) {
        return activityService.getRecentByClub(clubId);
    }

    // ===============================
    // 🔹 SEARCH
    // ===============================
    @GetMapping("/clubs/{clubId}/activities/search")
    public Page<ActivitySummary> search(
            @PathVariable UUID clubId,
            @RequestParam String q,
            Pageable pageable
    ) {
        return activityService.search(clubId, q, pageable);
    }

    // ===============================
    // 🔹 COUNT
    // ===============================
    @GetMapping("/clubs/{clubId}/activities/count")
    public long count(@PathVariable UUID clubId) {
        return activityService.count(clubId);
    }
}
