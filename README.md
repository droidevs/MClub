# MClub — Club Management Platform + Attendance + Ratings + AI (WhatsApp/RAG)

MClub is a Spring Boot application for managing clubs and their events. It supports **club membership workflows**, **event registration**, **QR attendance check-in windows**, **event ratings**, **threaded comments with likes**, and an optional **AI assistant** that can answer questions and perform actions via **WhatsApp-style webhooks**.

This README is intentionally architecture-heavy: it explains the main "landscapes" (modules/packages) and the end-to-end flows.

---

## Table of contents

- [1) Key capabilities](#1-key-capabilities)
- [2) Tech stack](#2-tech-stack)
- [3) High-level architecture (layers)](#3-high-level-architecture-layers)
- [4) Package / module map (landscapes)](#4-package--module-map-landscapes)
- [5) Runtime entry points (UI, API, Webhooks)](#5-runtime-entry-points-ui-api-webhooks)
- [6) Core business flows (end-to-end)](#6-core-business-flows-end-to-end)
  - [6.1 Authentication (JWT)](#61-authentication-jwt)
  - [6.2 Clubs + memberships (club-scoped roles)](#62-clubs--memberships-club-scoped-roles)
  - [6.3 Events + registrations](#63-events--registrations)
  - [6.4 Attendance (QR windows + manual check-in)](#64-attendance-qr-windows--manual-check-in)
  - [6.5 Event ratings](#65-event-ratings)
  - [6.6 Comments + likes (threaded)](#66-comments--likes-threaded)
  - [6.7 Club applications (apply-to-create)](#67-club-applications-apply-to-create)
  - [6.8 AI Assistant (RAG + tools + WhatsApp adapters)](#68-ai-assistant-rag--tools--whatsapp-adapters)
- [7) Data model (Flyway baseline)](#7-data-model-flyway-baseline)
- [8) Security model](#8-security-model)
- [9) Configuration](#9-configuration)
- [10) How to run (local)](#10-how-to-run-local)
- [11) Troubleshooting](#11-troubleshooting)
- [12) Related docs in this repo](#12-related-docs-in-this-repo)

---

## 1) Key capabilities

**Core platform**
- Create and browse clubs
- Join a club (membership request) + approval workflow
- Club-scoped roles: a user can be **ADMIN** in one club and **MEMBER** in another

**Events**
- Create events under a club
- Students can register for events

**Attendance**
- Organizers open an **attendance window** for an event and receive a **raw token** suitable for turning into a QR code
- Students check in with the token (server stores **only a hash**)
- Organizers can also do **manual check-ins**

**Engagement**
- Students can rate events (one rating per student per event; updates overwrite)
- Threaded comments for events/activities + likes

**AI assistant (optional)**
- Webhook endpoint can receive chat messages
- The agent retrieves context from the platform, decides whether to answer or to invoke a tool, executes tools that call the existing service layer, and sends a response (logging sender by default; Meta Cloud sender optionally)

---

## 2) Tech stack

- Java (Gradle toolchain set to **Java 21**)
- Spring Boot (see `build.gradle`)
- Spring MVC (REST and Thymeleaf controller endpoints)
- Thymeleaf templates + `thymeleaf-extras-springsecurity6`
- Spring Data JPA / Hibernate
- Flyway migrations (`src/main/resources/db/migration/V1__create_user_table.sql`)
- PostgreSQL (compose uses `pgvector/pgvector:pg16`)
- Spring Security + JWT
- MapStruct + Lombok
- WebClient (for OpenAI-compatible LLM calling)

---

## 3) High-level architecture (layers)

MClub follows a fairly standard layered Spring architecture:

1. **Controller layer** (`io.droidevs.mclub.controller`)
   - `@Controller` pages (Thymeleaf)
   - `@RestController` APIs (`/api/**`)
2. **Service layer** (`io.droidevs.mclub.service`)
   - business rules + authorization checks
3. **Repository layer** (`io.droidevs.mclub.repository`)
   - Spring Data repositories + custom fetch queries
4. **Domain layer** (`io.droidevs.mclub.domain`)
   - JPA entities + enums
5. **DTO/mapping** (`io.droidevs.mclub.dto`, `io.droidevs.mclub.mapper`)
   - DTOs protect APIs and views from entity leakage and lazy-loading
6. **Cross-cutting**
   - `io.droidevs.mclub.security` for JWT + filter chain
   - exceptions for consistent 403/404 behavior
   - AI module under `io.droidevs.mclub.ai` (see below)

Important setting:

- `spring.jpa.open-in-view: false` in `application.yml`.
  - This forces services/repositories to fetch everything the UI/API needs.
  - It avoids the "it works in dev but fails in prod" lazy-loading trap.

---

## 4) Package / module map (landscapes)

### Core (non-AI)

- `io.droidevs.mclub.controller`
  - REST API controllers: auth, clubs, events, membership, attendance, ratings, comments
  - Web controllers render templates for the UI (dashboard, club admin pages, event pages, etc.)

- `io.droidevs.mclub.service`
  - the authoritative business rules:
    - membership and club-scoped authorization
    - event registration logic
    - attendance window creation + token hashing
    - rating rules
    - comment thread building and like toggling

- `io.droidevs.mclub.security`
  - `SecurityConfig` defines route access rules and registers the JWT filter
  - `JwtTokenProvider` issues and validates tokens
  - `JwtAuthenticationFilter` extracts and authenticates JWTs for requests

### AI / Conversational assistant (WhatsApp/RAG)

All AI code lives under `io.droidevs.mclub.ai`.

- `io.droidevs.mclub.ai.webhook.whatsapp`
  - inbound webhook controllers: `/webhooks/whatsapp` (+ Meta Cloud webhook endpoints)
  - outbound adapter: `WhatsAppSender` interface
  - implementations:
    - `LoggingWhatsAppSender` (dev default)
    - `MetaCloudWhatsAppSender` (Meta WhatsApp Cloud API)
  - security/hardening helpers:
    - raw body caching filter
    - signature verification
    - processed-message store (dedupe)

- `io.droidevs.mclub.ai.conversation`
  - `ConversationService` is the inbound chat entry point (async)
  - `ConversationStore` keeps per-conversation session history (current impl is in-memory)
  - `WhatsappIdentityService` maps a phone number to a platform user id/email (via link service)

- `io.droidevs.mclub.ai.rag`
  - `RagService` orchestrates the chat logic (currently delegates to an agent loop executor)
  - building blocks: tool calls, retrieval context, prompt building

- `io.droidevs.mclub.ai.llm`
  - `OpenAiLlmClient` is an OpenAI-compatible client that supports tool/function calling
  - enabled via `mclub.ai.openai.enabled=true`

- `io.droidevs.mclub.ai.tools`
  - set of tools the model can invoke (`Tool` + `ToolRegistry`)
  - examples include: list/search clubs & events, register for event, check-in, rate, comment, like

---

## 5) Runtime entry points (UI, API, Webhooks)

### Server-rendered UI (Thymeleaf)

Templates live in `src/main/resources/templates`.

Typical routes (examples; exact wiring is in the `Web*Controller` classes):
- `GET /` dashboard (requires login)
- `GET /clubs` club listing (public)
- `GET /events` event listing (public)
- `GET /events/{id}` event detail (requires login)
### REST API

APIs live under `/api/**` and are used by the UI JavaScript and external clients.

A few notable endpoints (see also `README-api.md`):
- Auth
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- Events
  - `GET /api/events/club/{clubId}` (public read)
  - `POST /api/events/{eventId}/registrations` (varies by controller; see `EventController`)
- Attendance
  - `POST /api/events/{eventId}/attendance/window` (organizer)
  - `POST /api/events/{eventId}/attendance/window/close` (organizer)
  - `GET /api/events/{eventId}/attendance` (organizer)
  - `POST /api/attendance/check-in` (student)
  - `POST /api/events/{eventId}/attendance/check-in/{studentId}` (organizer)
- Ratings
  - `POST /api/events/{eventId}/ratings` (student)
  - `GET /api/events/{eventId}/ratings/summary` (public)
  - `GET /api/events/{eventId}/ratings` (organizer)
- Comments
  - `GET /api/comments/**` (public reads)

### Webhooks (AI)

- Provider-agnostic webhook: `POST /webhooks/whatsapp`
  - Maps provider payload into `WhatsAppWebhookRequest { conversationId, fromPhoneE164, text }`
  - Dispatches to `ConversationService.handleIncomingMessage(...)` asynchronously

---

## 6) Core business flows (end-to-end)

This section explains the most important flows as step-by-step narratives.

### 6.1 Authentication (JWT)

**Goal:** make authentication stateless for both REST calls and browser flows.

Flow:
1. User registers/logs in via `/api/auth/*` (or via web forms that call the same logic)
2. Server issues a JWT
3. Later requests authenticate via:
   - `Authorization: Bearer <token>` header (typical for API clients)
   - and/or a cookie (depending on UI auth controller behavior)
4. `JwtAuthenticationFilter` validates and sets Spring Security `Authentication`

Security behavior:
- `/api/**` unauthenticated → `401`
- browser unauthenticated → redirect to `/login`

### 6.2 Clubs + memberships (club-scoped roles)

**Concept:** global role is "platform-wide", but club permissions derive from `memberships`.

- Global roles (platform): see `io.droidevs.mclub.security.Role` (commonly includes `PLATFORM_ADMIN`, `STUDENT`)
- Club roles (scoped per club): stored on `memberships.role` (`ADMIN | STAFF | MEMBER`)
- Membership status: `PENDING | APPROVED | REJECTED`

Typical join/approve flow:
1. Student clicks **Join club** → creates a `memberships` record with `status=PENDING`
2. Club admin/staff reviews membership requests in the club admin pages
3. Approve → status becomes `APPROVED`
4. Authorization checks for club admin actions rely on membership role + status

### 6.3 Events + registrations

Event creation flow:
1. Club admin/staff creates event (web form or API)
2. Event is stored in `events` table, with `club_id` and `created_by`

Registration flow:
1. Student registers for an event → `event_registrations` row
2. Registration is used later to gate attendance & ratings (you must be registered)

### 6.4 Attendance (QR windows + manual check-in)

This is one of the most important flows.

**Data model**
- `event_attendance_windows`: one per event, holding the window config and a **token hash**
- `event_attendance`: the actual check-in records, unique `(event_id, user_id)`

**Organizer: open (or rotate) a window**
1. Organizer calls `POST /api/events/{eventId}/attendance/window` with:
   - `opensBeforeStartMinutes`
   - `closesAfterStartMinutes`
2. Service generates a **raw token** and stores only `sha256(token)` (or equivalent hash)
3. Response returns the raw token; organizer generates a QR with it

**Student: check-in by scanning QR**
1. Student scans QR → gets raw token
2. Student submits `POST /api/attendance/check-in` `{ "token": "..." }`
3. Backend:
   - finds matching active window by hash
   - validates time window relative to event start
   - validates student is registered
   - inserts attendance record if not present (idempotent via unique constraint)

**Organizer: manual check-in**
1. Organizer calls `POST /api/events/{eventId}/attendance/check-in/{studentId}`
2. Service verifies organizer can manage the event (platform admin OR club-scoped ADMIN/STAFF)
3. Inserts attendance marked as manual

### 6.5 Event ratings

**Data model**
- `event_ratings` has a unique constraint `(event_id, student_id)` → update overwrites previous

Flow:
1. Student submits `POST /api/events/{eventId}/ratings` with `{ rating: 1..5, comment?: string }`
2. Backend enforces that the user can rate (student role; typically registered and attended)
3. Rating is created or updated
4. Public summary endpoint: `GET /api/events/{eventId}/ratings/summary`

### 6.6 Comments + likes (threaded)

**Data model**
- `comments` is a tree (via `parent_id`) and targets entities via `(target_type, target_id)`
- `comment_likes` is a toggle table unique by `(comment_id, user_id)`

Thread rendering flow:
1. API fetch loads all comments for a target and authors
2. Service builds an in-memory tree and attaches computed fields:
   - like count
   - liked-by-me

### 6.7 Club applications (apply-to-create)

Flow:
1. Student submits application → `club_applications` row with status
2. Platform admin reviews applications
3. Approve may create a real `clubs` record + membership assignments

### 6.8 AI Assistant (RAG + tools + WhatsApp adapters)

This flow is the conversational entry point.

**Inbound webhook**
1. Provider calls `POST /webhooks/whatsapp`
2. `WhatsAppWebhookController.receive(...)` maps inbound JSON into `WhatsAppWebhookRequest`
3. Controller fires async: `ConversationService.handleIncomingMessage(...)`

**Conversation/session layer** (`ConversationService`)
1. Loads/creates a `ConversationSession` from `ConversationStore`
2. Appends user message to session history
3. Builds `ConversationContext` from phone number using `WhatsappIdentityService`
   - if linked: context includes `userId` + `email`
4. Calls `RagService.handle(session, ctx)`
5. Appends assistant message back into the session
6. Sends response via `WhatsAppSender.sendText(...)`

**RAG Agent**
- `RagService` delegates to an agent loop (multi-step tool use)
- The LLM client may:
  - answer directly, or
  - return a tool call (function calling) such as `search_events`, `register_event`, `checkin_event`, `rate_event`, `add_comment`
- Tools execute against the existing service layer (so business rules remain centralized)

**OpenAI integration**
- Implemented in `OpenAiLlmClient`
- Uses `/v1/chat/completions` and provides tool schemas
- If API key is missing, the client returns a user-friendly configuration message

---

## 7) Data model (Flyway baseline)

Flyway baseline migration: `src/main/resources/db/migration/V1__create_user_table.sql`.

Key tables:
- `users`
- `clubs`
- `memberships`
- `club_applications`
- `events`
- `activities`
- `event_registrations`
- `event_attendance`
- `event_attendance_windows`
- `comments`
- `comment_likes`
- `event_ratings`

---

## 8) Security model

Security config is in `io.droidevs.mclub.security.SecurityConfig`.

High-level notes:
- Stateless JWT auth (`SessionCreationPolicy.STATELESS`)
- CSRF disabled (common for pure JWT APIs; re-evaluate if adding cookie-based browser mutations)
- Different unauthenticated behavior for:
  - `/api/**` → `401 Unauthorized`
  - browser routes → redirect to `/login`

Public endpoints (examples):
- static assets
- `GET /clubs`, `GET /events`
- `/api/auth/**`
- reads: `GET /api/events/*/ratings/summary`, `GET /api/comments/**`

Club-admin routes:
- `/club-admin/**` requires login, then deeper club-scoped checks happen in controllers/services.

---

## 9) Configuration

Main config: `src/main/resources/application.yml`.

### Database
- default datasource: `jdbc:postgresql://localhost:5432/mclub_db`
- docker compose: `compose.yaml` (postgres + pgvector image)

### JWT
- `app.jwt.secret`
- `app.jwt.expiration`

### AI / OpenAI
- `mclub.ai.openai.enabled`
- `mclub.ai.openai.base-url`
- `mclub.ai.openai.api-key` (ideally set via `OPENAI_API_KEY` environment variable)
- `mclub.ai.openai.model` (default shown: `gpt-4o-mini`)

### WhatsApp (Meta Cloud)
- `mclub.whatsapp.meta.*`
- signature verification toggle under `mclub.whatsapp.meta.signature-verification.enabled`

---

## 10) How to run (local)

### Prerequisites
- JDK 21
- Docker (optional, for Postgres)

### Start Postgres (Docker)
Use `compose.yaml` (postgres + pgvector). Configure `spring.datasource.*` accordingly.

### Run the app
Use Gradle wrapper.

- Application port: `server.port` (default `8080`)
- UI routes are served from the same server

---

## 11) Troubleshooting

- **Flyway/JPA mismatch**: JPA is set to `ddl-auto: validate`, so the DB schema must match the entities.
  - If you changed entities, create a new Flyway migration.

- **Lazy-loading errors in templates**: `open-in-view` is off.
  - Fix by fetching required associations in service/repository methods.

- **OpenAI errors**:
  - If `mclub.ai.openai.enabled=true` and the API key is missing/invalid, the assistant returns a readable error.
  - Confirm `OPENAI_API_KEY` is present and the selected model is accessible.

---

## 12) Related docs in this repo

- `README-api.md` — request examples and endpoint notes
- `docs/SYSTEM_OVERVIEW.md` — deeper overview of modules and schema
- `docs/PROJECT_ARCHITECTURE.md` — detailed architecture and logic notes
- `docs/ai-rag-whatsapp.md` — RAG + WhatsApp assistant design
- `UI-ATTENDANCE-RATING.md` — UI notes for attendance and rating screens
- `docs/qr-and-attendance.md` — additional QR/attendance notes

