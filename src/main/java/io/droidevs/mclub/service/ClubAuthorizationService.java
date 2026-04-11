package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.domain.MembershipStatus;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.repository.MembershipRepository;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClubAuthorizationService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public boolean isPlatformAdmin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return user.getRole() == Role.PLATFORM_ADMIN;
    }

    public void requirePlatformAdminOrClubRole(String email, UUID clubId, Set<ClubRole> allowedRoles) {
        if (isPlatformAdmin(email)) {
            return;
        }

        User user = userRepository.findByEmail(email).orElseThrow();

        boolean ok = membershipRepository.findByUserIdAndClubId(user.getId(), clubId)
                .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                .map(m -> allowedRoles.contains(m.getRole()))
                .orElse(false);

        if (!ok) {
            throw new RuntimeException("Not allowed for this club");
        }
    }

    public void requirePlatformAdminOrClubAdminOrStaff(String email, UUID clubId) {
        requirePlatformAdminOrClubRole(email, clubId, EnumSet.of(ClubRole.ADMIN, ClubRole.STAFF));
    }
}

