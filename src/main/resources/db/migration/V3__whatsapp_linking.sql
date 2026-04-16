-- WhatsApp linking table (maps verified WhatsApp phone -> user)
create table if not exists user_whatsapp_link (
    user_id uuid primary key,
    phone_e164 varchar(32) not null unique,
    verified_at timestamp not null,
    constraint fk_user_whatsapp_link_user foreign key (user_id) references users(id) on delete cascade
);

create index if not exists idx_user_whatsapp_link_phone on user_whatsapp_link(phone_e164);

-- OTP challenges for linking (short-lived)
create table if not exists whatsapp_link_otp (
    id uuid primary key,
    phone_e164 varchar(32) not null,
    code_hash varchar(128) not null,
    expires_at timestamp not null,
    consumed_at timestamp,
    created_at timestamp not null,
    constraint uk_whatsapp_link_otp_phone unique(phone_e164)
);

create index if not exists idx_whatsapp_link_otp_expires on whatsapp_link_otp(expires_at);

