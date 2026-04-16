package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.repository.MembershipRepository;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubAuthorizationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;

    @InjectMocks
    private ClubAuthorizationService service;

    private UUID clubId;

    @BeforeEach
    void init() {
        clubId = UUID.randomUUID();
    }

    @Test
    void requirePlatformAdminOrClubRole_shouldAllow_platformAdmin() {
        User admin = User.builder().id(UUID.randomUUID()).email("a").role(Role.PLATFORM_ADMIN).build();
        when(userRepository.findByEmail("a")).thenReturn(Optional.of(admin));

        assertDoesNotThrow(() -> service.requirePlatformAdminOrClubRole("a", clubId, Set.of(ClubRole.ADMIN)));
        verify(membershipRepository, never()).findByUserIdAndClubId(any(), any());
    }

    @Test
    void requirePlatformAdminOrClubRole_shouldForbid_whenMembershipMissing() {
        User student = User.builder().id(UUID.randomUUID()).email("s").role(Role.STUDENT).build();
        when(userRepository.findByEmail("s")).thenReturn(Optional.of(student));
        when(membershipRepository.findByUserIdAndClubId(student.getId(), clubId)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> service.requirePlatformAdminOrClubRole("s", clubId, Set.of(ClubRole.ADMIN)));
    }

    @Test
    void requirePlatformAdminOrClubRole_shouldForbid_whenNotApproved() {
        User student = User.builder().id(UUID.randomUUID()).email("s").role(Role.STUDENT).build();
        when(userRepository.findByEmail("s")).thenReturn(Optional.of(student));

        Membership m = Membership.builder().user(student).club(Club.builder().id(clubId).build()).role(ClubRole.ADMIN).status(MembershipStatus.PENDING).build();
        when(membershipRepository.findByUserIdAndClubId(student.getId(), clubId)).thenReturn(Optional.of(m));

        assertThrows(ForbiddenException.class, () -> service.requirePlatformAdminOrClubRole("s", clubId, Set.of(ClubRole.ADMIN)));
    }

    @Test
    void requirePlatformAdminOrClubRole_shouldForbid_whenRoleNotAllowed() {
        User student = User.builder().id(UUID.randomUUID()).email("s").role(Role.STUDENT).build();
        when(userRepository.findByEmail("s")).thenReturn(Optional.of(student));

        Membership m = Membership.builder().user(student).club(Club.builder().id(clubId).build()).role(ClubRole.MEMBER).status(MembershipStatus.APPROVED).build();
        when(membershipRepository.findByUserIdAndClubId(student.getId(), clubId)).thenReturn(Optional.of(m));

        assertThrows(ForbiddenException.class, () -> service.requirePlatformAdminOrClubRole("s", clubId, Set.of(ClubRole.ADMIN)));
    }

    @Test
    void requirePlatformAdminOrClubAdminOrStaff_shouldAllow_adminOrStaff() {
        User student = User.builder().id(UUID.randomUUID()).email("s").role(Role.STUDENT).build();
        when(userRepository.findByEmail("s")).thenReturn(Optional.of(student));

        Membership adminMembership = Membership.builder().user(student).club(Club.builder().id(clubId).build()).role(ClubRole.ADMIN).status(MembershipStatus.APPROVED).build();
        when(membershipRepository.findByUserIdAndClubId(student.getId(), clubId)).thenReturn(Optional.of(adminMembership));

        assertDoesNotThrow(() -> service.requirePlatformAdminOrClubAdminOrStaff("s", clubId));
    }
}

