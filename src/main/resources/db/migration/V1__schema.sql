-- Flyway baseline schema for MClub
--
-- Note: this is a simplified baseline that covers the tables used by the app,
-- including comments/likes and event ratings.

create table if not exists users (
    id uuid primary key,
    email varchar(255) not null unique,
    password varchar(255) not null,
    full_name varchar(255),
    role varchar(50),
    created_at timestamp
);

create table if not exists clubs (
    id uuid primary key,
    name varchar(255),
    description varchar(255),
    created_by uuid,
    created_at timestamp,
    constraint fk_clubs_created_by foreign key (created_by) references users(id)
);

create table if not exists memberships (
    id uuid primary key,
    user_id uuid,
    club_id uuid,
    role varchar(50),
    status varchar(50),
    joined_at timestamp,
    constraint fk_memberships_user foreign key (user_id) references users(id),
    constraint fk_memberships_club foreign key (club_id) references clubs(id)
);

create index if not exists idx_memberships_user_club on memberships(user_id, club_id);

create table if not exists club_applications (
    id uuid primary key,
    name varchar(255) not null,
    description varchar(255) not null,
    submitted_by uuid not null,
    status varchar(50) not null,
    created_at timestamp,
    constraint fk_club_applications_submitted_by foreign key (submitted_by) references users(id)
);

create table if not exists events (
    id uuid primary key,
    club_id uuid,
    title varchar(255),
    description varchar(255),
    location varchar(255),
    start_date timestamp,
    end_date timestamp,
    created_by uuid,
    created_at timestamp,
    constraint fk_events_club foreign key (club_id) references clubs(id),
    constraint fk_events_created_by foreign key (created_by) references users(id)
);

create index if not exists idx_events_club on events(club_id);

create table if not exists activities (
    id uuid primary key,
    club_id uuid,
    event_id uuid,
    title varchar(255),
    description varchar(255),
    date timestamp,
    created_by uuid,
    constraint fk_activities_club foreign key (club_id) references clubs(id),
    constraint fk_activities_event foreign key (event_id) references events(id),
    constraint fk_activities_created_by foreign key (created_by) references users(id)
);

create index if not exists idx_activities_club on activities(club_id);

create table if not exists event_registrations (
    id uuid primary key,
    user_id uuid,
    event_id uuid,
    registered_at timestamp,
    constraint fk_event_reg_user foreign key (user_id) references users(id),
    constraint fk_event_reg_event foreign key (event_id) references events(id)
);

create index if not exists idx_event_reg_event on event_registrations(event_id);

create table if not exists event_attendance (
    id uuid primary key,
    event_id uuid not null,
    user_id uuid not null,
    checked_in_at timestamp not null,
    checked_in_by uuid,
    method varchar(80) not null,
    constraint uk_event_attendance_event_user unique(event_id, user_id),
    constraint fk_event_att_event foreign key (event_id) references events(id),
    constraint fk_event_att_user foreign key (user_id) references users(id),
    constraint fk_event_att_checked_by foreign key (checked_in_by) references users(id)
);

create index if not exists idx_event_att_event on event_attendance(event_id);

create table if not exists event_attendance_windows (
    id uuid primary key,
    event_id uuid not null,
    active boolean not null,
    opens_before_start_minutes int not null,
    closes_after_start_minutes int not null,
    token_hash varchar(64) not null,
    created_at timestamp,
    token_rotated_at timestamp,
    constraint uk_att_window_event unique(event_id),
    constraint uk_att_window_token_hash unique(token_hash),
    constraint fk_att_window_event foreign key (event_id) references events(id)
);

-- Comments (event/activity)
create table if not exists comments (
    id uuid primary key,
    target_type varchar(40) not null,
    target_id uuid not null,
    parent_id uuid,
    author_id uuid not null,
    content varchar(2000) not null,
    created_at timestamp,
    deleted boolean not null default false,
    constraint fk_comments_author foreign key (author_id) references users(id)
);

create index if not exists idx_comments_target on comments(target_type, target_id);
create index if not exists idx_comments_parent on comments(parent_id);

create table if not exists comment_likes (
    id uuid primary key,
    comment_id uuid not null,
    user_id uuid not null,
    created_at timestamp,
    constraint uk_comment_like_comment_user unique(comment_id, user_id),
    constraint fk_comment_likes_comment foreign key (comment_id) references comments(id),
    constraint fk_comment_likes_user foreign key (user_id) references users(id)
);

create index if not exists idx_comment_likes_comment on comment_likes(comment_id);

-- Event ratings
create table if not exists event_ratings (
    id uuid primary key,
    event_id uuid not null,
    student_id uuid not null,
    rating int not null,
    comment varchar(1000),
    created_at timestamp,
    updated_at timestamp,
    constraint uk_event_rating_event_student unique(event_id, student_id),
    constraint fk_event_rating_event foreign key (event_id) references events(id),
    constraint fk_event_rating_student foreign key (student_id) references users(id)
);

create index if not exists idx_event_ratings_event on event_ratings(event_id);
