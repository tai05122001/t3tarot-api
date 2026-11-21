package vn.t3tarot.api.identity.shared.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentityNormalizerTest {

    @Test
    void lowercasesAndStripsUsername() {
        assertThat(IdentityNormalizer.username("  Alice.Smith_01 ")).isEqualTo("alice.smith_01");
    }

    @Test
    void lowercasesAndStripsEmail() {
        assertThat(IdentityNormalizer.email(" Alice@Example.COM ")).isEqualTo("alice@example.com");
    }
}
