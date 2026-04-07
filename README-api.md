# MClub - Club Management System MVP API & Schema

## Database Schema (DDL Equivalent)
*Note: The application uses Hibernate `ddl-auto: update` to generate the schema at run-time. Below is a representation of the generated schema.*
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clubs (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE memberships (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    club_id UUID REFERENCES clubs(id),
    role VARCHAR(50),
    status VARCHAR(50),  -- PENDING, APPROVED, REJECTED
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, club_id)
);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    club_id UUID REFERENCES clubs(id),
    title VARCHAR(255),
    description TEXT,
    location VARCHAR(255),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE event_registrations (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    event_id UUID REFERENCES events(id),
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, event_id)
);

CREATE TABLE activities (
    id UUID PRIMARY KEY,
    club_id UUID REFERENCES clubs(id),
    event_id UUID REFERENCES events(id),
    title VARCHAR(255),
    description TEXT,
    date TIMESTAMP,
    created_by UUID REFERENCES users(id)
);
```

## Example API Requests

### 1. Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{
  "email": "admin@club.com",
  "password": "password123",
  "fullName": "Admin User",
  "role": "ADMIN"
}'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
  "email": "admin@club.com",
  "password": "password123"
}'
# Response: {"token": "ey..."}
```

### 3. Create Club (Requires ADMIN Token)
```bash
curl -X POST http://localhost:8080/api/clubs \
-H "Authorization: Bearer <YOUR_TOKEN>" \
-H "Content-Type: application/json" \
-d '{
  "name": "Tech Enthusiasts",
  "description": "A place for tech lovers."
}'
```

### 4. Create Event (Requires ADMIN Token)
```bash
curl -X POST http://localhost:8080/api/events \
-H "Authorization: Bearer <YOUR_TOKEN>" \
-H "Content-Type: application/json" \
-d '{
  "clubId": "<CLUB_UUID>",
  "title": "Hackathon 2026",
  "description": "Annual hackathon event",
  "location": "Main Hall",
  "startDate": "2026-05-01T09:00:00",
  "endDate": "2026-05-02T18:00:00"
}'
```

### 5. Join Club (Requires Auth Token)
```bash
curl -X POST http://localhost:8080/api/memberships/club/<CLUB_UUID>/join \
-H "Authorization: Bearer <YOUR_TOKEN>"
```

### 6. Register to Event (Requires Auth Token)
```bash
curl -X POST http://localhost:8080/api/events/<EVENT_UUID>/register \
-H "Authorization: Bearer <YOUR_TOKEN>"
```

### 7. View Events for Club
```bash
curl -X GET http://localhost:8080/api/events/club/<CLUB_UUID> \
-H "Authorization: Bearer <YOUR_TOKEN>"
```

