package vn.t3tarot.api.identity.session.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.t3tarot.api.identity.session.entity.ActiveSession;
import vn.t3tarot.api.identity.session.entity.RotatedSession;
import vn.t3tarot.api.identity.shared.repository.SqlTimestamps;

@Repository
@RequiredArgsConstructor
public class AuthSessionHotPathRepository {

    private static final String INSERT = """
            INSERT INTO auth_session
                (id, user_id, identity_id, refresh_token_hash, user_agent, ip, created_at, expires_at)
            VALUES (:id, :userId, :identityId, :refreshTokenHash, :userAgent, CAST(:ip AS inet), :now, :expiresAt)
            """;

    private static final String ROTATE = """
            UPDATE auth_session
            SET previous_refresh_token_hash = CASE WHEN refresh_token_hash = :presentedHash
                                                   THEN refresh_token_hash ELSE previous_refresh_token_hash END,
                rotated_at = CASE WHEN refresh_token_hash = :presentedHash THEN :now ELSE rotated_at END,
                refresh_token_hash = :newHash
            WHERE revoked_at IS NULL
              AND expires_at > :now
              AND (refresh_token_hash = :presentedHash
                   OR (previous_refresh_token_hash = :presentedHash AND rotated_at > :graceCutoff))
              AND EXISTS (SELECT 1 FROM app_user u WHERE u.id = auth_session.user_id AND u.status = 'ACTIVE')
            RETURNING id, user_id
            """;

    private static final String REVOKE_BY_PREVIOUS_HASH = """
            UPDATE auth_session SET revoked_at = :now
            WHERE previous_refresh_token_hash = :presentedHash AND revoked_at IS NULL
            """;

    private static final String REVOKE_BY_HASH = """
            UPDATE auth_session SET revoked_at = :now
            WHERE revoked_at IS NULL
              AND (refresh_token_hash = :hash OR previous_refresh_token_hash = :hash)
            """;

    private static final String REVOKE_ALL_OF_USER = """
            UPDATE auth_session SET revoked_at = :now WHERE user_id = :userId AND revoked_at IS NULL
            """;

    private static final String REVOKE_OF_USER = """
            UPDATE auth_session SET revoked_at = :now
            WHERE id = :sessionId AND user_id = :userId AND revoked_at IS NULL
            """;

    private static final String EXISTS_OF_USER = """
            SELECT EXISTS (SELECT 1 FROM auth_session WHERE id = :sessionId AND user_id = :userId)
            """;

    private static final String FIND_ACTIVE = """
            SELECT id, created_at, expires_at, user_agent, id = CAST(:currentSessionId AS uuid) AS is_current
            FROM auth_session
            WHERE user_id = :userId AND revoked_at IS NULL AND expires_at > :now
            ORDER BY created_at DESC
            """;

    private final JdbcClient jdbcClient;

    public void insert(
            UUID id,
            UUID userId,
            UUID identityId,
            byte[] refreshTokenHash,
            String userAgent,
            String ipAddress,
            Instant now,
            Instant expiresAt) {
        jdbcClient.sql(INSERT)
                .param("id", id)
                .param("userId", userId)
                .param("identityId", identityId)
                .param("refreshTokenHash", refreshTokenHash)
                .param("userAgent", userAgent)
                .param("ip", ipAddress)
                .param("now", SqlTimestamps.of(now))
                .param("expiresAt", SqlTimestamps.of(expiresAt))
                .update();
    }

    public Optional<RotatedSession> rotate(byte[] presentedHash, byte[] newHash, Instant now, Instant graceCutoff) {
        return jdbcClient.sql(ROTATE)
                .param("presentedHash", presentedHash)
                .param("newHash", newHash)
                .param("now", SqlTimestamps.of(now))
                .param("graceCutoff", SqlTimestamps.of(graceCutoff))
                .query((resultSet, rowNumber) -> new RotatedSession(
                        resultSet.getObject("id", UUID.class), resultSet.getObject("user_id", UUID.class)))
                .optional();
    }

    public int revokeByPreviousHash(byte[] presentedHash, Instant now) {
        return jdbcClient.sql(REVOKE_BY_PREVIOUS_HASH)
                .param("presentedHash", presentedHash)
                .param("now", SqlTimestamps.of(now))
                .update();
    }

    public int revokeByHash(byte[] hash, Instant now) {
        return jdbcClient.sql(REVOKE_BY_HASH)
                .param("hash", hash)
                .param("now", SqlTimestamps.of(now))
                .update();
    }

    public int revokeAllOfUser(UUID userId, Instant now) {
        return jdbcClient.sql(REVOKE_ALL_OF_USER)
                .param("userId", userId)
                .param("now", SqlTimestamps.of(now))
                .update();
    }

    public boolean revokeOfUser(UUID userId, UUID sessionId, Instant now) {
        return jdbcClient.sql(REVOKE_OF_USER)
                        .param("userId", userId)
                        .param("sessionId", sessionId)
                        .param("now", SqlTimestamps.of(now))
                        .update()
                > 0;
    }

    public boolean existsOfUser(UUID userId, UUID sessionId) {
        return Boolean.TRUE.equals(jdbcClient.sql(EXISTS_OF_USER)
                .param("userId", userId)
                .param("sessionId", sessionId)
                .query(Boolean.class)
                .single());
    }

    public List<ActiveSession> findActive(UUID userId, UUID currentSessionId, Instant now) {
        return jdbcClient.sql(FIND_ACTIVE)
                .param("userId", userId)
                .param("currentSessionId", currentSessionId)
                .param("now", SqlTimestamps.of(now))
                .query((resultSet, rowNumber) -> new ActiveSession(
                        resultSet.getObject("id", UUID.class),
                        SqlTimestamps.instantOrNull(resultSet, "created_at"),
                        SqlTimestamps.instantOrNull(resultSet, "expires_at"),
                        resultSet.getString("user_agent"),
                        resultSet.getBoolean("is_current")))
                .list();
    }
}
