create table if not exists event_registrations (
    id uuid primary key,

    user_id uuid not null,
    event_id uuid not null,

    registered_at timestamp not null,

    constraint uk_event_registration_event_user
    unique (event_id, user_id),

    constraint fk_event_registration_user
    foreign key (user_id) references users(id),

    constraint fk_event_registration_event
    foreign key (event_id) references events(id)
    );

-- Indexes (performance-critical for event pages)
create index if not exists idx_event_registration_event on event_registrations(event_id);
create index if not exists idx_event_registration_user on event_registrations(user_id);
create index if not exists idx_event_registration_date on event_registrations(registered_at);