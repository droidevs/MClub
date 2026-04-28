CREATE TABLE IF NOT EXISTS club_applications (
    id UUID PRIMARY KEY,

    name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,

    submitted_by UUID NOT NULL,

    reviewed_by UUID,

    status VARCHAR(20) NOT NULL,

    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP,

    CONSTRAINT fk_club_app_submitted_by
    FOREIGN KEY (submitted_by)
    REFERENCES users(id),

    CONSTRAINT fk_club_app_reviewed_by
    FOREIGN KEY (reviewed_by)
    REFERENCES users(id)
    );

CREATE INDEX IF NOT EXISTS idx_club_app_submitted_by
    ON club_applications(submitted_by);

CREATE INDEX IF NOT EXISTS idx_club_app_status
    ON club_applications(status);