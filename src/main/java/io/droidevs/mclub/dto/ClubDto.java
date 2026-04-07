package io.droidevs.mclub.dto;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;
@Data
public class ClubDto {
    private UUID id;
    private String name;
    private String description;
    private UUID createdById;
    private LocalDateTime createdAt;
}
