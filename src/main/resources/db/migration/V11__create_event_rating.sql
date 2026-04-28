create table if not exists event_ratings (
    id uuid primary key,

    event_id uuid not null,
    student_id uuid not null,

    rating int not null,
    comment varchar(1000),

    created_at timestamp not null,
    updated_at timestamp,

    constraint uk_event_rating_event_student
    unique (event_id, student_id),

    constraint fk_event_rating_event
    foreign key (event_id) references events(id),

    constraint fk_event_rating_student
    foreign key (student_id) references users(id)
    );

-- Indexes for performance & analytics
create index if not exists idx_event_rating_event on event_ratings(event_id);
create index if not exists idx_event_rating_student on event_ratings(student_id);
create index if not exists idx_event_rating_rating on event_ratings(rating);
create index if not exists idx_event_rating_created_at on event_ratings(created_at);