package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.ClubApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ClubApplicationRepository extends JpaRepository<ClubApplication, UUID> {
    List<ClubApplication> findByStatus(String status);
}

