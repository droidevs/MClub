CREATE TABLE IF NOT EXISTS activities (
    id UUID PRIMARY KEY,

    club_id UUID NOT NULL,
    event_id UUID,

    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),

    date TIMESTAMP NOT NULL,

    created_by UUID NOT NULL,

    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_activities_club
    FOREIGN KEY (club_id)
    REFERENCES clubs(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_activities_event
    FOREIGN KEY (event_id)
    REFERENCES events(id)
    ON DELETE SET NULL,

    CONSTRAINT fk_activities_created_by
    FOREIGN KEY (created_by)
    REFERENCES users(id)
    ON DELETE RESTRICT
);

-- Indexes (match your JPA @Index)
CREATE INDEX IF NOT EXISTS idx_activity_club
    ON activities(club_id);

CREATE INDEX IF NOT EXISTS idx_activity_event
    ON activities(event_id);

CREATE INDEX IF NOT EXISTS idx_activity_created_by
    ON activities(created_by);

CREATE INDEX IF NOT EXISTS idx_activity_date
    ON activities(date);