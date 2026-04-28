package io.droidevs.mclub.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ClubUserApplicationSummary {

    UUID getId();
    String getName();
    String getDescription();
    ApplicationStatus getStatus();
    LocalDateTime getCreatedAt();

    // Nested relation! Spring handles the JOIN automatically
    UserSummary getSubmittedBy();

    interface UserSummary {
        UUID getId();
        String getName();
        String getEmail();
    }
}
