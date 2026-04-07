package io.droidevs.mclub.repository;
import io.droidevs.mclub.domain.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ClubRepository extends JpaRepository<Club, UUID> {}
