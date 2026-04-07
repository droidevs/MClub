package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.ClubApplication;
import io.droidevs.mclub.dto.ClubApplicationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClubApplicationMapper {
    @Mapping(target = "submittedById", source = "submittedBy.id")
    @Mapping(target = "submittedByName", source = "submittedBy.fullName")
    @Mapping(target = "submittedByEmail", source = "submittedBy.email")
    ClubApplicationDto toDto(ClubApplication application);

    @Mapping(target = "submittedBy", ignore = true)
    ClubApplication toEntity(ClubApplicationDto dto);
}

