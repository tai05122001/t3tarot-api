package vn.t3tarot.api.identity.shared.service;

import java.util.Locale;

public final class IdentityNormalizer {

    private IdentityNormalizer() {
    }

    public static String username(String username) {
        return username.strip().toLowerCase(Locale.ROOT);
    }

    public static String email(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
