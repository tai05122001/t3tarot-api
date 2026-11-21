package vn.t3tarot.api.identity.emailverification.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class VerificationTokenPolicyTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void rejectsBlankToken(String token) {
        assertThat(VerificationTokenPolicy.isWellFormed(token)).isFalse();
    }

    @Test
    void acceptsTokenUpToMaxLength() {
        assertThat(VerificationTokenPolicy.isWellFormed("a".repeat(VerificationTokenPolicy.MAX_LENGTH))).isTrue();
    }

    @Test
    void rejectsTokenLongerThanMaxLength() {
        assertThat(VerificationTokenPolicy.isWellFormed("a".repeat(VerificationTokenPolicy.MAX_LENGTH + 1))).isFalse();
    }

    @Test
    void describesWholeHoursInHours() {
        assertThat(VerificationTokenPolicy.describeLifetime(Duration.ofHours(24))).isEqualTo("24 giờ");
        assertThat(VerificationTokenPolicy.describeLifetime(Duration.ofHours(1))).isEqualTo("1 giờ");
    }

    @Test
    void describesOtherDurationsInMinutes() {
        assertThat(VerificationTokenPolicy.describeLifetime(Duration.ofMinutes(30))).isEqualTo("30 phút");
        assertThat(VerificationTokenPolicy.describeLifetime(Duration.ofMinutes(90))).isEqualTo("90 phút");
    }
}
