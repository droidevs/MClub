package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.domain.Membership;
import io.droidevs.mclub.domain.MembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Page<Membership> findByClubId(UUID clubId, Pageable pageable);

    Optional<Membership> findByUserIdAndClubId(UUID userId, UUID clubId);

    // For club admin UI pages
    Page<Membership> findByClubIdAndStatus(UUID clubId, MembershipStatus status, Pageable pageable);


    @EntityGraph(attributePaths = {"user"})
    @Query("""
    SELECT m FROM Membership m
    JOIN m.user u
    WHERE m.club.id = :clubId
      AND m.status = 'APPROVED'
      AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
""")
    Page<Membership> searchMembers(
            @Param("clubId") UUID clubId,
            @Param("q") String q,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user"})
    @Query("""
    SELECT m FROM Membership m
    WHERE m.club.id = :clubId
      AND m.status IN :statuses
    ORDER BY m.joinedAt DESC
""")
    Page<Membership> findHistory(
            @Param("clubId") UUID clubId,
            @Param("statuses") List<MembershipStatus> statuses,
            Pageable pageable
    );

    List<Membership> findTop5ByClubIdAndStatusOrderByJoinedAtDesc(UUID userId, MembershipStatus status);

}
