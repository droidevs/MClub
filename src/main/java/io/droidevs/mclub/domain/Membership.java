package io.droidevs.mclub.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "memberships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_membership_user_club",
                        columnNames = {"user_id", "club_id"}
                )
        },
        indexes = {
                @Index(name = "idx_membership_user", columnList = "user_id"),
                @Index(name = "idx_membership_club", columnList = "club_id"),
                @Index(name = "idx_membership_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Membership {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;
}