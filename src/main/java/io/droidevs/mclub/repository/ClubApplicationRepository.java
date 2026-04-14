package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.ClubApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClubApplicationRepository extends JpaRepository<ClubApplication, UUID> {
    List<ClubApplication> findByStatus(String status);

    @Query("select a from ClubApplication a join fetch a.submittedBy where a.status = :status order by a.createdAt desc")
    List<ClubApplication> findByStatusWithSubmittedBy(@Param("status") String status);
}
