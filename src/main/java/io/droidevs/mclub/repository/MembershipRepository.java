package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.domain.Membership;
import io.droidevs.mclub.domain.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    List<Membership> findByClubId(UUID clubId);
    Optional<Membership> findByUserIdAndClubId(UUID userId, UUID clubId);

    // For club admin UI pages
    List<Membership> findByClubIdAndStatus(UUID clubId, MembershipStatus status);

    @Query("select m from Membership m join fetch m.club where m.user.id = :userId and (m.role = :adminRole or m.role = :staffRole)")
    List<Membership> findManagedByUserIdWithClub(@Param("userId") UUID userId,
                                                @Param("adminRole") ClubRole adminRole,
                                                @Param("staffRole") ClubRole staffRole);

    // Eager variants for Thymeleaf pages (need both club + user)
    @Query("select m from Membership m join fetch m.user join fetch m.club where m.club.id = :clubId and m.status = :status")
    List<Membership> findByClubIdAndStatusWithUserAndClub(@Param("clubId") UUID clubId,
                                                         @Param("status") MembershipStatus status);

    @Query("select m from Membership m join fetch m.user join fetch m.club where m.club.id = :clubId and m.status = :status")
    List<Membership> findByClubIdAndStatusWithUser(@Param("clubId") UUID clubId,
                                                  @Param("status") MembershipStatus status);
}
