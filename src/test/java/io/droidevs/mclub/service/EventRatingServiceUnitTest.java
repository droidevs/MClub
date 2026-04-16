package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.EventRating;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.EventRatingDto;
import io.droidevs.mclub.dto.EventRatingRequest;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.EventRatingFactoryMapper;
import io.droidevs.mclub.mapper.EventRatingMapper;
import io.droidevs.mclub.repository.EventRatingRepository;
import io.droidevs.mclub.repository.EventRegistrationRepository;
import io.droidevs.mclub.repository.EventRepository;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRatingServiceUnitTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @Mock private EventRatingRepository eventRatingRepository;
    @Mock private UserRepository userRepository;
    @Mock private AttendanceService attendanceService;
    @Mock private EventRatingMapper eventRatingMapper;
    @Mock private EventRatingFactoryMapper eventRatingFactoryMapper;

    @InjectMocks
    private EventRatingService service;

    private UUID eventId;
    private User student;

    @BeforeEach
    void init() {
        eventId = UUID.randomUUID();
        student = User.builder().id(UUID.randomUUID()).email("s@example.com").role(Role.STUDENT).build();
    }

    @Test
    void shouldForbid_whenNonStudentRates() {
        User admin = User.builder().id(UUID.randomUUID()).email("a").role(Role.PLATFORM_ADMIN).build();
        when(userRepository.findByEmail("a")).thenReturn(Optional.of(admin));

        assertThrows(ForbiddenException.class, () -> service.rateEvent(eventId, req(5), "a"));
    }

    @Test
    void shouldThrow_whenEventNotFound() {
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.rateEvent(eventId, req(5), student.getEmail()));
    }

    @Test
    void shouldForbid_whenNotRegistered() {
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(Event.builder().id(eventId).build()));
        when(eventRegistrationRepository.findByUserIdAndEventId(student.getId(), eventId)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> service.rateEvent(eventId, req(5), student.getEmail()));
    }

    @Test
    void shouldForbid_whenNotAttended() {
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(Event.builder().id(eventId).build()));
        when(eventRegistrationRepository.findByUserIdAndEventId(student.getId(), eventId)).thenReturn(Optional.of(new io.droidevs.mclub.domain.EventRegistration()));
        when(attendanceService.hasAttended(eventId, student.getId())).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.rateEvent(eventId, req(5), student.getEmail()));
    }

    @Test
    void shouldCreateNewRating_whenNoneExists_andUpsert() {
        Event event = Event.builder().id(eventId).build();
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.findByUserIdAndEventId(student.getId(), eventId)).thenReturn(Optional.of(new io.droidevs.mclub.domain.EventRegistration()));
        when(attendanceService.hasAttended(eventId, student.getId())).thenReturn(true);
        when(eventRatingRepository.findByEventIdAndStudentId(eventId, student.getId())).thenReturn(Optional.empty());

        EventRating newRating = new EventRating();
        when(eventRatingFactoryMapper.create()).thenReturn(newRating);
        when(eventRatingRepository.save(newRating)).thenReturn(newRating);

        EventRatingDto dto = new EventRatingDto();
        when(eventRatingMapper.toDto(newRating)).thenReturn(dto);

        EventRatingDto out = service.rateEvent(eventId, req(4), student.getEmail());
        assertSame(dto, out);
        assertSame(event, newRating.getEvent());
        assertSame(student, newRating.getStudent());
        assertEquals(4, newRating.getRating());
    }

    private static EventRatingRequest req(int r) {
        EventRatingRequest req = new EventRatingRequest();
        req.setRating(r);
        req.setComment("ok");
        return req;
    }
}

