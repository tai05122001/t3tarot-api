package vn.t3tarot.api.identity.shared.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.t3tarot.api.common.security.Role;

@Repository
@RequiredArgsConstructor
public class AppUserHotPathRepository {

    private static final String INSERT = """
            INSERT INTO app_user (id, display_name, created_at, updated_at)
            VALUES (:id, :displayName, :now, :now)
            """;

    private static final String SELECT_ROLE = "SELECT role FROM app_user WHERE id = :id";

    private final JdbcClient jdbcClient;

    public void insert(UUID id, String displayName, Instant now) {
        jdbcClient.sql(INSERT)
                .param("id", id)
                .param("displayName", displayName)
                .param("now", SqlTimestamps.of(now))
                .update();
    }

    public Optional<Role> findRole(UUID id) {
        return jdbcClient.sql(SELECT_ROLE)
                .param("id", id)
                .query(String.class)
                .optional()
                .flatMap(Role::parse);
    }
}
