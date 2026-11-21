package vn.t3tarot.api.common.security;

import java.util.Arrays;
import java.util.Optional;

public enum Role {
    USER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }

    public static Optional<Role> parse(String value) {
        return Arrays.stream(values()).filter(role -> role.name().equals(value)).findFirst();
    }
}
