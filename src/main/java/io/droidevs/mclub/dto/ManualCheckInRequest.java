package io.droidevs.mclub.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ManualCheckInRequest {
    private UUID studentId;
}