package io.droidevs.mclub.repository;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.dto.ClubSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubRepository extends JpaRepository<Club, UUID> {

    // ===============================
    // 🔹 BASIC PAGINATED LIST (LIGHT)
    // ===============================
    @Query("""
        select c.id, c.name
        from Club c
        order by c.createdAt desc
    """)
    Page<ClubSummaryDto> findAllSummary(Pageable pageable);


    // ===============================
    // 🔹 DETAIL VIEW (SAFE FETCH)
    // ===============================
    @EntityGraph(attributePaths = {"createdBy"})
    Optional<Club> findById(UUID id);


    // ===============================
    // 🔹 ADMIN / MANAGED CLUBS (OPTIMIZED)
    // ===============================
    @Query("""
        select distinct c
        from Membership m
        join m.club c
        join m.user u
        where u.email = :email
        and m.role in :roles
    """)
    Page<Club> findManagedClubs(
            String email,
            List<ClubRole> roles,
            Pageable pageable
    );


    // ===============================
    // 🔹 LIGHT VERSION (BEST FOR UI)
    // ===============================
    @Query("""
        select c.id, c.name, c.description
        from Membership m
        join m.club c
        join m.user u
        where u.email = :email
        and m.role in :roles
    """)
    Page<ClubSummaryDto> findManagedClubsSummary(
            String email,
            List<ClubRole> roles,
            Pageable pageable
    );


    // ===============================
    // 🔹 EXISTS (VERY IMPORTANT)
    // ===============================
    boolean existsById(UUID id);


    // ===============================
    // 🔹 COUNT (METRICS)
    // ===============================
    long count();


    // ===============================
    // 🔹 SEARCH (SCALABLE)
    // ===============================
    @Query("""
        select
            c.id,
            c.name,
            c.description
        from Club c
        where lower(c.name) like lower(concat('%', :query, '%'))
        or lower(c.description) like lower(concat('%', :query, '%'))
    """)
    Page<ClubSummaryDto> search(String query, Pageable pageable);

    @Query("""
    update Club c
    set c.description = :description
    where c.id = :id
""")
    @Modifying
    int updateDescription(
            @Param("id") UUID id,
            @Param("description") String description
    );
}
