package vn.t3tarot.api.identity.shared.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.t3tarot.api.identity.shared.entity.LinkedIdentity;
import vn.t3tarot.api.identity.shared.enumeration.AccountStatus;
import vn.t3tarot.api.identity.shared.enumeration.IdentityProvider;

@Repository
@RequiredArgsConstructor
public class UserIdentityHotPathRepository {

    private static final String EXISTS_EMAIL = """
            SELECT EXISTS (SELECT 1 FROM user_identity WHERE email_normalized = :email)
            """;

    private static final String EXISTS_EMAIL_ON_OTHER_PROVIDER = """
            SELECT EXISTS (
                SELECT 1 FROM user_identity WHERE email_normalized = :email AND provider <> :provider)
            """;

    private static final String INSERT_EMAIL_IDENTITY = """
            INSERT INTO user_identity (id, user_id, provider, provider_subject, email_normalized, created_at)
            VALUES (:id, :userId, 'EMAIL', :email, :email, :now)
            ON CONFLICT (provider, provider_subject) DO NOTHING
            RETURNING id
            """;

    private static final String INSERT_GOOGLE_IDENTITY = """
            INSERT INTO user_identity (id, user_id, provider, provider_subject, email_normalized, verified_at, created_at)
            VALUES (:id, :userId, 'GOOGLE', :subject, :email, :now, :now)
            """;

    private static final String FIND_BY_PROVIDER_SUBJECT = """
            SELECT ui.user_id, ui.id AS identity_id, u.status
            FROM user_identity ui
            JOIN app_user u ON u.id = ui.user_id
            WHERE ui.provider = :provider AND ui.provider_subject = :subject
            """;

    private static final String FIND_UNVERIFIED_EMAIL_IDENTITY = """
            SELECT ui.user_id, ui.id AS identity_id, u.status
            FROM user_identity ui
            JOIN app_user u ON u.id = ui.user_id
            WHERE ui.provider = 'EMAIL' AND ui.provider_subject = :email AND ui.verified_at IS NULL
            """;

    private static final String RECORD_LOGIN = """
            UPDATE user_identity SET last_login_at = :now WHERE id = :identityId
            """;

    private final JdbcClient jdbcClient;

    public boolean existsEmail(String emailNormalized) {
        return Boolean.TRUE.equals(jdbcClient.sql(EXISTS_EMAIL)
                .param("email", emailNormalized)
                .query(Boolean.class)
                .single());
    }

    public boolean existsEmailOnOtherProvider(String emailNormalized, IdentityProvider provider) {
        return Boolean.TRUE.equals(jdbcClient.sql(EXISTS_EMAIL_ON_OTHER_PROVIDER)
                .param("email", emailNormalized)
                .param("provider", provider.name())
                .query(Boolean.class)
                .single());
    }

    public Optional<UUID> insertEmailIdentity(UUID id, UUID userId, String emailNormalized, Instant now) {
        return jdbcClient.sql(INSERT_EMAIL_IDENTITY)
                .param("id", id)
                .param("userId", userId)
                .param("email", emailNormalized)
                .param("now", SqlTimestamps.of(now))
                .query(UUID.class)
                .optional();
    }

    public void insertGoogleIdentity(UUID id, UUID userId, String subject, String emailNormalized, Instant now) {
        jdbcClient.sql(INSERT_GOOGLE_IDENTITY)
                .param("id", id)
                .param("userId", userId)
                .param("subject", subject)
                .param("email", emailNormalized)
                .param("now", SqlTimestamps.of(now))
                .update();
    }

    public Optional<LinkedIdentity> findByProviderSubject(IdentityProvider provider, String subject) {
        return jdbcClient.sql(FIND_BY_PROVIDER_SUBJECT)
                .param("provider", provider.name())
                .param("subject", subject)
                .query((resultSet, rowNumber) -> new LinkedIdentity(
                        resultSet.getObject("user_id", UUID.class),
                        resultSet.getObject("identity_id", UUID.class),
                        AccountStatus.valueOf(resultSet.getString("status"))))
                .optional();
    }

    public Optional<LinkedIdentity> findUnverifiedEmailIdentity(String emailNormalized) {
        return jdbcClient.sql(FIND_UNVERIFIED_EMAIL_IDENTITY)
                .param("email", emailNormalized)
                .query((resultSet, rowNumber) -> new LinkedIdentity(
                        resultSet.getObject("user_id", UUID.class),
                        resultSet.getObject("identity_id", UUID.class),
                        AccountStatus.valueOf(resultSet.getString("status"))))
                .optional();
    }

    public void recordLogin(UUID identityId, Instant now) {
        jdbcClient.sql(RECORD_LOGIN)
                .param("identityId", identityId)
                .param("now", SqlTimestamps.of(now))
                .update();
    }
}
