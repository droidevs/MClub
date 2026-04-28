package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.dto.ClubDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface ClubEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "memberships", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Club toEntity(ClubDto dto);

}

