package io.droidevs.mclub.dto;


import jakarta.validation.constraints.NotBlank;

public record CreateClubRequest(
        @NotBlank String name,
        String description
) {}