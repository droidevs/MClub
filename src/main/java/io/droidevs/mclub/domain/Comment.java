package io.droidevs.mclub.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comments_target", columnList = "target_type, target_id"),
                @Index(name = "idx_comments_parent", columnList = "parent_id"),
                @Index(name = "idx_comments_author", columnList = "author_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Comment {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private CommentTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    /**
     * If null → top-level comment
     * If not null → reply to another comment
     */
    @Column(name = "parent_id")
    private UUID parentId;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}