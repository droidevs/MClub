CREATE TABLE IF NOT EXISTS users (
     id UUID PRIMARY KEY,

     email VARCHAR(255) NOT NULL UNIQUE,
     password VARCHAR(255) NOT NULL,
     full_name VARCHAR(150),

     role VARCHAR(30) NOT NULL,

     enabled BOOLEAN NOT NULL DEFAULT TRUE,
     deleted BOOLEAN NOT NULL DEFAULT FALSE,

     created_at TIMESTAMP WITHOUT TIME ZONE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_email
    ON users(email);

CREATE INDEX IF NOT EXISTS idx_users_role
    ON users(role);

CREATE INDEX IF NOT EXISTS idx_users_enabled
    ON users(enabled);