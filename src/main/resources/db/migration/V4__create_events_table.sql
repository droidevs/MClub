CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY,

    club_id UUID NOT NULL,
    created_by UUID NOT NULL,

    title VARCHAR(150) NOT NULL,
    description TEXT,
    location VARCHAR(255),

    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,

    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP,

    CONSTRAINT fk_events_club
    FOREIGN KEY (club_id)
    REFERENCES clubs(id),

    CONSTRAINT fk_events_created_by
    FOREIGN KEY (created_by)
    REFERENCES users(id),

    CONSTRAINT chk_event_dates
    CHECK (end_date >= start_date)
    );

CREATE INDEX IF NOT EXISTS idx_event_club
    ON events(club_id);

CREATE INDEX IF NOT EXISTS idx_event_created_by
    ON events(created_by);

CREATE INDEX IF NOT EXISTS idx_event_start_date
    ON events(start_date);