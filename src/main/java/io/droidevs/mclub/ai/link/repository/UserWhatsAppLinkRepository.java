package io.droidevs.mclub.ai.link.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserWhatsAppLinkRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UserWhatsAppLinkRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<LinkRow> findByPhone(String phoneE164) {
        String sql = "select user_id, phone_e164, verified_at from user_whatsapp_link where phone_e164 = :p";
        var rows = jdbc.query(sql, new MapSqlParameterSource("p", phoneE164), (rs, i) -> new LinkRow(
                UUID.fromString(rs.getString("user_id")),
                rs.getString("phone_e164"),
                rs.getTimestamp("verified_at").toInstant()
        ));
        return rows.stream().findFirst();
    }

    public void upsert(UUID userId, String phoneE164, Instant verifiedAt) {
        String sql = """
                insert into user_whatsapp_link(user_id, phone_e164, verified_at)
                values (:u, :p, :v)
                on conflict (user_id) do update set phone_e164 = excluded.phone_e164, verified_at = excluded.verified_at
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("u", userId)
                .addValue("p", phoneE164)
                .addValue("v", Timestamp.from(verifiedAt)));
    }

    public record LinkRow(UUID userId, String phoneE164, Instant verifiedAt) {}
}

