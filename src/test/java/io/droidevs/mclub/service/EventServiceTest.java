package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.EventDto;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.EventEntityMapper;
import io.droidevs.mclub.mapper.EventMapper;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.EventRepository;
import io.droidevs.mclub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventMapper eventMapper;
    @Mock private EventEntityMapper eventEntityMapper;
    @Mock private ClubAuthorizationService clubAuthorizationService;

    @InjectMocks
    private EventService eventService;

    private UUID clubId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        clubId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    @Test
    void shouldThrow_whenClubIdMissing() {
        EventDto dto = new EventDto();
        dto.setTitle("t");
        dto.setDescription("d");
        dto.setLocation("l");
        dto.setStartDate(LocalDateTime.now());
        dto.setEndDate(LocalDateTime.now().plusHours(1));

        assertThrows(IllegalArgumentException.class, () -> eventService.createEvent(dto, "x@example.com"));
    }

    @Test
    void shouldThrow_whenEndDateBeforeStartDate() {
        EventDto dto = validDto();
        dto.setEndDate(dto.getStartDate().minusMinutes(1));

        assertThrows(IllegalArgumentException.class, () -> eventService.createEvent(dto, "x@example.com"));
    }

    @Test
    void shouldThrow_whenClubNotFound() {
        EventDto dto = validDto();
        when(clubRepository.findById(clubId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.createEvent(dto, "x@example.com"));
        verify(clubAuthorizationService, never()).requirePlatformAdminOrClubAdminOrStaff(any(), any());
    }

    @Test
    void shouldCreateEvent_andSetControlledRelations() {
        EventDto dto = validDto();
        Club club = Club.builder().id(clubId).build();
        User user = User.builder().id(UUID.randomUUID()).email("manager@example.com").build();

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(user));

        Event mapped = new Event();
        when(eventEntityMapper.toEntity(dto)).thenReturn(mapped);

        Event saved = new Event();
        saved.setId(eventId);
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventDto out = new EventDto();
        out.setId(eventId);
        when(eventMapper.toDto(saved)).thenReturn(out);

        EventDto result = eventService.createEvent(dto, "manager@example.com");

        assertEquals(eventId, result.getId());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertSame(club, captor.getValue().getClub());
        assertSame(user, captor.getValue().getCreatedBy());
        verify(clubAuthorizationService).requirePlatformAdminOrClubAdminOrStaff("manager@example.com", clubId);
    }

    @Test
    void requireCanManageEvent_shouldThrow_whenEventNotFound() {
        when(eventRepository.findByIdWithClub(eventId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.requireCanManageEvent("x@example.com", eventId));
    }

    private EventDto validDto() {
        EventDto dto = new EventDto();
        dto.setClubId(clubId);
        dto.setTitle("Title");
        dto.setDescription("Description");
        dto.setLocation("Location");
        dto.setStartDate(LocalDateTime.now().plusDays(1));
        dto.setEndDate(dto.getStartDate().plusHours(2));
        return dto;
    }
}

