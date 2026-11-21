package vn.t3tarot.api.identity.shared.entity;

import java.time.Instant;
import java.util.UUID;
import vn.t3tarot.api.identity.shared.enumeration.AccountStatus;

public record PasswordLogin(
        UUID userId,
        String passwordHash,
        int failedLoginCount,
        Instant lockedUntil,
        AccountStatus status,
        UUID identityId,
        Instant emailVerifiedAt) {

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    @Override
    public String toString() {
        return "PasswordLogin[userId=" + userId + "]";
    }
}
