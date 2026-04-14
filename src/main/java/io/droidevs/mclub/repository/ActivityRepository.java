package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    List<Activity> findByClubId(UUID clubId);

    List<Activity> findTop5ByClubIdOrderByDateDesc(UUID clubId);

    // For Thymeleaf snapshots (club detail) to avoid LazyInitializationException during mapping
    @Query("select a from Activity a join fetch a.club left join fetch a.event left join fetch a.createdBy where a.club.id = :clubId order by a.date desc")
    List<Activity> findRecentByClubIdWithClubEventAndCreatedBy(@Param("clubId") UUID clubId);
}
