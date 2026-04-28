package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Comment;
import io.droidevs.mclub.dto.CommentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
public interface CommentMapper {

    @Mapping(source = "c.id", target = "id")
    @Mapping(source = "c.parentId", target = "parentId")

    @Mapping(source = "c.author.id", target = "authorId")
    @Mapping(source = "c.author.fullName", target = "authorFullName")

    @Mapping(source = "c.createdAt", target = "createdAt")

    // injected by service layer
    @Mapping(source = "likeCount", target = "likeCount")
    @Mapping(source = "likedByMe", target = "likedByMe")
    @Mapping(source = "replyCount", target = "replyCount")
    @Mapping(source = "hasMoreReplies", target = "hasMoreReplies")

    // soft delete handling
    @Mapping(expression = "java(c.isDeleted() ? \"[deleted]\" : c.getContent())", target = "content")

    // always initialized
    @Mapping(expression = "java(new java.util.ArrayList<>())", target = "repliesPreview")
    CommentDto toDto(Comment c, long likeCount, boolean likedByMe, int replyCount, boolean hasMoreReplies);
}

