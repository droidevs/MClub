package io.droidevs.mclub.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateClubDescriptionRequest(
        @NotBlank String description
) {}