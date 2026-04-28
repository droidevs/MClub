package io.droidevs.mclub.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_ratings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_rating_event_student",
                        columnNames = {"event_id", "student_id"}
                )
        },
        indexes = {
                @Index(name = "idx_event_rating_event", columnList = "event_id"),
                @Index(name = "idx_event_rating_student", columnList = "student_id"),
                @Index(name = "idx_event_rating_rating", columnList = "rating"),
                @Index(name = "idx_event_rating_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EventRating {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ===== RELATIONS =====

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // ===== DATA =====

    @Column(nullable = false)
    private int rating; // 1..5

    @Column(length = 1000)
    private String comment;

    // ===== AUDIT =====

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

