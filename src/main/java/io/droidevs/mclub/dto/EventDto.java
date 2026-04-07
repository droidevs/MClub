package io.droidevs.mclub.dto;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;
@Data
public class EventDto {
    private UUID id;
    private UUID clubId;
    private String title;
    private String description;
    private String location;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private UUID createdById;
}
