CREATE TABLE IF NOT EXISTS comments (
    id UUID PRIMARY KEY,

    target_type VARCHAR(20) NOT NULL,
    target_id UUID NOT NULL,

    parent_id UUID,

    author_id UUID NOT NULL,

    content VARCHAR(2000) NOT NULL,

    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP,

    CONSTRAINT fk_comments_author
    FOREIGN KEY (author_id)
    REFERENCES users(id)
    );

CREATE INDEX IF NOT EXISTS idx_comments_target
    ON comments(target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_comments_parent
    ON comments(parent_id);

CREATE INDEX IF NOT EXISTS idx_comments_author
    ON comments(author_id);