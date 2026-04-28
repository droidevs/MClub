package io.droidevs.mclub.dto;

import java.util.List;

public record CommentPreviewDto(
        long totalComments,
        long totalReplies,
        long totalLikes,

        List<CommentDto> topComments,
        CommentDto latestComment
) {
}
