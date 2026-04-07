package io.droidevs.mclub.dto;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;
@Data
public class RegistrationDto {
    private UUID id;
    private UUID userId;
    private UUID eventId;
    private LocalDateTime registeredAt;
}
