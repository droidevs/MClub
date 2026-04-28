package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.exception.ClubAuthorizationException;
import io.droidevs.mclub.exception.MemberShipNotFoundException;
import io.droidevs.mclub.exception.MembershipNotApprovedException;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.*;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.droidevs.mclub.domain.MembershipStatus.*;

@Service
@RequiredArgsConstructor
public class MembershipService {
    private final MembershipRepository membershipRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubAuthorizationService authorizationService;
    private final MembershipMapper membershipMapper;
    private final MembershipFactoryMapper membershipFactoryMapper;

    public MembershipDto joinClub(UUID clubId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ResourceNotFoundException("Club not found"));
        if (membershipRepository.findByUserIdAndClubId(user.getId(), clubId).isPresent()) {
            throw new RuntimeException("Already joined");
        }

        Membership m = membershipFactoryMapper.create(ClubRole.MEMBER, MembershipStatus.PENDING);
        m.setUser(user);
        m.setClub(club);

        return membershipMapper.toDto(membershipRepository.save(m));
    }

    public MembershipDto updateStatus(
            UUID membershipId,
            MembershipStatus status,
            String userEmail
    ) {
        Membership m = membershipRepository.findById(membershipId).orElseThrow(
                () -> new MemberShipNotFoundException(membershipId)
        );

        UUID clubId = m.getClub().getId();

        if (!authorizationService.canManageClub(clubId, userEmail)) {
            throw new ClubAuthorizationException(clubId);
        }

        // 🔥 business rules
        switch (status) {
            case APPROVED -> {
                if (m.getStatus() != MembershipStatus.PENDING) {
                    throw new IllegalStateException("Only pending can be approved");
                }
            }
            case REJECTED -> {
                if (m.getStatus() != MembershipStatus.PENDING) {
                    throw new IllegalStateException("Only pending can be rejected");
                }
            }
            case KICKED -> {
                if (m.getStatus() != APPROVED) {
                    throw new IllegalStateException("Only approved members can be kicked");
                }
            }
        }

        m.setStatus(MembershipStatus.valueOf(status.name()));
        membershipRepository.save(m);
        return membershipMapper.toDto(membershipRepository.save(m));
    }

    public MembershipDto updateRole(UUID membershipId, String role, String userEmail) {
        Membership m = membershipRepository.findById(membershipId).orElseThrow();

        Club club = m.getClub();

        User user = userRepository.findByEmail(userEmail).orElseThrow();
        if(user.getRole() != Role.PLATFORM_ADMIN) {
            Membership selfM = membershipRepository.findByUserIdAndClubId(
                    user.getId(),
                    club.getId()
            ).orElseThrow(
                    () -> new ClubAuthorizationException(club.getId())
            );

            if ((selfM.getRole() == ClubRole.STAFF
                    && m.getRole() == ClubRole.ADMIN
                    && !m.getUser().getEmail().equals(userEmail)) ||
                    (selfM.getRole() == ClubRole.ADMIN
                            && m.getRole() == ClubRole.ADMIN
                            && !m.getUser().getEmail().equals(userEmail)
                            && !club.getCreatedBy().getEmail().equals(userEmail))

            ) {
                throw new ClubAuthorizationException(club.getId());
            }
        }
        if (m.getStatus() != APPROVED) {
            throw new MembershipNotApprovedException();
        }

        m.setRole(ClubRole.valueOf(role.toUpperCase()));
        return membershipMapper.toDto(membershipRepository.save(m));
    }


    public Page<Membership> getMembershipApplications(@PathVariable UUID clubId,Pageable pageable, String userEmail) {
        if (authorizationService.canManageClub(clubId, userEmail)) {
            return membershipRepository.findByClubIdAndStatus(clubId, MembershipStatus.PENDING,pageable);
        } else {
            throw new ClubAuthorizationException(clubId);
        }
    }

    public Page<MembershipDto> getMembers(UUID clubId, Pageable pageable, String userEmail) {
        authorizationService.requireClubManager(clubId, userEmail);

        return membershipRepository.findByClubIdAndStatus(clubId, APPROVED, pageable)
                .map(membershipMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<MembershipDto> searchMembers(
            UUID clubId,
            String query,
            Pageable pageable,
            String email
    ) {
        authorizationService.requireClubManager(clubId, email);
        Page<Membership> page = membershipRepository.searchMembers(clubId, query, pageable);

        return page.map(membershipMapper::toDto);
    }

    public List<MembershipDto> getRecentMembers(UUID clubId, String userEmail) {
        authorizationService.requireClubManager(clubId, userEmail);

        return membershipRepository.findTop5ByClubIdAndStatusOrderByJoinedAtDesc(clubId, APPROVED)
                .stream()
                .map(membershipMapper::toDto)
                .toList();
    }

    public Optional<Membership> getMembership(UUID clubId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        return membershipRepository.findByUserIdAndClubId(user.getId(), clubId);
    }

    @Transactional(readOnly = true)
    public Page<MembershipDto> getMembershipHistory(
            UUID clubId,
            Pageable pageable,
            String email
    ) {
        authorizationService.requireClubManager(clubId, email);

        return membershipRepository.findHistory(
                clubId,
                List.of(APPROVED, REJECTED),
                pageable
        ).map(membershipMapper::toDto);
    }

    public MembershipDto approve(UUID id, String email) {
        return updateStatus(id, MembershipStatus.APPROVED, email);
    }

    public MembershipDto reject(UUID id, String email) {
        return updateStatus(id, MembershipStatus.REJECTED, email);
    }

    public MembershipDto kick(UUID id, String email) {
        return updateStatus(id, MembershipStatus.KICKED, email);
    }
}
