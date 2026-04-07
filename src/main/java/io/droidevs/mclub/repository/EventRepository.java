package io.droidevs.mclub.repository;
import io.droidevs.mclub.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByClubId(UUID clubId);
}
