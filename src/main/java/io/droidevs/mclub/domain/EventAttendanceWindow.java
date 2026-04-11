package io.droidevs.mclub.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_attendance_windows", uniqueConstraints = {
        @UniqueConstraint(name = "uk_att_window_event", columnNames = {"event_id"}),
        @UniqueConstraint(name = "uk_att_window_token_hash", columnNames = {"token_hash"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAttendanceWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private boolean active;

    /** Minutes before event start when check-in opens */
    @Column(nullable = false)
    private int opensBeforeStartMinutes;

    /** Minutes after event start when check-in closes (relative to start) */
    @Column(nullable = false)
    private int closesAfterStartMinutes;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime tokenRotatedAt;
}

