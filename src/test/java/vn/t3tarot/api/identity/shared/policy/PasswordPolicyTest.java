package vn.t3tarot.api.identity.shared.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsPasswordOfExactlyMaximumBytes() {
        assertThat(PasswordPolicy.isHashable("a".repeat(PasswordPolicy.MAX_BYTES))).isTrue();
    }

    @Test
    void rejectsPasswordOneByteOverMaximum() {
        assertThat(PasswordPolicy.isHashable("a".repeat(PasswordPolicy.MAX_BYTES + 1))).isFalse();
    }

    @Test
    void countsBytesNotCharacters() {
        assertThat(PasswordPolicy.isHashable("ệ".repeat(24))).isTrue();
        assertThat(PasswordPolicy.isHashable("ệ".repeat(25))).isFalse();
    }

    @Test
    void rejectsNullPassword() {
        assertThat(PasswordPolicy.isHashable(null)).isFalse();
    }
}
