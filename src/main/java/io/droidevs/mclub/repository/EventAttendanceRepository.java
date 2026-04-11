package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.EventAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventAttendanceRepository extends JpaRepository<EventAttendance, UUID> {
    Optional<EventAttendance> findByEventIdAndUserId(UUID eventId, UUID userId);
    List<EventAttendance> findByEventId(UUID eventId);
    long countByEventId(UUID eventId);
}

