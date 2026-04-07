package io.droidevs.mclub.dto;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;
@Data
public class ActivityDto {
    private UUID id;
    private UUID clubId;
    private UUID eventId;
    private String title;
    private String description;
    private LocalDateTime date;
    private UUID createdById;
}
