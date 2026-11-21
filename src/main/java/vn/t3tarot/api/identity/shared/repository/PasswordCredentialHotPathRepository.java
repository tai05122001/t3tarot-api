package vn.t3tarot.api.identity.shared.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.t3tarot.api.identity.shared.entity.PasswordLogin;
import vn.t3tarot.api.identity.shared.enumeration.AccountStatus;

@Repository
@RequiredArgsConstructor
public class PasswordCredentialHotPathRepository {

    private static final String INSERT = """
            INSERT INTO password_credential
                (user_id, username, username_normalized, password_hash, created_at, updated_at)
            VALUES (:userId, :username, :usernameNormalized, :passwordHash, :now, :now)
            """;

    private static final String FIND_BY_USERNAME = """
            SELECT pc.user_id, pc.password_hash, pc.failed_login_count, pc.locked_until,
                   u.status, ui.id AS identity_id, ui.verified_at
            FROM password_credential pc
            JOIN app_user u ON u.id = pc.user_id
            LEFT JOIN user_identity ui ON ui.user_id = pc.user_id AND ui.provider = 'EMAIL'
            WHERE pc.username_normalized = :usernameNormalized
            """;

    private static final String RECORD_FAILURE = """
            UPDATE password_credential
            SET failed_login_count = LEAST(failed_login_count + 1, :maxFailedLogins),
                locked_until = CASE WHEN failed_login_count + 1 >= :maxFailedLogins
                                    THEN :lockedUntil ELSE locked_until END,
                updated_at = :now
            WHERE user_id = :userId
            """;

    private static final String RECORD_SUCCESS = """
            UPDATE password_credential
            SET failed_login_count = 0, locked_until = NULL, updated_at = :now
            WHERE user_id = :userId AND (failed_login_count > 0 OR locked_until IS NOT NULL)
            """;

    private final JdbcClient jdbcClient;

    public void insert(UUID userId, String username, String usernameNormalized, String passwordHash, Instant now) {
        jdbcClient.sql(INSERT)
                .param("userId", userId)
                .param("username", username)
                .param("usernameNormalized", usernameNormalized)
                .param("passwordHash", passwordHash)
                .param("now", SqlTimestamps.of(now))
                .update();
    }

    public Optional<PasswordLogin> findByUsername(String usernameNormalized) {
        return jdbcClient.sql(FIND_BY_USERNAME)
                .param("usernameNormalized", usernameNormalized)
                .query((resultSet, rowNumber) -> new PasswordLogin(
                        resultSet.getObject("user_id", UUID.class),
                        resultSet.getString("password_hash"),
                        resultSet.getInt("failed_login_count"),
                        SqlTimestamps.instantOrNull(resultSet, "locked_until"),
                        AccountStatus.valueOf(resultSet.getString("status")),
                        resultSet.getObject("identity_id", UUID.class),
                        SqlTimestamps.instantOrNull(resultSet, "verified_at")))
                .optional();
    }

    public void recordFailure(UUID userId, int maxFailedLogins, Instant lockedUntil, Instant now) {
        jdbcClient.sql(RECORD_FAILURE)
                .param("userId", userId)
                .param("maxFailedLogins", maxFailedLogins)
                .param("lockedUntil", SqlTimestamps.of(lockedUntil))
                .param("now", SqlTimestamps.of(now))
                .update();
    }

    public void recordSuccess(UUID userId, Instant now) {
        jdbcClient.sql(RECORD_SUCCESS)
                .param("userId", userId)
                .param("now", SqlTimestamps.of(now))
                .update();
    }
}
