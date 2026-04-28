package io.droidevs.mclub.controller;


import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.service.AttendanceService;
import io.droidevs.mclub.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EventService eventService;

    // ===============================
    // 🎯 ATTENDANCE WINDOW (RESOURCE)
    // ===============================

    // Open or rotate QR window
    @PutMapping("/attendance-window")
    public ResponseEntity<AttendanceEventQrDto> openOrUpdateWindow(
            @PathVariable UUID eventId,
            @Valid @RequestBody AttendanceWindowRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(
                attendanceService.openOrUpdateWindow(eventId, request, auth.getName())
        );
    }

    // Close attendance window
    @DeleteMapping("/attendance-window")
    public ResponseEntity<Void> closeWindow(
            @PathVariable UUID eventId,
            Authentication auth
    ) {
        attendanceService.closeWindow(eventId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    // ===============================
    // 👥 ATTENDANCE RECORDS (COLLECTION)
    // ===============================

    // List attendance records
    @GetMapping("/attendances")
    public ResponseEntity<Page<AttendanceRecordDto>> listAttendances(
            @PathVariable UUID eventId,
            @PageableDefault Pageable pageable,
            Authentication auth
    ) {
        eventService.requireCanManageEvent(auth.getName(), eventId);

        return ResponseEntity.ok(
                attendanceService.listAttendance(eventId,pageable, auth.getName())
        );
    }

    // Organizer manually checks in a student
    @PostMapping("/attendances")
    public ResponseEntity<AttendanceRecordDto> organizerCheckIn(
            @PathVariable UUID eventId,
            @Valid @RequestBody ManualCheckInRequest request,
            Authentication auth
    ) {
        var result = attendanceService.organizerCheckInStudent(
                eventId,
                request.getStudentId(),
                auth.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}