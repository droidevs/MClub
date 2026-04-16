package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.AttendanceRecordDto;
import io.droidevs.mclub.dto.AttendanceWindowRequest;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.AttendanceMapper;
import io.droidevs.mclub.mapper.EventAttendanceFactoryMapper;
import io.droidevs.mclub.mapper.EventAttendanceWindowFactoryMapper;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.security.Role;
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
class AttendanceServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @Mock private EventAttendanceRepository eventAttendanceRepository;
    @Mock private EventAttendanceWindowRepository windowRepository;
    @Mock private ClubAuthorizationService clubAuthorizationService;
    @Mock private AttendanceTokenService attendanceTokenService;
    @Mock private AttendanceMapper attendanceMapper;
    @Mock private EventAttendanceFactoryMapper eventAttendanceFactoryMapper;
    @Mock private EventAttendanceWindowFactoryMapper eventAttendanceWindowFactoryMapper;

    @InjectMocks
    private AttendanceService attendanceService;

    private UUID eventId;
    private UUID clubId;

    @BeforeEach
    void init() {
        eventId = UUID.randomUUID();
        clubId = UUID.randomUUID();
    }

    @Test
    void openOrUpdateWindow_shouldHashToken_andStoreOnlyHash() {
        Event e = Event.builder().id(eventId).club(Club.builder().id(clubId).build()).startDate(LocalDateTime.now().plusHours(1)).build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(e));

        when(attendanceTokenService.generateRawToken()).thenReturn("raw");
        when(attendanceTokenService.sha256Hex("raw")).thenReturn("hash");

        when(windowRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        EventAttendanceWindow newWindow = new EventAttendanceWindow();
        when(eventAttendanceWindowFactoryMapper.create(e)).thenReturn(newWindow);

        AttendanceWindowRequest req = new AttendanceWindowRequest();
        req.setOpensBeforeStartMinutes(10);
        req.setClosesAfterStartMinutes(20);

        attendanceService.openOrUpdateWindow(eventId, req, "admin@example.com");

        ArgumentCaptor<EventAttendanceWindow> captor = ArgumentCaptor.forClass(EventAttendanceWindow.class);
        verify(windowRepository).save(captor.capture());
        assertEquals("hash", captor.getValue().getTokenHash());
        assertTrue(captor.getValue().isActive());
        // raw token must never be stored
        assertNotEquals("raw", captor.getValue().getTokenHash());
    }

    @Test
    void studentCheckInByEventQr_shouldForbid_whenNotStudent() {
        User u = User.builder().id(UUID.randomUUID()).email("x").role(Role.PLATFORM_ADMIN).build();
        when(userRepository.findByEmail("x")).thenReturn(Optional.of(u));

        assertThrows(ForbiddenException.class, () -> attendanceService.studentCheckInByEventQr("raw", "x"));
    }

    @Test
    void studentCheckInByEventQr_shouldThrow_whenTokenInvalid() {
        User student = User.builder().id(UUID.randomUUID()).email("s").role(Role.STUDENT).build();
        when(userRepository.findByEmail("s")).thenReturn(Optional.of(student));
        when(attendanceTokenService.sha256Hex("raw")).thenReturn("hash");
        when(windowRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.studentCheckInByEventQr("raw", "s"));
    }

    @Test
    void studentCheckInByEventQr_shouldForbid_whenNotRegistered() {
        User student = User.builder().id(UUID.randomUUID()).email("s").role(Role.STUDENT).build();
        Event event = Event.builder().id(eventId).club(Club.builder().id(clubId).build()).startDate(LocalDateTime.now().plusMinutes(5)).endDate(LocalDateTime.now().plusHours(2)).build();
        EventAttendanceWindow window = new EventAttendanceWindow();
        window.setActive(true);
        window.setOpensBeforeStartMinutes(60);
        window.setClosesAfterStartMinutes(60);
        window.setEvent(event);

        when(userRepository.findByEmail("s")).thenReturn(Optional.of(student));
        when(attendanceTokenService.sha256Hex("raw")).thenReturn("hash");
        when(windowRepository.findByTokenHash("hash")).thenReturn(Optional.of(window));
        when(eventRegistrationRepository.findByUserIdAndEventId(student.getId(), eventId)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> attendanceService.studentCheckInByEventQr("raw", "s"));
    }

    @Test
    void organizerCheckInStudent_shouldReturnExistingAttendance_idempotent() {
        User organizer = User.builder().id(UUID.randomUUID()).email("org").role(Role.PLATFORM_ADMIN).build();
        User student = User.builder().id(UUID.randomUUID()).email("s").role(Role.STUDENT).build();
        Event event = Event.builder().id(eventId).club(Club.builder().id(clubId).build()).startDate(LocalDateTime.now().plusMinutes(5)).endDate(LocalDateTime.now().plusHours(2)).build();

        EventAttendanceWindow window = new EventAttendanceWindow();
        window.setActive(true);
        window.setOpensBeforeStartMinutes(60);
        window.setClosesAfterStartMinutes(60);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(window));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(eventRegistrationRepository.findByUserIdAndEventId(student.getId(), eventId)).thenReturn(Optional.of(new EventRegistration()));

        EventAttendance existing = new EventAttendance();
        when(eventAttendanceRepository.findByEventIdAndUserId(eventId, student.getId())).thenReturn(Optional.of(existing));
        AttendanceRecordDto dto = new AttendanceRecordDto();
        when(attendanceMapper.toDto(existing)).thenReturn(dto);

        AttendanceRecordDto out = attendanceService.organizerCheckInStudent(eventId, student.getId(), organizer.getEmail());
        assertSame(dto, out);
        verify(eventAttendanceRepository, never()).save(any());
    }
}

