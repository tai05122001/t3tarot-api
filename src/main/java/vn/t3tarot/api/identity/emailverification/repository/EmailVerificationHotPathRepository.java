package vn.t3tarot.api.identity.emailverification.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.t3tarot.api.identity.shared.repository.SqlTimestamps;

@Repository
@RequiredArgsConstructor
public class EmailVerificationHotPathRepository {

    private static final String INSERT_IF_NO_RECENT = """
            INSERT INTO email_verification (id, user_id, identity_id, token_hash, created_at, expires_at)
            SELECT :id, :userId, :identityId, :tokenHash, :now, :expiresAt
            WHERE NOT EXISTS (
                SELECT 1 FROM email_verification
                WHERE identity_id = :identityId AND created_at > :recentCutoff)
            """;

    private static final String CONSUME = """
            WITH consumed AS (
                UPDATE email_verification SET consumed_at = :now
                WHERE token_hash = :tokenHash AND consumed_at IS NULL AND expires_at > :now
                RETURNING identity_id)
            UPDATE user_identity ui
            SET verified_at = COALESCE(ui.verified_at, :now)
            FROM consumed
            WHERE ui.id = consumed.identity_id
            RETURNING ui.id
            """;

    private static final String FIND_STATE = """
            SELECT ev.expires_at, ev.consumed_at, ui.verified_at
            FROM email_verification ev
            JOIN user_identity ui ON ui.id = ev.identity_id
            WHERE ev.token_hash = :tokenHash
            """;

    private final JdbcClient jdbcClient;

    public boolean insertIfNoRecent(
            UUID id,
            UUID userId,
            UUID identityId,
            byte[] tokenHash,
            Instant now,
            Instant expiresAt,
            Instant recentCutoff) {
        return jdbcClient.sql(INSERT_IF_NO_RECENT)
                        .param("id", id)
                        .param("userId", userId)
                        .param("identityId", identityId)
                        .param("tokenHash", tokenHash)
                        .param("now", SqlTimestamps.of(now))
                        .param("expiresAt", SqlTimestamps.of(expiresAt))
                        .param("recentCutoff", SqlTimestamps.of(recentCutoff))
                        .update()
                > 0;
    }

    public Optional<TokenState> findState(byte[] tokenHash) {
        return jdbcClient.sql(FIND_STATE)
                .param("tokenHash", tokenHash)
                .query((resultSet, rowNumber) -> new TokenState(
                        SqlTimestamps.instantOrNull(resultSet, "expires_at"),
                        SqlTimestamps.instantOrNull(resultSet, "consumed_at"),
                        SqlTimestamps.instantOrNull(resultSet, "verified_at")))
                .optional();
    }

    public record TokenState(Instant expiresAt, Instant consumedAt, Instant identityVerifiedAt) {
    }

    public Optional<UUID> consume(byte[] tokenHash, Instant now) {
        return jdbcClient.sql(CONSUME)
                .param("tokenHash", tokenHash)
                .param("now", SqlTimestamps.of(now))
                .query(UUID.class)
                .optional();
    }
}
