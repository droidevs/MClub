package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.*;
import io.droidevs.mclub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final ClubAuthorizationService clubAuthorizationService;
    private final EventCreateRequestMapper eventCreateRequestMapper;

    public EventDto createEvent(EventCreateRequest request, String email) {
        if (request.getClubId() == null) {
            throw new IllegalArgumentException("clubId is required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (request.getLocation() == null || request.getLocation().isBlank()) {
            throw new IllegalArgumentException("location is required");
        }
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (request.getEndDate() == null) {
            throw new IllegalArgumentException("endDate is required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }

        Club club = clubRepository.findById(request.getClubId())
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));

        clubAuthorizationService.requireClubManager(club.getId(), email);


        User user = userRepository.findByEmail(email).orElseThrow();

        // MapStruct maps simple scalar fields; service sets controlled relationships.
        Event event = eventCreateRequestMapper.toEntity(request);
        event.setClub(club);
        event.setCreatedBy(user);

        return eventMapper.toDto(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public Page<EventDto> getEventsByClub(UUID clubId, Pageable pageable) {
        return eventRepository.findByClubId(clubId, pageable)
                .map(eventMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<EventDto> searchEvents(UUID clubId, String query, Pageable pageable) {
        return eventRepository.searchByClub(clubId, query, pageable)
                .map(eventMapper::toDto);
    }

    public Page<EventSummary> searchEventsLight(UUID clubId, String query, Pageable pageable) {
        return eventRepository.searchLightweightByClub(clubId, query, pageable);
    }

    @Transactional(readOnly = true)
    public List<EventDto> getRecentEventsByClub(UUID clubId) {
        // Fetch joins (club + createdBy) to avoid LazyInitializationException during mapping
        return eventRepository.findTop5ByClubIdOrderByStartDateDesc(clubId)
                .stream().map(eventMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<EventDto> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable).map(eventMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Event getEvent(UUID id) {
        return eventRepository.findByIdWithClub(id)
                .orElseGet(() -> eventRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found")));
    }

    public void requireCanManageEvent(String email, UUID eventId) {
        Event event = eventRepository.findByIdWithClub(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        clubAuthorizationService.requireClubManager(event.getClub().getId(), email);
    }
}
