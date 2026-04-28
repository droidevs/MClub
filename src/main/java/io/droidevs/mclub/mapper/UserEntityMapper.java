package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.RegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface UserEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "password", ignore = true) // encoded in AuthService
    @Mapping(target = "role", ignore = true)     // derived in AuthService
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "memberships", ignore = true)
    @Mapping(target = "createdEvents", ignore = true)
    @Mapping(target = "createdActivities", ignore = true)
    @Mapping(target = "comments", ignore = true)
    User toEntity(RegisterRequest request);
}

