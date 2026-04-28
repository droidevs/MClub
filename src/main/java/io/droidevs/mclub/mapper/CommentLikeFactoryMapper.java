package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Comment;
import io.droidevs.mclub.domain.CommentLike;
import io.droidevs.mclub.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface CommentLikeFactoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)

    @Mapping(source = "comment", target = "comment")
    @Mapping(source = "user", target = "user")

    CommentLike create(Comment comment, User user);
}

