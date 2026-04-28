CREATE TABLE IF NOT EXISTS clubs (
    id UUID PRIMARY KEY,

    name VARCHAR(150) NOT NULL,
    description TEXT,

    created_by UUID,

    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP,

    CONSTRAINT fk_clubs_created_by
    FOREIGN KEY (created_by)
    REFERENCES users(id)
    );

CREATE INDEX IF NOT EXISTS idx_club_created_by
    ON clubs(created_by);

CREATE INDEX IF NOT EXISTS idx_club_name
    ON clubs(name);