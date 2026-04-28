package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.ActivityCreateRequest;
import io.droidevs.mclub.dto.ActivityDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ActivityMapperTest {

    @Autowired
    private ActivityMapper mapper;

    // ===============================
    // toDto tests
    // ===============================

    @Test
    void should_map_activity_to_dto_correctly() {
        UUID clubId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Activity activity = Activity.builder()
                .id(UUID.randomUUID())
                .title("Test Activity")
                .description("Desc")
                .date(LocalDateTime.now())
                .club(Club.builder().id(clubId).build())
                .event(Event.builder().id(eventId).build())
                .createdBy(User.builder().id(userId).build())
                .build();

        ActivityDto dto = mapper.toDto(activity);

        assertThat(dto.getClubId()).isEqualTo(clubId);
        assertThat(dto.getEventId()).isEqualTo(eventId);
        assertThat(dto.getCreatedById()).isEqualTo(userId);
        assertThat(dto.getTitle()).isEqualTo("Test Activity");
        assertThat(dto.getDescription()).isEqualTo("Desc");
        assertThat(dto.getDate()).isEqualTo(activity.getDate());
    }

    @Test
    void should_handle_null_nested_entities() {
        Activity activity = Activity.builder()
                .title("Test")
                .build();

        ActivityDto dto = mapper.toDto(activity);

        assertThat(dto.getClubId()).isNull();
        assertThat(dto.getEventId()).isNull();
        assertThat(dto.getCreatedById()).isNull();
    }

    // ===============================
    // toEntity tests
    // ===============================

    @Test
    void should_map_dto_to_entity_and_ignore_relationships() {
        ActivityDto dto = new ActivityDto();
        dto.setTitle("Activity");
        dto.setDescription("Desc");
        dto.setDate(LocalDateTime.now());

        Activity entity = mapper.toEntity(dto);

        assertThat(entity.getTitle()).isEqualTo(dto.getTitle());
        assertThat(entity.getDescription()).isEqualTo(dto.getDescription());
        assertThat(entity.getDate()).isEqualTo(dto.getDate());

        // ignored fields
        assertThat(entity.getId()).isNull();
        assertThat(entity.getClub()).isNull();
        assertThat(entity.getEvent()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
    }

    // ===============================
    // CreateRequest → DTO
    // ===============================

    @Test
    void should_map_create_request_to_entity_and_then_to_dto() {
        ActivityCreateRequest request = new ActivityCreateRequest();
        request.setTitle("New Activity");
        request.setDescription("Desc");
        request.setDate(LocalDateTime.now());

        // Step 1: Request → Entity
        Activity entity = mapper.toEntity(request);

        assertThat(entity.getTitle()).isEqualTo(request.getTitle());
        assertThat(entity.getDescription()).isEqualTo(request.getDescription());
        assertThat(entity.getDate()).isEqualTo(request.getDate());

        // relations should be null (ignored)
        assertThat(entity.getClub()).isNull();
        assertThat(entity.getEvent()).isNull();
        assertThat(entity.getCreatedBy()).isNull();

        // Step 2: Entity → DTO
        ActivityDto dto = mapper.toDto(entity);

        assertThat(dto.getTitle()).isEqualTo(request.getTitle());
        assertThat(dto.getDescription()).isEqualTo(request.getDescription());
        assertThat(dto.getDate()).isEqualTo(request.getDate());

        // still null because relations not set
        assertThat(dto.getClubId()).isNull();
        assertThat(dto.getEventId()).isNull();
        assertThat(dto.getCreatedById()).isNull();
    }
}
