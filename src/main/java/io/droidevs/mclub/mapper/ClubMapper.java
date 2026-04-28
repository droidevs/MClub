package io.droidevs.mclub.mapper;
import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.dto.ClubDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface ClubMapper {

    // =====================
    // ENTITY → DTO
    // =====================
    @Mapping(source = "createdBy.id", target = "createdById")
    ClubDto toDto(Club club);

}
