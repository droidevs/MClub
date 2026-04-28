package io.droidevs.mclub.mapper;
import io.droidevs.mclub.domain.Activity;
import io.droidevs.mclub.dto.ActivityCreateRequest;
import io.droidevs.mclub.dto.ActivityDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface ActivityMapper {

    // ===== ENTITY -> DTO =====
    @Mapping(source = "club.id", target = "clubId")
    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "createdBy.id", target = "createdById")
    ActivityDto toDto(Activity a);

    // ===== DTO -> ENTITY =====
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "club", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Activity toEntity(ActivityDto dto);

    // ===== CREATE REQUEST -> ENTITY (FIXED) =====
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "club", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Activity toEntity(ActivityCreateRequest request);
}