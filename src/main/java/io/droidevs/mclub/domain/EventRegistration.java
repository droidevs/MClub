package io.droidevs.mclub.domain;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_registrations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_registration_event_user",
                        columnNames = {"event_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_event_registration_event", columnList = "event_id"),
                @Index(name = "idx_event_registration_user", columnList = "user_id"),
                @Index(name = "idx_event_registration_date", columnList = "registered_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EventRegistration {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ===== RELATIONS =====

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // ===== DATA =====

    @CreationTimestamp
    @Column(name = "registered_at", updatable = false)
    private LocalDateTime registeredAt;
}