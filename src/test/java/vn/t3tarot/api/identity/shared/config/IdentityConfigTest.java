package vn.t3tarot.api.identity.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.t3tarot.api.identity.IdentityTestProperties;

class IdentityConfigTest {

    @Test
    void hashesPasswordsWithConfiguredBcryptStrength() {
        PasswordEncoder encoder = new IdentityConfig().passwordEncoder(IdentityTestProperties.identity());

        String hash = encoder.encode("a-long-enough-password");

        assertThat(hash).startsWith("$2a$04$");
        assertThat(encoder.matches("a-long-enough-password", hash)).isTrue();
    }
}
