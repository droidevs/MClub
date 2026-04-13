package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.service.AttendanceService;
import io.droidevs.mclub.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EventService eventService;

    // Organizer: open/update window + generate a fresh QR token for the event
    @PostMapping("/api/events/{eventId}/attendance/window")
    public ResponseEntity<AttendanceEventQrDto> openOrUpdate(@PathVariable UUID eventId,
                                                            @Valid @RequestBody AttendanceWindowRequest request,
                                                            Authentication auth) {
        return ResponseEntity.ok(attendanceService.openOrUpdateWindow(eventId, request, auth.getName()));
    }

    // Organizer: close attendance window
    @PostMapping("/api/events/{eventId}/attendance/window/close")
    public ResponseEntity<Void> close(@PathVariable UUID eventId, Authentication auth) {
        attendanceService.closeWindow(eventId, auth.getName());
        return ResponseEntity.ok().build();
    }

    // Organizer: list attendance
    @GetMapping("/api/events/{eventId}/attendance")
    public ResponseEntity<List<AttendanceRecordDto>> list(@PathVariable UUID eventId, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        eventService.requireCanManageEvent(auth.getName(), eventId);
        return ResponseEntity.ok(attendanceService.listAttendance(eventId, auth.getName()));
    }

    // Student: scan event QR token and self check-in
    @PostMapping("/api/attendance/check-in")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AttendanceRecordDto> studentCheckIn(@Valid @RequestBody AttendanceScanRequest request,
                                                             Authentication auth) {
        return ResponseEntity.ok(attendanceService.studentCheckInByEventQr(request.getToken(), auth.getName()));
    }

    // Organizer: manually check-in a student (after scanning student's code in real life)
    @PostMapping("/api/events/{eventId}/attendance/check-in/{studentId}")
    public ResponseEntity<AttendanceRecordDto> organizerCheckIn(@PathVariable UUID eventId,
                                                               @PathVariable UUID studentId,
                                                               Authentication auth) {
        return ResponseEntity.ok(attendanceService.organizerCheckInStudent(eventId, studentId, auth.getName()));
    }
}

