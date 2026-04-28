package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.Activity;
import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.ActivityCreateRequest;
import io.droidevs.mclub.dto.ActivityDto;
import io.droidevs.mclub.dto.ActivitySummary;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.ActivityMapper;
import io.droidevs.mclub.repository.ActivityRepository;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.EventRepository;
import io.droidevs.mclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ClubRepository clubRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ActivityMapper activityMapper;
    private final ClubAuthorizationService clubAuthorizationService;

    public ActivityDto create(ActivityCreateRequest request, String email) {
        if (request.getClubId() == null) {
            throw new IllegalArgumentException("clubId is required");
        }

        Club club = clubRepository.findById(request.getClubId())
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));

        clubAuthorizationService.canManageClub(club.getId(), email);

        User user = userRepository.findByEmail(email).orElseThrow();

        Event event = null;
        if (request.getEventId() != null) {
            event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
            if (!event.getClub().getId().equals(club.getId())) {
                throw new IllegalArgumentException("eventId does not belong to this club");
            }
        }

        // MapStruct maps simple scalar fields; service sets controlled relationships.
        Activity a = activityMapper.toEntity(request);
        a.setClub(club);
        a.setEvent(event);
        a.setCreatedBy(user);

        return activityMapper.toDto(activityRepository.save(a));
    }

    @Transactional(readOnly = true)
    public List<ActivitySummary> getRecentByClub(UUID clubId) {
        return activityRepository.findTop5SummaryByClubId(
                clubId,
                PageRequest.of(0, 5)
        );
    }

    // ===============================
    // 🔹 GET PAGINATED (LIGHT)
    // ===============================
    @Transactional(readOnly = true)
    public Page<ActivitySummary> getSummaryByClub(UUID clubId, Pageable pageable) {
        return activityRepository.findSummaryByClubId(clubId, pageable);
    }



    // ===============================
    // 🔹 GET DETAILS
    // ===============================
    @Transactional(readOnly = true)
    public ActivityDto getById(UUID activityId) {
        Activity activity = activityRepository.findByIdAndDeletedFalse(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

        return activityMapper.toDto(activity);
    }

    // ===============================
    // 🔹 UPDATE
    // ===============================
    @Transactional
    public ActivityDto update(UUID activityId, ActivityDto dto, String email) {

        Activity activity = activityRepository.findByIdAndDeletedFalse(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

        clubAuthorizationService.requireClubManager(activity.getClub().getId(), email);

        // Update fields
        activity.setTitle(dto.getTitle());
        activity.setDescription(dto.getDescription());
        activity.setDate(dto.getDate());

        if (dto.getEventId() != null) {
            Event event = eventRepository.findById(dto.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

            if (!event.getClub().getId().equals(activity.getClub().getId())) {
                throw new IllegalArgumentException("Event does not belong to this club");
            }

            activity.setEvent(event);
        } else {
            activity.setEvent(null);
        }

        return activityMapper.toDto(activityRepository.save(activity));
    }

    // ===============================
    // 🔹 DELETE (SOFT)
    // ===============================
    @Transactional
    public void delete(UUID activityId, String email) {

        Activity activity = activityRepository.findByIdAndDeletedFalse(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

        clubAuthorizationService.requireClubManager(activity.getClub().getId(), email);

        activity.setDeleted(true);
        activityRepository.save(activity);
    }

    // ===============================
    // 🔹 SEARCH
    // ===============================
    @Transactional(readOnly = true)
    public Page<ActivitySummary> search(UUID clubId, String query, Pageable pageable) {
        return activityRepository.searchByClub(clubId, query, pageable);
    }

    // ===============================
    // 🔹 COUNT
    // ===============================
    public long count(UUID clubId) {
        return activityRepository.countByClubIdAndDeletedFalse(clubId);
    }
}
