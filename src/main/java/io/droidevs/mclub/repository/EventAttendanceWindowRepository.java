package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.EventAttendanceWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventAttendanceWindowRepository extends JpaRepository<EventAttendanceWindow, UUID> {
    Optional<EventAttendanceWindow> findByEventId(UUID eventId);
    Optional<EventAttendanceWindow> findByTokenHash(String tokenHash);
}

