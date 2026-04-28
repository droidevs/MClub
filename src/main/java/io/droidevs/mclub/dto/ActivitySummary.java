package io.droidevs.mclub.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivitySummary(
        UUID id,
        String title,
        LocalDateTime date,
        LocalDateTime createdAt
) {}
