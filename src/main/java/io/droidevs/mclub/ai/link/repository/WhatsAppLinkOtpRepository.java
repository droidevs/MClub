package io.droidevs.mclub.ai.link.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WhatsAppLinkOtpRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public WhatsAppLinkOtpRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsertChallenge(String phoneE164, String codeHash, Instant expiresAt) {
        String sql = """
                insert into whatsapp_link_otp(id, phone_e164, code_hash, expires_at, consumed_at, created_at)
                values (:id, :p, :h, :e, null, :c)
                on conflict (phone_e164) do update
                  set code_hash = excluded.code_hash,
                      expires_at = excluded.expires_at,
                      consumed_at = null,
                      created_at = excluded.created_at
                """;

        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("p", phoneE164)
                .addValue("h", codeHash)
                .addValue("e", Timestamp.from(expiresAt))
                .addValue("c", Timestamp.from(Instant.now())));
    }

    public Optional<OtpRow> findActiveByPhone(String phoneE164) {
        String sql = """
                select id, phone_e164, code_hash, expires_at, consumed_at
                from whatsapp_link_otp
                where phone_e164 = :p
                """;
        var rows = jdbc.query(sql, new MapSqlParameterSource("p", phoneE164), (rs, i) -> new OtpRow(
                UUID.fromString(rs.getString("id")),
                rs.getString("phone_e164"),
                rs.getString("code_hash"),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("consumed_at") == null ? null : rs.getTimestamp("consumed_at").toInstant()
        ));
        return rows.stream().findFirst();
    }

    public void markConsumed(UUID id) {
        String sql = "update whatsapp_link_otp set consumed_at = :t where id = :id";
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("t", Timestamp.from(Instant.now())));
    }

    public record OtpRow(UUID id, String phoneE164, String codeHash, Instant expiresAt, Instant consumedAt) {}
}

