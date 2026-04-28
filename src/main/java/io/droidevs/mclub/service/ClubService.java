package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.domain.Membership;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.ClubDto;
import io.droidevs.mclub.dto.ClubSummaryDto;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.ClubEntityMapper;
import io.droidevs.mclub.mapper.ClubMapper;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClubService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubMapper clubMapper;
    private final ClubEntityMapper clubEntityMapper;
    private final ClubAuthorizationService clubAuthorizationService;

    // ===============================
    // 🔹 CREATE
    // ===============================
    public ClubDto createClub(ClubDto dto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Club club = clubEntityMapper.toEntity(dto);
        club.setCreatedBy(user);

        return clubMapper.toDto(clubRepository.save(club));
    }

    // ===============================
    // 🔹 PUBLIC LIST (LIGHT)
    // ===============================
    @Transactional(readOnly = true)
    public Page<ClubSummaryDto> getClubsSummary(Pageable pageable) {
        return clubRepository.findAllSummary(pageable);
    }

    // ===============================
    // 🔹 FULL LIST (HEAVY)
    // ===============================
    @Transactional(readOnly = true)
    public Page<ClubDto> getAllClubs(Pageable pageable) {
        return clubRepository.findAll(pageable)
                .map(clubMapper::toDto);
    }

    // ===============================
    // 🔹 DETAIL VIEW
    // ===============================
    @Transactional(readOnly = true)
    public ClubDto getClub(UUID id) {
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));

        return clubMapper.toDto(club);
    }

    // ===============================
    // 🔹 MANAGED CLUBS (FULL)
    // ===============================
    @Transactional(readOnly = true)
    public Page<ClubDto> getMyManagedClubs(String email, Pageable pageable) {
        return clubRepository.findManagedClubs(
                email,
                List.of(ClubRole.ADMIN, ClubRole.STAFF),
                pageable
        ).map(clubMapper::toDto);
    }

    // ===============================
    // 🔹 MANAGED CLUBS (LIGHT)
    // ===============================
    @Transactional(readOnly = true)
    public Page<ClubSummaryDto> getMyManagedClubsSummary(String email, Pageable pageable) {
        return clubRepository.findManagedClubsSummary(
                email,
                List.of(ClubRole.ADMIN, ClubRole.STAFF),
                pageable
        );
    }

    // ===============================
    // 🔹 SEARCH
    // ===============================
    @Transactional(readOnly = true)
    public Page<ClubSummaryDto> search(String query, Pageable pageable) {
        return clubRepository.search(query, pageable);
    }

    public void updateClubDescription(UUID clubId, String description, String email) {
        clubAuthorizationService.requireClubManager(clubId, email);

        updateClubDescription(clubId, description);
    }

    private void updateClubDescription(UUID id, String description) {
        int updated = clubRepository.updateDescription(id, description);

        if (updated == 0) {
            throw new ResourceNotFoundException("Club not found");
        }
    }
}
