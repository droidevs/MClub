package io.droidevs.mclub.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_attendance_event_user",
                        columnNames = {"event_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_attendance_event", columnList = "event_id"),
                @Index(name = "idx_attendance_user", columnList = "user_id"),
                @Index(name = "idx_attendance_checked_by", columnList = "checked_in_by"),
                @Index(name = "idx_attendance_checked_at", columnList = "checked_in_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EventAttendance {

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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_in_by")
    private User checkedInBy;

    // ===== DATA =====

    @CreationTimestamp
    @Column(name = "checked_in_at", nullable = false, updatable = false)
    private LocalDateTime checkedInAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AttendanceMethod method;
}