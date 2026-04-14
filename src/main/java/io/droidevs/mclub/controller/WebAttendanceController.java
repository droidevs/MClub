package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.AttendanceRecordDto;
import io.droidevs.mclub.dto.AttendanceWindowRequest;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.EventRepository;
import io.droidevs.mclub.repository.MembershipRepository;
import io.droidevs.mclub.service.AttendanceService;
import io.droidevs.mclub.service.CurrentUserService;
import io.droidevs.mclub.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/club-admin")
@RequiredArgsConstructor
public class WebAttendanceController {

    private final ClubRepository clubRepository;
    private final EventRepository eventRepository;
    private final MembershipRepository membershipRepository;
    private final AttendanceService attendanceService;
    private final CurrentUserService currentUserService;
    private final EventService eventService;

    private boolean canManageClub(UUID clubId, UUID userId) {
        return membershipRepository.findByClubId(clubId).stream()
                .anyMatch(m -> m.getUser().getId().equals(userId)
                        && (m.getRole() == ClubRole.ADMIN || m.getRole() == ClubRole.STAFF));
    }

    @GetMapping("/clubs/{clubId}/attendance")
    public String clubAttendanceEvents(@PathVariable UUID clubId, Model model, Authentication auth) {
        User user = currentUserService.requireUser(auth);
        if (!canManageClub(clubId, user.getId())) {
            return "redirect:/";
        }

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));

        model.addAttribute("club", club);
        model.addAttribute("events", eventRepository.findByClubId(clubId));
        return "club-attendance-events";
    }

    @GetMapping("/events/{eventId}/attendance")
    public String manageAttendance(@PathVariable UUID eventId,
                                   @ModelAttribute("qrToken") String qrToken,
                                   Model model,
                                   Authentication auth) {
        var event = eventRepository.findByIdWithClub(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        User user = currentUserService.requireUser(auth);
        UUID clubId = event.getClub().getId();
        if (!canManageClub(clubId, user.getId())) {
            return "redirect:/";
        }

        List<AttendanceRecordDto> attendance = attendanceService.listAttendance(eventId, auth.getName());

        model.addAttribute("club", event.getClub());
        model.addAttribute("event", event);
        model.addAttribute("attendance", attendance);
        model.addAttribute("attendanceCount", attendance.size());
        model.addAttribute("qrToken", (qrToken == null || qrToken.isBlank()) ? null : qrToken);
        model.addAttribute("windowRequest", new AttendanceWindowRequest());

        return "manage-attendance";
    }

    @PostMapping("/events/{eventId}/attendance/window")
    public String openOrRotate(@PathVariable UUID eventId,
                               @RequestParam int opensBeforeStartMinutes,
                               @RequestParam int closesAfterStartMinutes,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            AttendanceWindowRequest req = new AttendanceWindowRequest();
            req.setOpensBeforeStartMinutes(opensBeforeStartMinutes);
            req.setClosesAfterStartMinutes(closesAfterStartMinutes);
            var qr = attendanceService.openOrUpdateWindow(eventId, req, auth.getName());
            redirectAttributes.addFlashAttribute("message", "Attendance window opened and QR rotated.");
            redirectAttributes.addFlashAttribute("qrToken", qr.getToken());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/club-admin/events/" + eventId + "/attendance";
    }

    @PostMapping("/events/{eventId}/attendance/window/close")
    public String close(@PathVariable UUID eventId, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            attendanceService.closeWindow(eventId, auth.getName());
            redirectAttributes.addFlashAttribute("message", "Attendance window closed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/club-admin/events/" + eventId + "/attendance";
    }

    @PostMapping("/events/{eventId}/attendance/check-in")
    public String manualCheckIn(@PathVariable UUID eventId,
                                @RequestParam UUID studentId,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            attendanceService.organizerCheckInStudent(eventId, studentId, auth.getName());
            redirectAttributes.addFlashAttribute("message", "Student checked in successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/club-admin/events/" + eventId + "/attendance";
    }

    @GetMapping("/events/{eventId}/admin")
    public String eventAdmin(@PathVariable UUID eventId, Model model, Authentication auth) {
        // Reuse attendanceService authorization via listAttendance (it enforces club admin/staff)
        // But here we just want the event for the page and rely on REST calls for data lists.
        var event = eventService.getEvent(eventId);
        // Ensure user can manage this event
        eventService.requireCanManageEvent(auth.getName(), eventId);

        model.addAttribute("event", event);
        return "event-admin";
    }
}
