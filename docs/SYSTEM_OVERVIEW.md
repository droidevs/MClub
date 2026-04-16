~~# MClub System Overview

This document explains the **MClub** Spring Boot application: its architecture, major features, request flows, security model, and database schema.

> Source of truth: the code in `src/main/java/io/droidevs/mclub`, Thymeleaf templates in `src/main/resources/templates`, and Flyway migration `src/main/resources/db/migration/V1__schema.sql`.

## 1) What MClub is

MClub is a club management system with:

- **Clubs** (created/managed by platform admins and club-scoped admins/staff)
- **Memberships** (join requests, approval, role inside a club)
- **Events** (created by clubs; students can register)
- **Attendance / check-in** (QR-token window configured by organizers; students check in)
- **Event ratings** (students rate events; last rating per student per event is stored)
- **Comments** (threaded comments for **events** and **activities**, including likes and replies)
- **Club applications** (students apply to create a club; platform admin reviews)

The app has both:

- A **server-rendered UI** based on **Thymeleaf**.
- A **REST API** under `/api/**` used by UI JavaScript and also usable by external clients.

## 2) Tech stack

- Java + Spring Boot
- Spring MVC (web controllers + REST controllers)
- Thymeleaf templates
- Spring Security with JWT authentication
- PostgreSQL database
- Flyway migrations (`V1__schema.sql`) with JPA set to `ddl-auto: validate`

Key config:

- `src/main/resources/application.yml`
  - `spring.jpa.open-in-view: false` (important to avoid hidden lazy-loading in views)
  - PostgreSQL datasource
  - Hikari pool tuned to reduce “connection has been closed” warnings

## 3) High-level architecture

**Layered structure**:

1. **Controllers** (`io.droidevs.mclub.controller`)
   - Web controllers: `@Controller` return Thymeleaf views.
   - API controllers: `@RestController` return JSON.

2. **Services** (`io.droidevs.mclub.service`)
   - Business logic and validation.
   - Authorization checks (global and club-scoped).

3. **Repositories** (`io.droidevs.mclub.repository`)
   - Spring Data JPA repositories.

4. **Domain model** (`io.droidevs.mclub.domain`)
   - JPA entities (User, Club, Event, Comment, etc.) + enums.

5. **DTOs / mappers**
   - DTOs in `io.droidevs.mclub.dto`
   - Mapping helpers in `io.droidevs.mclub.mapper`

6. **Security** (`io.droidevs.mclub.security`)
   - JWT filter, token provider, security configuration.

## 4) Security & authorization model

### 4.1 Global roles

A user has exactly one global role stored on `users.role`.

From `README-api.md` and `SecurityConfig`:

- `PLATFORM_ADMIN`
- `STUDENT`

### 4.2 Club-scoped roles

Inside a club, permissions come from a membership record:

- `memberships.role`: `ADMIN | STAFF | MEMBER`
- `memberships.status`: `PENDING | APPROVED | REJECTED`

This allows the same user to be club-admin in one club and just a member in another.

### 4.3 JWT authentication

- Security is **stateless** (`SessionCreationPolicy.STATELESS`).
- A custom `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`.
- Many routes use `@PreAuthorize(...)` on controller methods.

### 4.4 Route protection (high level)

From `SecurityConfig`:

- Public pages: `/`, `/clubs`, `/events`, `/login`, `/register`, static assets.
- Event detail pages: `/events/*` require authentication.
- Public API reads for UI:
  - `GET /api/events/*/ratings/summary`
  - `GET /api/comments/**`
- Club-admin pages: `/club-admin/**` require login (club-scoped authorization happens inside code).
- Club applications:
  - applying requires login
  - reviewing requires `PLATFORM_ADMIN`

## 5) Database schema (Flyway V1)

Schema source: `src/main/resources/db/migration/V1__schema.sql`.

### 5.1 Tables

#### `users`
- `id (uuid, pk)`
- `email (unique)`
- `password`
- `full_name`
- `role`
- `created_at`

#### `clubs`
- `id (uuid, pk)`
- `name`
- `description`
- `created_by (fk -> users.id)`
- `created_at`

#### `memberships`
- `id (uuid, pk)`
- `user_id (fk -> users.id)`
- `club_id (fk -> clubs.id)`
- `role`
- `status`
- `joined_at`
- index: `(user_id, club_id)`

#### `club_applications`
- `id (uuid, pk)`
- `name`
- `description`
- `submitted_by (fk -> users.id)`
- `status`
- `created_at`

#### `events`
- `id (uuid, pk)`
- `club_id (fk -> clubs.id)`
- `title`
- `description`
- `location`
- `start_date`, `end_date`
- `created_by (fk -> users.id)`
- `created_at`

#### `activities`
- `id (uuid, pk)`
- `club_id (fk -> clubs.id)`
- `event_id (fk -> events.id, nullable)`
- `title`
- `description`
- `date`
- `created_by (fk -> users.id)`

#### `event_registrations`
- `id (uuid, pk)`
- `user_id (fk -> users.id)`
- `event_id (fk -> events.id)`
- `registered_at`

#### `event_attendance`
- `id (uuid, pk)`
- `event_id (fk -> events.id)`
- `user_id (fk -> users.id)`
- `checked_in_at`
- `checked_in_by (fk -> users.id, nullable)`
- `method` (QR vs manual)
- unique: `(event_id, user_id)`

#### `event_attendance_windows`
- `id (uuid, pk)`
- `event_id (fk -> events.id)`
- `active (boolean)`
- `opens_before_start_minutes` / `closes_after_start_minutes`
- `token_hash (unique)`
- `created_at`, `token_rotated_at`
- unique: `(event_id)` (one window per event)

#### `comments`
Threaded comments for multiple target types (events / activities).

- `id (uuid, pk)`
- `target_type` (e.g. EVENT, ACTIVITY)
- `target_id (uuid)`
- `parent_id (uuid, nullable)` (reply threading)
- `author_id (fk -> users.id)`
- `content`
- `created_at`
- `deleted (boolean default false)`
- index: `(target_type, target_id)` and `(parent_id)`

#### `comment_likes`
- `id (uuid, pk)`
- `comment_id (fk -> comments.id)`
- `user_id (fk -> users.id)`
- unique: `(comment_id, user_id)`

#### `event_ratings`
- `id (uuid, pk)`
- `event_id (fk -> events.id)`
- `student_id (fk -> users.id)`
- `rating (int)`
- `comment (varchar 1000)`
- `created_at`, `updated_at`
- unique: `(event_id, student_id)` **(keeps last rating per student per event)**

### 5.2 Relationships (ER-style summary)

- `User 1..* Club` via `clubs.created_by`
- `User *..* Club` via `memberships`
- `Club 1..* Event` via `events.club_id`
- `Club 1..* Activity` via `activities.club_id`
- `User *..* Event` via `event_registrations`, `event_attendance`, `event_ratings`
- `Comment` is a tree via `comments.parent_id`, and targets `EVENT | ACTIVITY` through `(target_type, target_id)`.

## 6) Main features & flows

### 6.1 Authentication

- Register: `POST /api/auth/register`
- Login: `POST /api/auth/login`

UI pages:
- `/login`
- `/register`

### 6.2 Clubs

Typical responsibilities:

- Browse clubs (public or login depending on configuration)
- View club detail
- Join club (membership request)
- Club admin/staff manage members and club content

Club roles are enforced via `MembershipService` + `ClubAuthorizationService`.

### 6.3 Club applications (create-a-club approval)

- Student submits an application (`club_applications`)
- Platform admin reviews and approves/rejects

### 6.4 Events

**Registration**:

- Student registers for an event (creates `event_registrations`).

**Attendance window + QR check-in**:

- Organizer (club admin/staff) opens an attendance window and gets a raw token.
- Only a hash is stored in DB (`event_attendance_windows.token_hash`).
- Student checks in by sending the raw token; backend verifies:
  - window exists and active
  - token hash matches
  - current time is inside allowed window
  - student is registered
  - student hasn’t already checked in

**Manual check-in**:

- Organizer can check in a student by ID.
- Backend should validate:
  - student registered
  - time window constraints
  - not already checked-in

### 6.5 Ratings

- Students can rate an event.
- Because of unique `(event_id, student_id)`, re-rating updates the existing rating (keeps last).

API (from README):

- `POST /api/events/{eventId}/ratings`
- `GET /api/events/{eventId}/ratings/me`
- `GET /api/events/{eventId}/ratings/summary`

UI:

- `GET /events/{eventId}/rate` (server-rendered page)
- `POST /events/{eventId}/rate`

### 6.6 Comments (events and activities)

The system supports:

- Fetch thread for a target (event/activity)
- Create a comment on a target (root comment)
- Reply to an existing comment
- Like/unlike a comment

API controller:

- `GET /api/comments/{targetType}/{targetId}` → thread
- `GET /api/comments/{commentId}/replies` → direct replies
- `POST /api/comments/{targetType}/{targetId}` → create (root or reply if `parentId` set)
- `POST /api/comments/{commentId}/reply` → create reply explicitly
- `POST /api/comments/{commentId}/like` → toggle like

UI approach used in the project:

- Event detail shows only a small preview (e.g., top 3 comments).
- “See all comments” links to a dedicated comments page.
- On the comments page, each comment shows only the first reply with a “see more replies” expandable section.

## 7) Important design notes (based on past issues)

### 7.1 `open-in-view: false` and LazyInitializationException

Because `spring.jpa.open-in-view` is disabled, **any data needed by a view must be eagerly loaded inside the service/repository layer** (e.g., via `join fetch` queries or DTO projections).

If templates access fields like `comment.author.fullName` but the author was lazily loaded, you can get:

- `LazyInitializationException: Could not initialize proxy ... - no session`

Mitigation patterns used/expected:

- Repository methods that eagerly fetch required relationships.
- DTO mapping inside transactional service methods.

### 7.2 Comment threading

- `comments.parent_id` forms a tree.
- “Thread” endpoints normally return:
  - root comments (parent is null)
  - their immediate/preview replies
  - counts or indicators to fetch more replies

### 7.3 Attendance token security

- Raw tokens should not be stored.
- Only token hash is stored, enabling rotation.

## 8) Key packages & files (quick reference)

### Entry point
- `src/main/java/io/droidevs/mclub/MClubApplication.java`

### Security
- `src/main/java/io/droidevs/mclub/security/SecurityConfig.java`
- `src/main/java/io/droidevs/mclub/security/JwtAuthenticationFilter.java`
- `src/main/java/io/droidevs/mclub/security/JwtTokenProvider.java`
- `src/main/java/io/droidevs/mclub/security/CustomUserDetailsService.java`

### Migration / schema
- `src/main/resources/db/migration/V1__schema.sql`

### Controllers (examples)
- Web (Thymeleaf):
  - `WebController`
  - `WebEventRegistrationController`
  - `WebEventCheckInController`
  - `WebEventRatingController`
  - `WebCommentsController`
  - `WebClubAdminController`
  - `WebClubApplicationController`
- API (JSON):
  - `AuthController`
  - `ClubController`
  - `EventController`
  - `AttendanceController`
  - `EventRatingController`
  - `CommentController`
  - `ActivityController`

---

If you want this document to include a full **endpoint table** per controller (method, path, role, template, service), say so and I’ll extend it.~~
