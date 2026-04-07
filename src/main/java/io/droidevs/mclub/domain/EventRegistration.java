package io.droidevs.mclub.domain;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Table(name = "event_registrations")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EventRegistration {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id")
    private Event event;
    @CreationTimestamp
    private LocalDateTime registeredAt;
}
