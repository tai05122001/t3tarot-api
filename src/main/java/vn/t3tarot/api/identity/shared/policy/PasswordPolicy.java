package vn.t3tarot.api.identity.shared.policy;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean isHashable(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
