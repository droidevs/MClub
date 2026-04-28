package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.domain.MembershipStatus;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.exception.ForbiddenException;
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


    public void requireClubManager(UUID club, String email) {
        if (!canManageClub(club, email)) {
            throw new ForbiddenException("Not allowed for this club");
        }
    }

    public boolean canManageClub(UUID clubId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        if(user.getRole() == Role.PLATFORM_ADMIN)
            return true;
        else {
            return membershipRepository.findByUserIdAndClubId(user.getId(), clubId)
                    .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                    .map(m -> m.getRole() == ClubRole.ADMIN || m.getRole() == ClubRole.STAFF)
                    .orElse(false);
        }
    }

    public boolean isPlatformAdmin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return user.getRole() == Role.PLATFORM_ADMIN;
    }

}
