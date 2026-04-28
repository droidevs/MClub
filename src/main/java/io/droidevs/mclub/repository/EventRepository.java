package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.dto.EventSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByClubId(UUID clubId, Pageable pageable);

    @Query("""
    SELECT e FROM Event e
    WHERE e.club.id = :clubId
    AND (:query IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')))
    ORDER BY e.startDate DESC
""")
    Page<Event> searchByClub(
            @Param("clubId") UUID clubId,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
    SELECT e.id as id, e.title as title
    FROM Event e
    WHERE e.club.id = :clubId
    AND (:query IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')))
    ORDER BY e.startDate DESC
""")
    Page<EventSummary> searchLightweightByClub(
            UUID clubId,
            String query,
            Pageable pageable
    );

    List<Event> findTop5ByClubIdOrderByStartDateDesc(UUID clubId);

    @Query("select e from Event e join fetch e.club where e.id = :id")
    Optional<Event> findByIdWithClub(@Param("id") UUID id);

    // For Thymeleaf snapshots (club detail) to avoid LazyInitializationException during mapping
    @Query("select e from Event e join fetch e.club left join fetch e.createdBy where e.club.id = :clubId order by e.startDate desc")
    List<Event> findRecentByClubIdWithClubAndCreatedBy(@Param("clubId") UUID clubId);
}
