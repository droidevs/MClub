create table if not exists event_attendance_windows (
    id uuid primary key,

    event_id uuid not null,
    active boolean not null default false,

    opens_before_start_minutes int not null,
    closes_after_start_minutes int not null,

    token_hash varchar(64) not null,

    token_rotated_at timestamp,

    created_at timestamp not null,

    constraint uk_att_window_event unique (event_id),
    constraint uk_att_window_token_hash unique (token_hash),

    constraint fk_att_window_event
    foreign key (event_id) references events(id)
    );

-- Indexes (critical for performance)
create index if not exists idx_att_window_event on event_attendance_windows(event_id);
create index if not exists idx_att_window_active on event_attendance_windows(active);
create index if not exists idx_att_window_token_hash on event_attendance_windows(token_hash);