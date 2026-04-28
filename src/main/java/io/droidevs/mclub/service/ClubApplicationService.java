package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.ClubApplicationDto;
import io.droidevs.mclub.mapper.ClubApplicationEntityMapper;
import io.droidevs.mclub.mapper.ClubApplicationMapper;
import io.droidevs.mclub.mapper.ClubFromApplicationMapper;
import io.droidevs.mclub.mapper.MembershipFactoryMapper;
import io.droidevs.mclub.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClubApplicationService {

    private final ClubApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final ClubApplicationMapper mapper;
    private final ClubApplicationEntityMapper clubApplicationEntityMapper;
    private final ClubFromApplicationMapper clubFromApplicationMapper;
    private final MembershipFactoryMapper membershipFactoryMapper;

    public ClubApplicationDto submitApplication(ClubApplicationDto dto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ClubApplication app = clubApplicationEntityMapper.toEntity(dto);
        app.setSubmittedBy(user);
        app.setStatus(ApplicationStatus.PENDING);

        return mapper.toDto(applicationRepository.save(app));
    }

    @Transactional(readOnly = true)
    public Page<ClubApplicationSummary> getUserApplications(UUID userId, Pageable pageable) {
        return applicationRepository.findByUser(userId, pageable);
    }

    public Page<ClubApplicationDto> getApplicationsByStatus(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findSummaryByStatus(status, pageable)
                .map(mapper::toDto);
    }

    public Page<ClubApplicationDto> getPendingApplications(Pageable pageable) {
        return applicationRepository.findSummaryByStatus(ApplicationStatus.PENDING, pageable)
                .map(mapper::toDto);
    }

    public Page<ClubApplicationDto> getApprovedApplications(Pageable pageable) {
        return applicationRepository.findSummaryByStatus(ApplicationStatus.APPROVED, pageable)
                .map(mapper::toDto);
    }

    public Page<ClubApplicationDto> getRejectedApplications(Pageable pageable) {
        return applicationRepository.findSummaryByStatus(ApplicationStatus.REJECTED, pageable)
                .map(mapper::toDto);
    }

    @Transactional
    public void approveApplication(UUID id) {
        ClubApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!ApplicationStatus.PENDING.equals(app.getStatus())) {
            throw new RuntimeException("Application is already processed");
        }

        app.setStatus(ApplicationStatus.APPROVED);
        applicationRepository.save(app);

        User submitter = app.getSubmittedBy();

        // Create the club based on the application
        Club club = clubFromApplicationMapper.toEntity(app);
        club.setCreatedBy(submitter);
        club = clubRepository.save(club);

        // Assign the user who submitted the application as the Club ADMIN (club-scoped)
        Membership membership = membershipFactoryMapper.create(ClubRole.ADMIN, MembershipStatus.APPROVED);
        membership.setUser(submitter);
        membership.setClub(club);
        membershipRepository.save(membership);
    }

    @Transactional
    public void rejectApplication(UUID id) {
        ClubApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        app.setStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(app);
    }

    // Fetch detail view with full relations (submittedBy, reviewedBy)
    @Transactional(readOnly = true)
    public ClubApplication getApplicationDetail(UUID id) {
        return applicationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found with ID: " + id));
    }

    // Fast count for dashboard stats
    @Transactional(readOnly = true)
    public long countByStatus(ApplicationStatus status) {
        return applicationRepository.countByStatusAndDeletedFalse(status);
    }

    // Quick helper for a full dashboard summary map
    @Transactional(readOnly = true)
    public Map<String, Long> getDashboardStats() {
        return Map.of(
                "pending", applicationRepository.countByStatusAndDeletedFalse(ApplicationStatus.PENDING),
                "approved", applicationRepository.countByStatusAndDeletedFalse(ApplicationStatus.APPROVED),
                "rejected", applicationRepository.countByStatusAndDeletedFalse(ApplicationStatus.REJECTED)
        );
    }
}
