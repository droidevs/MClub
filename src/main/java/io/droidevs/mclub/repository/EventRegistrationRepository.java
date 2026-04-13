package io.droidevs.mclub.repository;
import io.droidevs.mclub.domain.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {
    List<EventRegistration> findByEventId(UUID eventId);
    Optional<EventRegistration> findByUserIdAndEventId(UUID userId, UUID eventId);

    long countByEventId(UUID eventId);
}
