package vn.t3tarot.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class RoleTest {

    @Test
    void prefixesAuthorityWithRole() {
        assertThat(Role.USER.authority()).isEqualTo("ROLE_USER");
        assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void parsesExactNames() {
        assertThat(Role.parse("USER")).contains(Role.USER);
        assertThat(Role.parse("ADMIN")).contains(Role.ADMIN);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "admin", "ROLE_ADMIN", " ADMIN", "SUPERUSER"})
    void rejectsAnythingElse(String value) {
        assertThat(Role.parse(value)).isEmpty();
    }
}
