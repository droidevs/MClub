create table if not exists event_attendance (
    id uuid primary key,

    event_id uuid not null,
    user_id uuid not null,
    checked_in_by uuid,

    checked_in_at timestamp not null,
    method varchar(50) not null,

    constraint uk_event_attendance_event_user
    unique (event_id, user_id),

    constraint fk_attendance_event
    foreign key (event_id) references events(id),

    constraint fk_attendance_user
    foreign key (user_id) references users(id),

    constraint fk_attendance_checked_by
    foreign key (checked_in_by) references users(id)
    );

-- Indexes (critical for performance)
create index if not exists idx_attendance_event on event_attendance(event_id);
create index if not exists idx_attendance_user on event_attendance(user_id);
create index if not exists idx_attendance_checked_by on event_attendance(checked_in_by);
create index if not exists idx_attendance_checked_at on event_attendance(checked_in_at);