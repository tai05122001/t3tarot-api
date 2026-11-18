package vn.t3tarot.api.common.security;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public record AuthenticatedUser(UUID userId) implements Serializable {

    private static final Pattern CANONICAL_UUID =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId");
    }

    public static Optional<AuthenticatedUser> parse(String subject) {
        if (subject == null || !CANONICAL_UUID.matcher(subject).matches()) {
            return Optional.empty();
        }
        return Optional.of(new AuthenticatedUser(UUID.fromString(subject)));
    }
}
