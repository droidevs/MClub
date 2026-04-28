package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.AttendanceRecordDto;
import io.droidevs.mclub.dto.AttendanceScanRequest;
import io.droidevs.mclub.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class StudentAttendanceController {

    private final AttendanceService attendanceService;

    // Student scans QR and checks in
    @PostMapping("/check-in")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AttendanceRecordDto> studentCheckIn(
            @Valid @RequestBody AttendanceScanRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(
                attendanceService.studentCheckInByEventQr(
                        request.getToken(),
                        auth.getName()
                )
        );
    }
}
