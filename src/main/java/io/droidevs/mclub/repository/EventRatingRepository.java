package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.EventRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRatingRepository extends JpaRepository<EventRating, UUID> {

    Optional<EventRating> findByEventIdAndStudentId(UUID eventId, UUID studentId);

    List<EventRating> findByEventId(UUID eventId);

    @Query("select avg(r.rating) from EventRating r where r.event.id = :eventId")
    Double getAverageRating(UUID eventId);

    @Query("select count(r) from EventRating r where r.event.id = :eventId")
    long getRatingCount(UUID eventId);
}

