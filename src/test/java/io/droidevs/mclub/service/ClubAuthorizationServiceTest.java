package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.repository.MembershipRepository;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClubAuthorizationService}.
 *
 * <p>Conventions:
 * <ul>
 *   <li>AssertJ for fluent, readable assertions.</li>
 *   <li>Mockito strict stubs via {@code MockitoExtension} – unused stubs fail fast.</li>
 *   <li>One {@code @Nested} class per public method.</li>
 *   <li>Test names follow {@code shouldExpectedBehavior_whenCondition}.</li>
 *   <li>Private factory helpers keep arrange-blocks lean.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClubAuthorizationService")
class ClubAuthorizationServiceTest {

    // -------------------------------------------------------------------------
    // Mocks & SUT
    // -------------------------------------------------------------------------

    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;

    @InjectMocks
    private ClubAuthorizationService service;

    // -------------------------------------------------------------------------
    // Shared fixtures
    // -------------------------------------------------------------------------

    private static final String ADMIN_EMAIL   = "platform-admin@mclub.io";
    private static final String STUDENT_EMAIL = "student@mclub.io";

    private UUID clubId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        clubId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // =========================================================================
    // requireClubManager()
    // =========================================================================

    @Nested
    @DisplayName("requireClubManager()")
    class RequireClubManager {

        @Test
        @DisplayName("should not throw when caller is a PLATFORM_ADMIN")
        void shouldNotThrow_whenPlatformAdmin() {
            // Arrange
            stubUser(ADMIN_EMAIL, Role.PLATFORM_ADMIN);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> service.requireClubManager(clubId, ADMIN_EMAIL));
        }

        @Test
        @DisplayName("should not call membershipRepository when caller is a PLATFORM_ADMIN")
        void shouldSkipMembershipLookup_whenPlatformAdmin() {
            // Arrange
            stubUser(ADMIN_EMAIL, Role.PLATFORM_ADMIN);

            // Act
            service.requireClubManager(clubId, ADMIN_EMAIL);

            // Assert – short-circuit: membership table must never be queried
            verify(membershipRepository, never()).findByUserIdAndClubId(any(), any());
        }

        @Test
        @DisplayName("should not throw when caller is an approved club ADMIN")
        void shouldNotThrow_whenApprovedClubAdmin() {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            stubMembership(user, ClubRole.ADMIN, MembershipStatus.APPROVED);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> service.requireClubManager(clubId, STUDENT_EMAIL));
        }

        @Test
        @DisplayName("should not throw when caller is an approved club STAFF")
        void shouldNotThrow_whenApprovedClubStaff() {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            stubMembership(user, ClubRole.STAFF, MembershipStatus.APPROVED);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> service.requireClubManager(clubId, STUDENT_EMAIL));
        }

        @Test
        @DisplayName("should throw ForbiddenException when caller is an approved club MEMBER")
        void shouldThrow_whenApprovedClubMember() {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            stubMembership(user, ClubRole.MEMBER, MembershipStatus.APPROVED);

            // Act & Assert
            assertThatThrownBy(() -> service.requireClubManager(clubId, STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Not allowed");
        }

        @Test
        @DisplayName("should throw ForbiddenException when club ADMIN membership is PENDING (not approved)")
        void shouldThrow_whenClubAdminMembershipIsPending() {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            stubMembership(user, ClubRole.ADMIN, MembershipStatus.PENDING);

            // Act & Assert
            assertThatThrownBy(() -> service.requireClubManager(clubId, STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when club STAFF membership is REJECTED")
        void shouldThrow_whenClubStaffMembershipIsRejected() {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            stubMembership(user, ClubRole.STAFF, MembershipStatus.REJECTED);

            // Act & Assert
            assertThatThrownBy(() -> service.requireClubManager(clubId, STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no membership in the club at all")
        void shouldThrow_whenNoMembershipExists() {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            when(membershipRepository.findByUserIdAndClubId(user.getId(), clubId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.requireClubManager(clubId, STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when user email is not found")
        void shouldThrow_whenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.empty());

            // Act & Assert – orElseThrow() with no argument throws NoSuchElementException
            assertThatThrownBy(() -> service.requireClubManager(clubId, STUDENT_EMAIL))
                    .isInstanceOf(java.util.NoSuchElementException.class);
        }
    }

    // =========================================================================
    // canManageClub()
    // =========================================================================

    @Nested
    @DisplayName("canManageClub()")
    class CanManageClub {

        @Test
        @DisplayName("should return true for a PLATFORM_ADMIN regardless of membership")
        void shouldReturnTrue_whenPlatformAdmin() {
            // Arrange
            stubUser(ADMIN_EMAIL, Role.PLATFORM_ADMIN);

            // Act & Assert
            assertThat(service.canManageClub(clubId, ADMIN_EMAIL)).isTrue();
        }

        @Test
        @DisplayName("should return true for an approved club ADMIN")
        void shouldReturnTrue_whenApprovedClubAdmin() {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            stubMembership(user, ClubRole.ADMIN, MembershipStatus.APPROVED);

            // Act & Assert
            assertThat(service.canManageClub(clubId, STUDENT_EMAIL)).isTrue();
        }

        @Test
        @DisplayName("should return true for an approved club STAFF")
        void shouldReturnTrue_whenApprovedClubStaff() {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            stubMembership(user, ClubRole.STAFF, MembershipStatus.APPROVED);

            // Act & Assert
            assertThat(service.canManageClub(clubId, STUDENT_EMAIL)).isTrue();
        }

        @Test
        @DisplayName("should return false for an approved club MEMBER")
        void shouldReturnFalse_whenApprovedClubMember() {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            stubMembership(user, ClubRole.MEMBER, MembershipStatus.APPROVED);

            // Act & Assert
            assertThat(service.canManageClub(clubId, STUDENT_EMAIL)).isFalse();
        }

        @ParameterizedTest(name = "should return false when membership status is {0}")
        @EnumSource(value = MembershipStatus.class, names = {"PENDING", "REJECTED"})
        @DisplayName("should return false when membership is not APPROVED")
        void shouldReturnFalse_whenMembershipNotApproved(MembershipStatus status) {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            stubMembership(user, ClubRole.ADMIN, status);

            // Act & Assert
            assertThat(service.canManageClub(clubId, STUDENT_EMAIL)).isFalse();
        }

        @Test
        @DisplayName("should return false when user has no membership in the club")
        void shouldReturnFalse_whenNoMembership() {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            when(membershipRepository.findByUserIdAndClubId(user.getId(), clubId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThat(service.canManageClub(clubId, STUDENT_EMAIL)).isFalse();
        }

        @Test
        @DisplayName("should not query membershipRepository at all for a PLATFORM_ADMIN")
        void shouldShortCircuit_forPlatformAdmin() {
            // Arrange
            stubUser(ADMIN_EMAIL, Role.PLATFORM_ADMIN);

            // Act
            service.canManageClub(clubId, ADMIN_EMAIL);

            // Assert
            verify(membershipRepository, never()).findByUserIdAndClubId(any(), any());
        }

        @Test
        @DisplayName("should throw NoSuchElementException when user email is not found")
        void shouldThrow_whenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.canManageClub(clubId, STUDENT_EMAIL))
                    .isInstanceOf(java.util.NoSuchElementException.class);
        }

        @Test
        @DisplayName("should use the correct clubId when querying membershipRepository")
        void shouldQueryMembership_withCorrectClubId() {
            // Arrange
            User user = stubUser(STUDENT_EMAIL, Role.STUDENT);
            when(membershipRepository.findByUserIdAndClubId(user.getId(), clubId))
                    .thenReturn(Optional.empty());

            // Act
            service.canManageClub(clubId, STUDENT_EMAIL);

            // Assert – verifies the exact clubId is forwarded, not a default/null
            verify(membershipRepository).findByUserIdAndClubId(user.getId(), clubId);
        }
    }

    // =========================================================================
    // isPlatformAdmin()
    // =========================================================================

    @Nested
    @DisplayName("isPlatformAdmin()")
    class IsPlatformAdmin {

        @Test
        @DisplayName("should return true when user has PLATFORM_ADMIN role")
        void shouldReturnTrue_whenPlatformAdmin() {
            // Arrange
            stubUser(ADMIN_EMAIL, Role.PLATFORM_ADMIN);

            // Act & Assert
            assertThat(service.isPlatformAdmin(ADMIN_EMAIL)).isTrue();
        }

        @Test
        @DisplayName("should return false when user has STUDENT role")
        void shouldReturnFalse_whenStudent() {
            // Arrange
            stubUser(STUDENT_EMAIL, Role.STUDENT);

            // Act & Assert
            assertThat(service.isPlatformAdmin(STUDENT_EMAIL)).isFalse();
        }

        @Test
        @DisplayName("should throw NoSuchElementException when user email is not found")
        void shouldThrow_whenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.isPlatformAdmin(STUDENT_EMAIL))
                    .isInstanceOf(java.util.NoSuchElementException.class);
        }

        @Test
        @DisplayName("should never query membershipRepository (role is on the User entity)")
        void shouldNeverQueryMembership() {
            // Arrange
            stubUser(ADMIN_EMAIL, Role.PLATFORM_ADMIN);

            // Act
            service.isPlatformAdmin(ADMIN_EMAIL);

            // Assert
            verifyNoInteractions(membershipRepository);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Stubs {@code userRepository.findByEmail} and returns the created {@link User}.
     */
    private User stubUser(String email, Role role) {
        User user = User.builder()
                .id(userId)
                .email(email)
                .role(role)
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        return user;
    }

    /**
     * Stubs {@code membershipRepository.findByUserIdAndClubId} for the given user,
     * using the shared {@code clubId} fixture.
     */
    private void stubMembership(User user, ClubRole clubRole, MembershipStatus status) {
        Membership membership = Membership.builder()
                .user(user)
                .club(Club.builder().id(clubId).build())
                .role(clubRole)
                .status(status)
                .build();
        when(membershipRepository.findByUserIdAndClubId(user.getId(), clubId))
                .thenReturn(Optional.of(membership));
    }
}