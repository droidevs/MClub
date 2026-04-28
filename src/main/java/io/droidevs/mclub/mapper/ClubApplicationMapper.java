package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.ClubApplication;
import io.droidevs.mclub.domain.ClubUserApplicationSummary;
import io.droidevs.mclub.dto.ClubApplicationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface ClubApplicationMapper {


    @Mapping(source = "submittedBy.id", target = "submittedById")
    @Mapping(source = "submittedBy.fullName", target = "submittedByName")
    @Mapping(source = "submittedBy.email", target = "submittedByEmail")
    @Mapping(source = "status", target = "status")
    ClubApplicationDto toDto(ClubApplication entity);



    @Mapping(target = "submittedById", source = "submittedBy.id")
    @Mapping(target = "submittedByName", source = "submittedBy.name")
    @Mapping(target = "submittedByEmail", source = "submittedBy.email")
    ClubApplicationDto toDto(ClubUserApplicationSummary summary);
}

