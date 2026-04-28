package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.Activity;
import io.droidevs.mclub.dto.ActivitySummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    // ===============================
    // 🔹 LIGHTWEIGHT PROJECTION (BEST FOR API)
    // ===============================
    @Query("""
        select new io.droidevs.mclub.dto.ActivitySummary(
            a.id,
            a.title,
            a.date,
            a.createdAt
        )
        from Activity a
        where a.club.id = :clubId
        and a.deleted = false
        order by a.date desc
    """)
    Page<ActivitySummary> findSummaryByClubId(UUID clubId, Pageable pageable);


    // ===============================
    // 🔹 RECENT (FAST DASHBOARD)
    // ===============================
    @Query("""
        select new io.droidevs.mclub.dto.ActivitySummary(
            a.id,
            a.title,
            a.date,
            a.createdAt
        )
        from Activity a
        where a.club.id = :clubId
        and a.deleted = false
        order by a.date desc
    """)
    List<ActivitySummary> findTop5SummaryByClubId(UUID clubId, Pageable pageable);


    @Query("""
    select new io.droidevs.mclub.dto.ActivitySummary(
        a.id, a.title, a.date, a.createdAt
    )
    from Activity a
    where a.club.id = :clubId
    and a.deleted = false
    and (
        lower(a.title) like lower(concat('%', :query, '%'))
        or lower(a.description) like lower(concat('%', :query, '%'))
    )
""")
    Page<ActivitySummary> searchByClub(UUID clubId, String query, Pageable pageable);


    // ===============================
    // 🔹 SINGLE ENTITY WITH RELATIONS
    // ===============================
    @EntityGraph(attributePaths = {"club", "event", "createdBy"})
    Optional<Activity> findByIdAndDeletedFalse(UUID id);


    // ===============================
    // 🔹 COUNT (FAST METRICS)
    // ===============================
    long countByClubIdAndDeletedFalse(UUID clubId);
}
