package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.ApplicationStatus;
import io.droidevs.mclub.domain.ClubApplication;
import io.droidevs.mclub.domain.ClubApplicationSummary;
import io.droidevs.mclub.domain.ClubUserApplicationSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ClubApplicationRepository extends JpaRepository<ClubApplication, UUID> {


    // ===============================
    // 🔹 LIGHTWEIGHT PROJECTION (BEST FOR LIST UI)
    // ===============================
    @EntityGraph(attributePaths = {"submittedBy"})
    @Query("""
        select a from ClubApplication a
        where a.status = :status
        and a.deleted = false
    """)
    Page<ClubUserApplicationSummary> findSummaryByStatus(
            ApplicationStatus status,
            Pageable pageable
    );


    // ===============================
    // 🔹 DETAIL VIEW (ADMIN)
    // ===============================
    @EntityGraph(attributePaths = {"submittedBy", "reviewedBy"})
    Optional<ClubApplication> findByIdAndDeletedFalse(UUID id);



    // ===============================
    // 🔹 COUNT (FAST DASHBOARD)
    // ===============================
    long countByStatusAndDeletedFalse(ApplicationStatus status);


    // ===============================
    // 🔹 USER APPLICATIONS (OPTIONAL FEATURE)
    // ===============================
    @Query("""
        select a
        from ClubApplication a
        join a.submittedBy u
        where u.id = :userId
        and a.deleted = false
        order by a.createdAt desc
    """)
    Page<ClubApplicationSummary> findByUser(UUID userId, Pageable pageable);
}