package io.droidevs.mclub.dto;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;
@Data
public class MembershipDto {
    private UUID id;
    private UUID userId;
    private UUID clubId;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
}
