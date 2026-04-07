package io.droidevs.mclub.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ClubApplicationDto {
    private UUID id;
    private String name;
    private String description;
    private UUID submittedById;
    private String submittedByName;
    private String submittedByEmail;
    private String status;
    private LocalDateTime createdAt;
}

