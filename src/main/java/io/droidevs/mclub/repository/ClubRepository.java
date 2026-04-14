package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClubRepository extends JpaRepository<Club, UUID> {
    @Query("select c from Club c left join fetch c.createdBy where c.id = :id")
    Optional<Club> findByIdEager(@Param("id") UUID id);
}
