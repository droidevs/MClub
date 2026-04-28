package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.dto.EventCreateRequest;
import io.droidevs.mclub.dto.EventDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventCreateRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "club", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Event toEntity(EventCreateRequest request);
}


