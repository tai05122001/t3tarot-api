package vn.t3tarot.api.identity.shared.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.t3tarot.api.identity.shared.enumeration.AccountStatus;

class PasswordLoginTest {

    private static final Instant NOW = Instant.parse("2026-11-19T00:00:00Z");
    private static final UUID USER_A = UUID.fromString("3f2a9c1e-7b4d-4e8a-9c2f-1a2b3c4d5e6f");

    private static PasswordLogin loginLockedUntil(Instant lockedUntil, Instant emailVerifiedAt) {
        return new PasswordLogin(
                USER_A, "$2a$12$hash", 0, lockedUntil, AccountStatus.ACTIVE, UUID.randomUUID(), emailVerifiedAt);
    }

    @Test
    void isLockedWhenLockedUntilIsAfterNow() {
        assertThat(loginLockedUntil(NOW.plusSeconds(1), null).isLockedAt(NOW)).isTrue();
    }

    @Test
    void isNotLockedWhenLockedUntilEqualsNow() {
        assertThat(loginLockedUntil(NOW, null).isLockedAt(NOW)).isFalse();
    }

    @Test
    void isNotLockedWhenLockedUntilIsNull() {
        assertThat(loginLockedUntil(null, null).isLockedAt(NOW)).isFalse();
    }

    @Test
    void reportsEmailVerificationFromTimestamp() {
        assertThat(loginLockedUntil(null, NOW).isEmailVerified()).isTrue();
        assertThat(loginLockedUntil(null, null).isEmailVerified()).isFalse();
    }

    @Test
    void doesNotExposePasswordHashInToString() {
        assertThat(loginLockedUntil(null, null).toString()).doesNotContain("$2a$").contains(USER_A.toString());
    }
}
