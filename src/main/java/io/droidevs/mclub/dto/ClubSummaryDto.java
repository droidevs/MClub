package io.droidevs.mclub.dto;

import java.util.UUID;

public record ClubSummaryDto(
        UUID id,
        String name,
        String description
){ }