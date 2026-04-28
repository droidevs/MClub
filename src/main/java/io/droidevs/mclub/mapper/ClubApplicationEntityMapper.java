package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.ClubApplication;
import io.droidevs.mclub.dto.ClubApplicationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface ClubApplicationEntityMapper {

    @Mapping(target = "id", ignore = true)

    // relations must be set in service layer
    @Mapping(target = "submittedBy", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)

    // system fields
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)

    // IMPORTANT: enum mapping
    @Mapping(target = "status", expression = "java(io.droidevs.mclub.domain.ApplicationStatus.PENDING)")
    ClubApplication toEntity(ClubApplicationDto dto);
}

