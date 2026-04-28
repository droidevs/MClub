package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.AttendanceRecordDto;
import io.droidevs.mclub.dto.AttendanceWindowRequest;
import io.droidevs.mclub.dto.ClubDto;
import io.droidevs.mclub.dto.EventDto;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.EventRepository;
import io.droidevs.mclub.repository.MembershipRepository;
import io.droidevs.mclub.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebAttendanceController {

    private final ClubRepository clubRepository;
    private final EventRepository eventRepository;
    private final MembershipRepository membershipRepository;
    private final AttendanceService attendanceService;
    private final CurrentUserService currentUserService;
    private final ClubAuthorizationService authorizationService;
    private final EventService eventService;
    private final ClubService clubService;


    @GetMapping("/clubs/{clubId}/attendance")
    public String clubAttendanceEvents(@PathVariable UUID clubId, Model model, Pageable pageable, Authentication auth) {
        if (!authorizationService.canManageClub(clubId, auth.getName())) {
            return "redirect:/";
        }

        ClubDto club = clubService.getClub(clubId);
        Page<EventDto> events = eventService.getEventsByClub(clubId, pageable);

        model.addAttribute("club", club);
        model.addAttribute("eventsPage", events);
        return "club-attendance-events";
    }


    @GetMapping("/events/{eventId}/attendance")
    public String manageAttendance(@PathVariable UUID eventId,
                                   @ModelAttribute("qrToken") String qrToken,
                                   Model model,
                                   @PageableDefault Pageable pageable,
                                   Authentication auth) {
        var event = eventRepository.findByIdWithClub(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        UUID clubId = event.getClub().getId();
        if (!authorizationService.canManageClub(clubId, auth.getName())) {
            return "redirect:/";
        }

        Page<AttendanceRecordDto> attendance = attendanceService.listAttendance(eventId,pageable, auth.getName());

        model.addAttribute("club", event.getClub());
        model.addAttribute("event", event);
        model.addAttribute("attendancePage", attendance);
        model.addAttribute("qrToken", (qrToken == null || qrToken.isBlank()) ? null : qrToken);
        model.addAttribute("windowRequest", new AttendanceWindowRequest());

        return "manage-attendance";
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
