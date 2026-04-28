CREATE TABLE IF NOT EXISTS memberships (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    club_id UUID NOT NULL,

    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,

    joined_at TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT fk_memberships_user
    FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_memberships_club
    FOREIGN KEY (club_id) REFERENCES clubs(id),

    CONSTRAINT uk_membership_user_club
    UNIQUE (user_id, club_id)
    );

-- Indexes (for performance)
CREATE INDEX IF NOT EXISTS idx_membership_user
    ON memberships(user_id);

CREATE INDEX IF NOT EXISTS idx_membership_club
    ON memberships(club_id);

CREATE INDEX IF NOT EXISTS idx_membership_status
    ON memberships(status);

CREATE INDEX idx_membership_club_status_updated
    ON membership (club_id, status);