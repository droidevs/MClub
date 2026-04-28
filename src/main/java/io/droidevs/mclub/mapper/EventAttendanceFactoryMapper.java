package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.AttendanceMethod;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.EventAttendance;
import io.droidevs.mclub.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface EventAttendanceFactoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "checkedInBy", ignore = true)
    @Mapping(target = "checkedInAt", ignore = true)
    // REQUIRED RELATIONS
    @Mapping(target = "event", source = "event")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "method", source = "method")
    EventAttendance create(Event event, User user, AttendanceMethod method);
}

