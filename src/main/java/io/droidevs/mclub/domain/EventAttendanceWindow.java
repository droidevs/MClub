package io.droidevs.mclub.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_attendance_windows",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_att_window_event", columnNames = {"event_id"}),
                @UniqueConstraint(name = "uk_att_window_token_hash", columnNames = {"token_hash"})
        },
        indexes = {
                @Index(name = "idx_att_window_event", columnList = "event_id"),
                @Index(name = "idx_att_window_active", columnList = "active"),
                @Index(name = "idx_att_window_token_hash", columnList = "token_hash")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EventAttendanceWindow {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ===== RELATIONS =====

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // ===== STATE =====

    @Builder.Default
    @Column(nullable = false)
    private boolean active = false;

    // ===== CONFIG =====

    /** Minutes before event start when check-in opens */
    @Column(name = "opens_before_start_minutes", nullable = false)
    private int opensBeforeStartMinutes;

    /** Minutes after event start when check-in closes */
    @Column(name = "closes_after_start_minutes", nullable = false)
    private int closesAfterStartMinutes;

    // ===== SECURITY =====

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "token_rotated_at")
    private LocalDateTime tokenRotatedAt;

    // ===== AUDIT =====

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}