package io.droidevs.mclub.dto;

import java.util.UUID;


public record CommentSummaryDto(
        UUID id,
        String content,
        String authorName,
        long likeCount,
        int replyCount,
        boolean likedByMe,
        String createdAt
) {}