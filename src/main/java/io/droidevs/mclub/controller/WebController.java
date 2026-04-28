package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.service.*;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final ClubService clubService;
    private final EventService eventService;
    private final MembershipService membershipService;
    private final AttendanceService attendanceService;
    private final ClubAuthorizationService authorizationService;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final CurrentUserService currentUserService;
    private final EventRatingService eventRatingService;
    private final ActivityService activityService;
    private final CommentService commentService;

    @GetMapping("/")
    public String dashboard(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        // Pass info about current user
        model.addAttribute("username", auth.getName());
        model.addAttribute("authorities", auth.getAuthorities());

        // Load partial data for dashboard
        model.addAttribute("recentClubs", clubService.getClubsSummary(PageRequest.of(0, 5)).getContent());
        model.addAttribute("recentEvents", eventService.getAllEvents(PageRequest.of(0, 5)).getContent());

        return "dashboard";
    }

    @GetMapping("/clubs")
    public String clubs(Model model, Pageable pageable) {
        model.addAttribute("clubsPage", clubService.getClubsSummary(pageable));
        return "clubs";
    }

    @GetMapping("/clubs/{id}")
    public String clubDetail(@PathVariable UUID id,
                             Model model,
                             Authentication auth,
                             @PageableDefault(size = 10) Pageable pageable) {

        model.addAttribute("club", clubService.getClub(id));

        List<MembershipDto> members = List.of();

        boolean canViewMembers = false;

        try {
            members = membershipService.getRecentMembers(id, auth.getName());
            canViewMembers = true;
        } catch (Exception e) {
            // forbidden OR any service exception → just hide members
            canViewMembers = false;
        }

        model.addAttribute("membersPage", members);
        model.addAttribute("recentMembers", members);
        model.addAttribute("canViewMembers", canViewMembers);

        model.addAttribute("recentEvents", eventService.getRecentEventsByClub(id));
        model.addAttribute("recentActivities", activityService.getRecentByClub(id));

        return "club-detail";
    }

    @GetMapping("/clubs/{id}/members")
    public String clubMembers(@PathVariable UUID id,
                              Model model,
                              Authentication auth,
                              RedirectAttributes redirectAttributes,
                              @PageableDefault(size = 20) Pageable pageable) {

        try {
            model.addAttribute("club", clubService.getClub(id));

            Page<MembershipDto> membersPage = membershipService.getMembers(id, pageable, auth.getName());

            model.addAttribute("membersPage", membersPage);
            model.addAttribute("members", membersPage.getContent());

            return "club-members";
        } catch (ForbiddenException ex) {
            redirectAttributes.addFlashAttribute("error", "You are not allowed to view club members.");
            return "redirect:/clubs/" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Something went wrong.");
            return "redirect:/clubs/" + id;
        }

    }

    @GetMapping("/clubs/{id}/events")
    public String clubEvents(@PathVariable UUID id,
                             @RequestParam(required = false) String q,
                             Model model,
                             @PageableDefault(size = 10) Pageable pageable) {

        Page<EventDto> events;

        if (q != null && !q.isBlank()) {
            events = eventService.searchEvents(id, q, pageable);
        } else {
            events = eventService.getEventsByClub(id, pageable);
        }

        model.addAttribute("club", clubService.getClub(id));
        model.addAttribute("eventsPage", events);
        model.addAttribute("events", events.getContent());
        model.addAttribute("query", q);

        return "club-events";
    }

    @GetMapping("/clubs/{id}/activities")
    public String clubActivities(@PathVariable UUID id, Model model, @PageableDefault Pageable pageable) {
        Page<ActivitySummary> activities = activityService.getSummaryByClub(id, pageable);

        model.addAttribute("club", clubService.getClub(id));
        model.addAttribute("activitiesPage", activities);

        return "club-activities";
    }

    @GetMapping("/activities/{id}")
    public String activityDetails(@PathVariable UUID id, Model model) {
        ActivityDto activity = activityService.getById(id);

        model.addAttribute("activity", activity);
        return "activity-detail";
    }

    @GetMapping("/events")
    public String events(Model model, Pageable pageable) {
        model.addAttribute("eventsPage", eventService.getAllEvents(pageable));
        return "events";
    }

    @GetMapping("/events/{id}")
    public String eventDetail(@PathVariable UUID id,
                              Model model,
                              Authentication auth) {

        var event = eventService.getEvent(id);

        boolean eventEnded = event.getEndDate() != null
                ? event.getEndDate().isBefore(LocalDateTime.now())
                : (event.getStartDate() != null && event.getStartDate().isBefore(LocalDateTime.now()));

        long attendanceCount = 0;
        boolean authenticated = true;

        if (eventEnded && auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                attendanceCount = attendanceService.countAttendance(id);
            } catch (Exception ignored) {
                // keep 0
            }
        } else {
            authenticated = false;
        }

        // Rating summary (unchanged)
        double ratingAverage = 0.0;
        long ratingCount = 0L;
        try {
            var summary = eventRatingService.getSummary(id);
            ratingAverage = summary.getAverage();
            ratingCount = summary.getCount();
        } catch (Exception ignored) {}

        // Comments (unchanged)
        try {
            String email = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                    ? auth.getName()
                    : null;
            model.addAttribute("commentsPreview",
                    commentService.getPreview(CommentTargetType.EVENT, id, email));
        } catch (Exception ignored) {
            model.addAttribute("commentsPreview",
                    new CommentPreviewDto(0, 0, 0, List.of(), null));
        }

        model.addAttribute("event", event);
        model.addAttribute("eventEnded", eventEnded);
        model.addAttribute("attendanceCount", attendanceCount);
        model.addAttribute("ratingAverage", ratingAverage);
        model.addAttribute("ratingCount", ratingCount);
        model.addAttribute("auth", auth);

        return "event-detail";
    }

    @GetMapping("/clubs/{clubId}/events/new")
    public String newClubEvent(@PathVariable UUID clubId, Model model, Authentication auth) {
        User user = currentUserService.requireUser(auth);
        if (!authorizationService.canManageClub(clubId, auth.getName())) {
            return "redirect:/";
        }
        var club = clubService.getClub(clubId);
        EventCreateRequest form = new EventCreateRequest();
        form.setClubId(clubId);
        model.addAttribute("club", club);
        model.addAttribute("form", form);
        return "club-event-new";
    }

    @GetMapping("/clubs/{clubId}/activities/new")
    public String newClubActivity(@PathVariable UUID clubId, Model model,Pageable pageable, Authentication auth) {
        User user = currentUserService.requireUser(auth);
        if (!authorizationService.canManageClub(clubId, auth.getName())) {
            return "redirect:/";
        }
        var club = clubRepository.findById(clubId).orElseThrow();
        ActivityCreateRequest form = new ActivityCreateRequest();
        form.setClubId(clubId);
        model.addAttribute("club", club);
        model.addAttribute("form", form);
        model.addAttribute("events", eventService.getEventsByClub(clubId,pageable));
        return "club-activity-new";
    }

    @GetMapping("/clubs/my-managed")
    public String myManagedClubs(Model model,
                                 Authentication auth,
                                 Pageable pageable) {

        Page<ClubSummaryDto> clubsPage = clubService.getMyManagedClubsSummary(auth.getName(), pageable);

        model.addAttribute("clubsPage", clubsPage);
        model.addAttribute("clubs", clubsPage.getContent()); // optional

        return "my-managed-clubs";
    }

    @GetMapping("/clubs/{clubId}/memberships")
    public String membershipApplications(@PathVariable UUID clubId, Model model, Authentication auth, Pageable pageable) {

        try {
            // 1. Load club (missing in your code)
            ClubDto club = clubService.getClub(clubId);

            // 2. Get paginated applications with auth check inside service
            Page<Membership> applications =
                    membershipService.getMembershipApplications(clubId, pageable, auth.getName());

            // 3. Model attributes
            model.addAttribute("club", club);
            model.addAttribute("applicationsPage", applications);
            model.addAttribute("applications", applications.getContent()); // optional for Thymeleaf

            return "membership-applications";
        } catch (Exception e) {
            return "redirect:/dashboard";
        }
    }

    @GetMapping("/clubs/{clubId}/manage-members")
    public String manageMembers(
            @PathVariable UUID clubId,
            @RequestParam(required = false) String q,
            Model model,
            @PageableDefault Pageable pageable,
            Authentication auth
    ) {
        ClubDto club = clubService.getClub(clubId);

        if (!authorizationService.canManageClub(clubId, auth.getName())) {
            return "redirect:/dashboard";
        }

        Page<MembershipDto> members;

        if (q == null || q.isBlank()) {
            members = membershipService.getMembers(clubId, pageable, auth.getName());
        } else {
            members = membershipService.searchMembers(clubId, q, pageable, auth.getName());
        }
        model.addAttribute("club", club);
        model.addAttribute("membersPage", members);
        model.addAttribute("query", q);

        return "manage-members";
    }

    @GetMapping("/clubs/{clubId}/membership-history")
    public String membershipHistory(
            @PathVariable UUID clubId,
            Model model,
            Pageable pageable,
            Authentication auth
    ) {

        Page<MembershipDto> history =
                membershipService.getMembershipHistory(clubId, pageable, auth.getName());

        model.addAttribute("historyPage", history);
        model.addAttribute("history", history.getContent());
        model.addAttribute("club", clubService.getClub(clubId));

        return "membership-history";
    }
}
