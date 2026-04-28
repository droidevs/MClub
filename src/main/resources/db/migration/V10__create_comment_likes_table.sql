CREATE TABLE IF NOT EXISTS comment_likes (
    id UUID PRIMARY KEY,

    comment_id UUID NOT NULL,
    user_id UUID NOT NULL,

    created_at TIMESTAMP,

    CONSTRAINT fk_comment_likes_comment
    FOREIGN KEY (comment_id)
    REFERENCES comments(id),

    CONSTRAINT fk_comment_likes_user
    FOREIGN KEY (user_id)
    REFERENCES users(id),

    CONSTRAINT uk_comment_like_comment_user
    UNIQUE (comment_id, user_id)
    );

CREATE INDEX IF NOT EXISTS idx_comment_likes_comment
    ON comment_likes(comment_id);

CREATE INDEX IF NOT EXISTS idx_comment_likes_user
    ON comment_likes(user_id);