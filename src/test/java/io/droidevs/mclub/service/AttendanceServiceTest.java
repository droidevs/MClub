package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.AttendanceEventQrDto;
import io.droidevs.mclub.dto.AttendanceRecordDto;
import io.droidevs.mclub.dto.AttendanceWindowRequest;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.AttendanceMapper;
import io.droidevs.mclub.mapper.EventAttendanceFactoryMapper;
import io.droidevs.mclub.mapper.EventAttendanceWindowFactoryMapper;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.security.Role;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AttendanceService}.
 *
 * <p>Conventions used:
 * <ul>
 *   <li>AssertJ for fluent, readable assertions.</li>
 *   <li>Mockito strict stubs (via MockitoExtension) to catch unused stubs early.</li>
 *   <li>Test method names follow the pattern:
 *       {@code methodName_shouldExpectedBehavior_whenCondition}.</li>
 *   <li>Each test has a single, well-named "Arrange / Act / Assert" structure.</li>
 *   <li>Builder-style factory helpers keep boilerplate out of tests.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService")
class AttendanceServiceTest {

    // -------------------------------------------------------------------------
    // Mocks
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Shared fixtures
    // -------------------------------------------------------------------------

    private static final String ORGANIZER_EMAIL  = "organizer@club.io";
    private static final String STUDENT_EMAIL    = "student@club.io";
    private static final String RAW_TOKEN        = "raw-token-abc123";
    private static final String TOKEN_HASH       = "hashed-token-abc123";

    private UUID eventId;
    private UUID clubId;
    private UUID studentId;

    private Club  club;
    private Event futureEvent;   // starts in the future, well within time windows
    private User  studentUser;
    private User  organizerUser;

    @BeforeEach
    void setUp() {
        eventId   = UUID.randomUUID();
        clubId    = UUID.randomUUID();
        studentId = UUID.randomUUID();

        club = Club.builder().id(clubId).build();

        futureEvent = Event.builder()
                .id(eventId)
                .club(club)
                .startDate(LocalDateTime.now().plusMinutes(10))
                .endDate(LocalDateTime.now().plusHours(2))
                .build();

        studentUser = User.builder()
                .id(studentId)
                .email(STUDENT_EMAIL)
                .role(Role.STUDENT)
                .build();

        organizerUser = User.builder()
                .id(UUID.randomUUID())
                .email(ORGANIZER_EMAIL)
                .role(Role.PLATFORM_ADMIN)   // CLUB_MANAGER is not a platform-level Role value
                .build();
    }

    // =========================================================================
    // openOrUpdateWindow
    // =========================================================================

    @Nested
    @DisplayName("openOrUpdateWindow()")
    class OpenOrUpdateWindow {

        @Test
        @DisplayName("should store only the token hash – never the raw token")
        void shouldHashToken_andStoreOnlyHash() {
            // Arrange
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(attendanceTokenService.generateRawToken()).thenReturn(RAW_TOKEN);
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.empty());

            EventAttendanceWindow newWindow = new EventAttendanceWindow();
            when(eventAttendanceWindowFactoryMapper.create(futureEvent, 10, 20)).thenReturn(newWindow);

            AttendanceWindowRequest request = buildWindowRequest(10, 20);

            // Act
            attendanceService.openOrUpdateWindow(eventId, request, ORGANIZER_EMAIL);

            // Assert
            ArgumentCaptor<EventAttendanceWindow> captor = ArgumentCaptor.forClass(EventAttendanceWindow.class);
            verify(windowRepository).save(captor.capture());

            EventAttendanceWindow saved = captor.getValue();
            assertThat(saved.getTokenHash())
                    .as("Only the hash must be persisted")
                    .isEqualTo(TOKEN_HASH)
                    .isNotEqualTo(RAW_TOKEN);
        }

        @Test
        @DisplayName("should activate the window when opening")
        void shouldSetWindowActive_whenOpening() {
            // Arrange
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(attendanceTokenService.generateRawToken()).thenReturn(RAW_TOKEN);
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.empty());

            EventAttendanceWindow newWindow = new EventAttendanceWindow();
            when(eventAttendanceWindowFactoryMapper.create(futureEvent, 10, 20)).thenReturn(newWindow);

            // Act
            attendanceService.openOrUpdateWindow(eventId, buildWindowRequest(10, 20), ORGANIZER_EMAIL);

            // Assert
            ArgumentCaptor<EventAttendanceWindow> captor = ArgumentCaptor.forClass(EventAttendanceWindow.class);
            verify(windowRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isTrue();
        }

        @Test
        @DisplayName("should return the raw token in the DTO (so it can be embedded in a QR code)")
        void shouldReturnRawToken_inDto() {
            // Arrange
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(attendanceTokenService.generateRawToken()).thenReturn(RAW_TOKEN);
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.empty());

            EventAttendanceWindow newWindow = new EventAttendanceWindow();
            when(eventAttendanceWindowFactoryMapper.create(futureEvent, 10, 20)).thenReturn(newWindow);

            // Act
            AttendanceEventQrDto dto = attendanceService.openOrUpdateWindow(eventId, buildWindowRequest(10, 20), ORGANIZER_EMAIL);

            // Assert
            assertThat(dto.getToken()).isEqualTo(RAW_TOKEN);
            assertThat(dto.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("should reuse existing window and rotate its token when one already exists")
        void shouldReuseExistingWindow_andRotateToken() {
            // Arrange
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(attendanceTokenService.generateRawToken()).thenReturn(RAW_TOKEN);
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);

            EventAttendanceWindow existingWindow = new EventAttendanceWindow();
            existingWindow.setActive(false);
            existingWindow.setTokenHash("old-hash");
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(existingWindow));

            // Act
            attendanceService.openOrUpdateWindow(eventId, buildWindowRequest(15, 30), ORGANIZER_EMAIL);

            // Assert – factory mapper must NOT be called; the existing window is reused
            verify(eventAttendanceWindowFactoryMapper, never()).create(any(), anyInt(), anyInt());

            ArgumentCaptor<EventAttendanceWindow> captor = ArgumentCaptor.forClass(EventAttendanceWindow.class);
            verify(windowRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash()).isEqualTo(TOKEN_HASH);
        }

        @Test
        @DisplayName("should set tokenRotatedAt to now when opening")
        void shouldSetTokenRotatedAt() {
            // Arrange
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(attendanceTokenService.generateRawToken()).thenReturn(RAW_TOKEN);
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.empty());
            EventAttendanceWindow newWindow = new EventAttendanceWindow();
            when(eventAttendanceWindowFactoryMapper.create(futureEvent, 10, 20)).thenReturn(newWindow);

            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // Act
            attendanceService.openOrUpdateWindow(eventId, buildWindowRequest(10, 20), ORGANIZER_EMAIL);

            // Assert
            ArgumentCaptor<EventAttendanceWindow> captor = ArgumentCaptor.forClass(EventAttendanceWindow.class);
            verify(windowRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenRotatedAt()).isAfter(before);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when event does not exist")
        void shouldThrow_whenEventNotFound() {
            // Arrange
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    attendanceService.openOrUpdateWindow(eventId, buildWindowRequest(10, 20), ORGANIZER_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should delegate authorization check to ClubAuthorizationService")
        void shouldDelegateAuthorizationCheck() {
            // Arrange – minimal stubs: auth is called after getEvent(), so we only need the event
            // and enough stubs to reach the save() call without NPE.
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(attendanceTokenService.generateRawToken()).thenReturn(RAW_TOKEN);
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.empty());
            EventAttendanceWindow newWindow = new EventAttendanceWindow();
            when(eventAttendanceWindowFactoryMapper.create(futureEvent, 10, 20)).thenReturn(newWindow);

            // Act
            attendanceService.openOrUpdateWindow(eventId, buildWindowRequest(10, 20), ORGANIZER_EMAIL);

            // Assert
            verify(clubAuthorizationService).requireClubManager(clubId, ORGANIZER_EMAIL);
        }
    }

    // =========================================================================
    // closeWindow
    // =========================================================================

    @Nested
    @DisplayName("closeWindow()")
    class CloseWindow {

        @Test
        @DisplayName("should deactivate an active window")
        void shouldDeactivateWindow() {
            // Arrange
            EventAttendanceWindow window = activeWindow(futureEvent);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(window));

            // Act
            attendanceService.closeWindow(eventId, ORGANIZER_EMAIL);

            // Assert
            ArgumentCaptor<EventAttendanceWindow> captor = ArgumentCaptor.forClass(EventAttendanceWindow.class);
            verify(windowRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when no window exists for the event")
        void shouldThrow_whenWindowNotFound() {
            // Arrange
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.closeWindow(eventId, ORGANIZER_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when event does not exist")
        void shouldThrow_whenEventNotFound() {
            // Arrange
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.closeWindow(eventId, ORGANIZER_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should delegate authorization check to ClubAuthorizationService")
        void shouldDelegateAuthorizationCheck() {
            // Arrange
            EventAttendanceWindow window = activeWindow(futureEvent);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(window));

            // Act
            attendanceService.closeWindow(eventId, ORGANIZER_EMAIL);

            // Assert
            verify(clubAuthorizationService).requireClubManager(clubId, ORGANIZER_EMAIL);
        }
    }

    // =========================================================================
    // studentCheckInByEventQr
    // =========================================================================

    @Nested
    @DisplayName("studentCheckInByEventQr()")
    class StudentCheckInByEventQr {

        @ParameterizedTest(name = "should throw ForbiddenException when caller has role={0}")
        @EnumSource(value = Role.class, names = {"PLATFORM_ADMIN"})
        void shouldForbid_whenCallerIsNotStudent(Role role) {
            // Arrange
            User nonStudent = User.builder().id(UUID.randomUUID()).email(STUDENT_EMAIL).role(role).build();
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(nonStudent));

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("student");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when token hash matches no window")
        void shouldThrow_whenTokenInvalid() {
            // Arrange
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when attendance window is closed")
        void shouldForbid_whenWindowIsClosed() {
            // Arrange
            EventAttendanceWindow closedWindow = windowWithConfig(futureEvent, false, 60, 60);
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(closedWindow));

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("closed");
        }

        @Test
        @DisplayName("should throw ForbiddenException when student is not registered for the event")
        void shouldForbid_whenStudentNotRegistered() {
            // Arrange
            EventAttendanceWindow window = activeWindow(futureEvent);
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(window));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, eventId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("registered");
        }

        @Test
        @DisplayName("should throw ForbiddenException when check-in time is before the open window")
        void shouldForbid_whenBeforeOpenWindow() {
            // Arrange: window opens 5 min before start, but start is 30 min away → now < openAt
            Event lateEvent = eventWithStartIn(30);
            EventAttendanceWindow window = windowWithConfig(lateEvent, true, 5, 60);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(window));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, lateEvent.getId()))
                    .thenReturn(Optional.of(new EventRegistration()));

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("not allowed at this time");
        }

        @Test
        @DisplayName("should throw ForbiddenException when check-in time is after the close window")
        void shouldForbid_whenAfterCloseWindow() {
            // Arrange: event started 90 min ago, window closes 60 min after start → now > closeAt
            Event pastEvent = eventWithStartIn(-90);
            EventAttendanceWindow window = windowWithConfig(pastEvent, true, 60, 60);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(window));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, pastEvent.getId()))
                    .thenReturn(Optional.of(new EventRegistration()));

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("not allowed at this time");
        }

        @Test
        @DisplayName("should throw ForbiddenException when event has no startDate configured")
        void shouldForbid_whenEventHasNoStartDate() {
            // Arrange
            Event noStartEvent = Event.builder().id(eventId).club(club).startDate(null).build();
            EventAttendanceWindow window = windowWithConfig(noStartEvent, true, 60, 60);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(window));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, eventId))
                    .thenReturn(Optional.of(new EventRegistration()));

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("start time");
        }

        @Test
        @DisplayName("should return existing attendance record without saving again (idempotent)")
        void shouldReturnExistingAttendance_idempotent() {
            // Arrange
            EventAttendanceWindow window = activeWindow(futureEvent);
            EventAttendance existing = new EventAttendance();
            AttendanceRecordDto expectedDto = new AttendanceRecordDto();

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(window));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, eventId))
                    .thenReturn(Optional.of(new EventRegistration()));
            when(eventAttendanceRepository.findByEventIdAndUserId(eventId, studentId))
                    .thenReturn(Optional.of(existing));
            when(attendanceMapper.toDto(existing)).thenReturn(expectedDto);

            // Act
            AttendanceRecordDto result = attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL);

            // Assert
            assertThat(result).isSameAs(expectedDto);
            verify(eventAttendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create a new attendance record with method STUDENT_SCANNED_EVENT_QR")
        void shouldCreateNewAttendance_withCorrectMethod() {
            // Arrange
            EventAttendanceWindow window = activeWindow(futureEvent);
            EventAttendance newRecord = new EventAttendance();
            AttendanceRecordDto expectedDto = new AttendanceRecordDto();

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(window));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, eventId))
                    .thenReturn(Optional.of(new EventRegistration()));
            when(eventAttendanceRepository.findByEventIdAndUserId(eventId, studentId))
                    .thenReturn(Optional.empty());
            when(eventAttendanceFactoryMapper.create(futureEvent, studentUser, AttendanceMethod.STUDENT_SCANNED_EVENT_QR))
                    .thenReturn(newRecord);
            when(eventAttendanceRepository.save(newRecord)).thenReturn(newRecord);
            when(attendanceMapper.toDto(newRecord)).thenReturn(expectedDto);

            // Act
            AttendanceRecordDto result = attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL);

            // Assert
            assertThat(result).isSameAs(expectedDto);
            verify(eventAttendanceFactoryMapper).create(futureEvent, studentUser, AttendanceMethod.STUDENT_SCANNED_EVENT_QR);
            assertThat(newRecord.getCheckedInBy()).isNull(); // self check-in: no organizer
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when student user does not exist")
        void shouldThrow_whenStudentUserNotFound() {
            // Arrange
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // organizerCheckInStudent
    // =========================================================================

    @Nested
    @DisplayName("organizerCheckInStudent()")
    class OrganizerCheckInStudent {

        @Test
        @DisplayName("should return existing attendance record without saving again (idempotent)")
        void shouldReturnExistingAttendance_idempotent() {
            // Arrange
            EventAttendanceWindow window = activeWindow(futureEvent);
            EventAttendance existing = new EventAttendance();
            AttendanceRecordDto expectedDto = new AttendanceRecordDto();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(window));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, eventId))
                    .thenReturn(Optional.of(new EventRegistration()));
            when(eventAttendanceRepository.findByEventIdAndUserId(eventId, studentId))
                    .thenReturn(Optional.of(existing));
            when(attendanceMapper.toDto(existing)).thenReturn(expectedDto);

            // Act
            AttendanceRecordDto result = attendanceService.organizerCheckInStudent(eventId, studentId, ORGANIZER_EMAIL);

            // Assert
            assertThat(result).isSameAs(expectedDto);
            verify(eventAttendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create a new attendance record with method ADMIN_SCANNED_STUDENT_QR")
        void shouldCreateNewAttendance_withCorrectMethod() {
            // Arrange
            EventAttendanceWindow window = activeWindow(futureEvent);
            EventAttendance newRecord = new EventAttendance();
            AttendanceRecordDto expectedDto = new AttendanceRecordDto();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(window));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, eventId))
                    .thenReturn(Optional.of(new EventRegistration()));
            when(eventAttendanceRepository.findByEventIdAndUserId(eventId, studentId))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(organizerUser));
            when(eventAttendanceFactoryMapper.create(futureEvent, studentUser, AttendanceMethod.ADMIN_SCANNED_STUDENT_QR))
                    .thenReturn(newRecord);
            when(eventAttendanceRepository.save(newRecord)).thenReturn(newRecord);
            when(attendanceMapper.toDto(newRecord)).thenReturn(expectedDto);

            // Act
            AttendanceRecordDto result = attendanceService.organizerCheckInStudent(eventId, studentId, ORGANIZER_EMAIL);

            // Assert
            assertThat(result).isSameAs(expectedDto);
            verify(eventAttendanceFactoryMapper)
                    .create(futureEvent, studentUser, AttendanceMethod.ADMIN_SCANNED_STUDENT_QR);
            assertThat(newRecord.getCheckedInBy()).isSameAs(organizerUser);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when event does not exist")
        void shouldThrow_whenEventNotFound() {
            // Arrange
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    attendanceService.organizerCheckInStudent(eventId, studentId, ORGANIZER_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when attendance window does not exist")
        void shouldThrow_whenWindowNotFound() {
            // Arrange
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    attendanceService.organizerCheckInStudent(eventId, studentId, ORGANIZER_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when attendance window is closed")
        void shouldForbid_whenWindowClosed() {
            // Arrange
            EventAttendanceWindow closedWindow = windowWithConfig(futureEvent, false, 60, 60);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(closedWindow));

            // Act & Assert
            assertThatThrownBy(() ->
                    attendanceService.organizerCheckInStudent(eventId, studentId, ORGANIZER_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("closed");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when student user does not exist")
        void shouldThrow_whenStudentNotFound() {
            // Arrange
            EventAttendanceWindow window = activeWindow(futureEvent);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(window));
            when(userRepository.findById(studentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    attendanceService.organizerCheckInStudent(eventId, studentId, ORGANIZER_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @ParameterizedTest(name = "should throw ForbiddenException when target has role={0}")
        @EnumSource(value = Role.class, names = {"PLATFORM_ADMIN"})
        void shouldForbid_whenTargetIsNotStudent(Role role) {
            // Arrange
            User nonStudent = User.builder().id(studentId).email("other@mail.io").role(role).build();
            EventAttendanceWindow window = activeWindow(futureEvent);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(window));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(nonStudent));

            // Act & Assert
            assertThatThrownBy(() ->
                    attendanceService.organizerCheckInStudent(eventId, studentId, ORGANIZER_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("student");
        }

        @Test
        @DisplayName("should throw ForbiddenException when student is not registered for the event")
        void shouldForbid_whenStudentNotRegistered() {
            // Arrange
            EventAttendanceWindow window = activeWindow(futureEvent);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(window));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, eventId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    attendanceService.organizerCheckInStudent(eventId, studentId, ORGANIZER_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("registered");
        }

        @Test
        @DisplayName("should throw ForbiddenException when check-in is outside the time window")
        void shouldForbid_whenOutsideTimeWindow() {
            // Arrange: event started 2 hours ago, window was only 30 min after start → expired
            Event pastEvent = eventWithStartIn(-120);
            EventAttendanceWindow window = windowWithConfig(pastEvent, true, 60, 30);

            when(eventRepository.findById(pastEvent.getId())).thenReturn(Optional.of(pastEvent));
            when(windowRepository.findByEventId(pastEvent.getId())).thenReturn(Optional.of(window));

            // Act & Assert
            assertThatThrownBy(() ->
                    attendanceService.organizerCheckInStudent(pastEvent.getId(), studentId, ORGANIZER_EMAIL))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when close time exceeds event endDate by more than 5 min")
        void shouldForbid_whenCloseWindowExceedsEventEndDate() {
            // Arrange: event starts now, ends in 10 min, but window closes 30 min after start
            Event shortEvent = Event.builder()
                    .id(eventId).club(club)
                    .startDate(LocalDateTime.now().minusMinutes(1))
                    .endDate(LocalDateTime.now().plusMinutes(10))
                    .build();
            EventAttendanceWindow window = windowWithConfig(shortEvent, true, 60, 30); // closeAt = start + 30 → 29 min after now > endDate + 5

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(shortEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(window));



            // Act & Assert
            assertThatThrownBy(() ->
                    attendanceService.organizerCheckInStudent(eventId, studentId, ORGANIZER_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("end time");
        }

        @Test
        @DisplayName("should delegate authorization check to ClubAuthorizationService")
        void shouldDelegateAuthorizationCheck() {
            // Arrange
            EventAttendanceWindow window = activeWindow(futureEvent);
            EventAttendance existing = new EventAttendance();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(windowRepository.findByEventId(eventId)).thenReturn(Optional.of(window));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, eventId))
                    .thenReturn(Optional.of(new EventRegistration()));
            when(eventAttendanceRepository.findByEventIdAndUserId(eventId, studentId))
                    .thenReturn(Optional.of(existing));
            when(attendanceMapper.toDto(any(EventAttendance.class))).thenReturn(new AttendanceRecordDto());

            // Act
            attendanceService.organizerCheckInStudent(eventId, studentId, ORGANIZER_EMAIL);

            // Assert
            verify(clubAuthorizationService).requireClubManager(clubId, ORGANIZER_EMAIL);
        }
    }

    // =========================================================================
    // listAttendance
    // =========================================================================

    @Nested
    @DisplayName("listAttendance()")
    class ListAttendance {

        @Test
        @DisplayName("should return paginated attendance records mapped to DTOs")
        void shouldReturnMappedDtoPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            EventAttendance record = new EventAttendance();
            AttendanceRecordDto dto = new AttendanceRecordDto();
            Page<EventAttendance> attendancePage = new PageImpl<>(List.of(record));

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(eventAttendanceRepository.findByEventId(eventId, pageable)).thenReturn(attendancePage);
            when(attendanceMapper.toDto(record)).thenReturn(dto);

            // Act
            Page<AttendanceRecordDto> result = attendanceService.listAttendance(eventId, pageable, ORGANIZER_EMAIL);

            // Assert
            assertThat(result.getContent()).containsExactly(dto);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return an empty page when there are no attendance records")
        void shouldReturnEmptyPage_whenNoAttendance() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(eventAttendanceRepository.findByEventId(eventId, pageable))
                    .thenReturn(Page.empty());

            // Act
            Page<AttendanceRecordDto> result = attendanceService.listAttendance(eventId, pageable, ORGANIZER_EMAIL);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when event does not exist")
        void shouldThrow_whenEventNotFound() {
            // Arrange
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    attendanceService.listAttendance(eventId, Pageable.unpaged(), ORGANIZER_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should delegate authorization check to ClubAuthorizationService")
        void shouldDelegateAuthorizationCheck() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(futureEvent));
            when(eventAttendanceRepository.findByEventId(eventId, pageable)).thenReturn(Page.empty());

            // Act
            attendanceService.listAttendance(eventId, pageable, ORGANIZER_EMAIL);

            // Assert
            verify(clubAuthorizationService).requireClubManager(clubId, ORGANIZER_EMAIL);
        }
    }

    // =========================================================================
    // countAttendance
    // =========================================================================

    @Nested
    @DisplayName("countAttendance()")
    class CountAttendance {

        @Test
        @DisplayName("should delegate directly to repository and return the count")
        void shouldReturnCountFromRepository() {
            // Arrange
            when(eventAttendanceRepository.countByEventId(eventId)).thenReturn(7L);

            // Act
            long count = attendanceService.countAttendance(eventId);

            // Assert
            assertThat(count).isEqualTo(7L);
        }

        @Test
        @DisplayName("should return zero when no students have attended")
        void shouldReturnZero_whenNoAttendees() {
            // Arrange
            when(eventAttendanceRepository.countByEventId(eventId)).thenReturn(0L);

            // Act & Assert
            assertThat(attendanceService.countAttendance(eventId)).isZero();
        }
    }

    // =========================================================================
    // hasAttended
    // =========================================================================

    @Nested
    @DisplayName("hasAttended()")
    class HasAttended {

        @Test
        @DisplayName("should return true when an attendance record exists")
        void shouldReturnTrue_whenAttendanceExists() {
            // Arrange
            when(eventAttendanceRepository.findByEventIdAndUserId(eventId, studentId))
                    .thenReturn(Optional.of(new EventAttendance()));

            // Act & Assert
            assertThat(attendanceService.hasAttended(eventId, studentId)).isTrue();
        }

        @Test
        @DisplayName("should return false when no attendance record exists")
        void shouldReturnFalse_whenNoAttendance() {
            // Arrange
            when(eventAttendanceRepository.findByEventIdAndUserId(eventId, studentId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThat(attendanceService.hasAttended(eventId, studentId)).isFalse();
        }
    }

    // =========================================================================
    // validateTimeWindow (exercised indirectly via check-in methods)
    // =========================================================================

    @Nested
    @DisplayName("validateTimeWindow() – edge cases")
    class ValidateTimeWindow {

        @Test
        @DisplayName("should allow check-in exactly at the open boundary")
        void shouldAllow_atExactOpenBoundary() {
            // Arrange: window opens 60 min before start; start is exactly 60 min from now
            Event event = eventWithStartIn(60);
            EventAttendanceWindow window = windowWithConfig(event, true, 60, 120);
            EventAttendance newRecord = new EventAttendance();
            AttendanceRecordDto dto = new AttendanceRecordDto();

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(window));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, event.getId()))
                    .thenReturn(Optional.of(new EventRegistration()));
            when(eventAttendanceRepository.findByEventIdAndUserId(event.getId(), studentId))
                    .thenReturn(Optional.empty());
            when(eventAttendanceFactoryMapper.create(event, studentUser, AttendanceMethod.STUDENT_SCANNED_EVENT_QR))
                    .thenReturn(newRecord);
            when(eventAttendanceRepository.save(newRecord)).thenReturn(newRecord);
            when(attendanceMapper.toDto(newRecord)).thenReturn(dto);

            // Act & Assert – should not throw
            assertThatNoException().isThrownBy(() ->
                    attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL));
        }

        @Test
        @DisplayName("should allow check-in within 5-minute grace period past event endDate")
        void shouldAllow_withinGracePeriodOfEndDate() {
            // Arrange: event started 10 min ago, ends in 3 min; window closes 20 min after start
            // closeAt = start + 20 = now + 10; endDate = now + 3
            // closeAt (now+10) > endDate+5 (now+8) → FORBIDDEN
            // …so let's test the boundary where it IS within grace: closeAt <= endDate + 5
            Event event = Event.builder()
                    .id(eventId).club(club)
                    .startDate(LocalDateTime.now().minusMinutes(10))
                    .endDate(LocalDateTime.now().plusMinutes(7))  // endDate+5 = now+12 >= closeAt(now+10) → allowed
                    .build();
            EventAttendanceWindow window = windowWithConfig(event, true, 60, 20);
            EventAttendance newRecord = new EventAttendance();
            AttendanceRecordDto dto = new AttendanceRecordDto();

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(attendanceTokenService.sha256Hex(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(windowRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(window));
            when(eventRegistrationRepository.findByUserIdAndEventId(studentId, eventId))
                    .thenReturn(Optional.of(new EventRegistration()));
            when(eventAttendanceRepository.findByEventIdAndUserId(eventId, studentId))
                    .thenReturn(Optional.empty());
            when(eventAttendanceFactoryMapper.create(event, studentUser, AttendanceMethod.STUDENT_SCANNED_EVENT_QR))
                    .thenReturn(newRecord);
            when(eventAttendanceRepository.save(newRecord)).thenReturn(newRecord);
            when(attendanceMapper.toDto(newRecord)).thenReturn(dto);

            // Act & Assert – should not throw
            assertThatNoException().isThrownBy(() ->
                    attendanceService.studentCheckInByEventQr(RAW_TOKEN, STUDENT_EMAIL));
        }
    }

    // =========================================================================
    // Private factory / helper methods
    // =========================================================================

    /** Creates an {@link AttendanceWindowRequest} with the given minute offsets. */
    private static AttendanceWindowRequest buildWindowRequest(int opensBefore, int closesAfter) {
        AttendanceWindowRequest req = new AttendanceWindowRequest();
        req.setOpensBeforeStartMinutes(opensBefore);
        req.setClosesAfterStartMinutes(closesAfter);
        return req;
    }

    /**
     * Builds an active {@link EventAttendanceWindow} with a generous ±60 minute time frame
     * so that the default {@code futureEvent} (starting in 10 min) is well within range.
     */
    private EventAttendanceWindow activeWindow(Event event) {
        int after = event.getEndDate() != null
                ? (int) ChronoUnit.MINUTES.between(event.getStartDate(), event.getEndDate())
                : 120;
        return windowWithConfig(event, true, 60, after);
    }

    /** Builds a configurable {@link EventAttendanceWindow}. */
    private EventAttendanceWindow windowWithConfig(Event event, boolean active,
                                                   int opensBefore, int closesAfter) {
        EventAttendanceWindow w = new EventAttendanceWindow();
        w.setEvent(event);
        w.setActive(active);
        w.setOpensBeforeStartMinutes(opensBefore);
        w.setClosesAfterStartMinutes(closesAfter);
        return w;
    }

    /** Creates a fresh {@link Event} whose startDate is {@code minutesFromNow} in the future (negative = past). */
    private Event eventWithStartIn(int minutesFromNow) {
        UUID id = UUID.randomUUID();
        return Event.builder()
                .id(id)
                .club(club)
                .startDate(LocalDateTime.now().plusMinutes(minutesFromNow))
                .endDate(LocalDateTime.now().plusMinutes(minutesFromNow + 120))
                .build();
    }
}