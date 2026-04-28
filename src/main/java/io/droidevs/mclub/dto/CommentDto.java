package io.droidevs.mclub.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommentDto {

    private UUID id;
    private UUID parentId;
    private String authorFullName;
    private UUID authorId;
    private String content;
    private LocalDateTime createdAt;

    private long likeCount;
    private boolean likedByMe;

    /** Total number of direct repliesPreview (children) for this comment. */
    private int replyCount;

    /**
     * When the UI receives a truncated reply preview list, this flag indicates there are more
     * repliesPreview than included in {@link #repliesPreview}.
     */
    private boolean hasMoreReplies;

    private List<CommentDto> repliesPreview = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getAuthorFullName() {
        return authorFullName;
    }

    public void setAuthorFullName(String authorFullName) {
        this.authorFullName = authorFullName;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public boolean isLikedByMe() {
        return likedByMe;
    }

    public void setLikedByMe(boolean likedByMe) {
        this.likedByMe = likedByMe;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }

    public boolean isHasMoreReplies() {
        return hasMoreReplies;
    }

    public void setHasMoreReplies(boolean hasMoreReplies) {
        this.hasMoreReplies = hasMoreReplies;
    }

    public List<CommentDto> getRepliesPreview() {
        return repliesPreview;
    }

    public void setRepliesPreview(List<CommentDto> repliesPreview) {
        this.repliesPreview = repliesPreview;
    }
}

