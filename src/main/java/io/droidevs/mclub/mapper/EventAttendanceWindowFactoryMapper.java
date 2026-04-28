package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.EventAttendanceWindow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventAttendanceWindowFactoryMapper {

    @Mapping(target = "id", ignore = true)

    @Mapping(target = "event", source = "event")

    // default value
    @Mapping(target = "active", constant = "false")

    // MUST be provided
    @Mapping(source = "opensBeforeStartMinutes", target = "opensBeforeStartMinutes")
    @Mapping(source = "closesAfterStartMinutes", target = "closesAfterStartMinutes")

    // security handled in service
    @Mapping(target = "tokenHash", ignore = true)
    @Mapping(target = "tokenRotatedAt", ignore = true)

    @Mapping(target = "createdAt", ignore = true)
    EventAttendanceWindow create(
            Event event,
            int opensBeforeStartMinutes,
            int closesAfterStartMinutes
    );
}

