package vn.t3tarot.api.identity.shared.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens.OpaqueToken;

class OpaqueTokensTest {

    private final OpaqueTokens opaqueTokens = new OpaqueTokens(new SecureRandom());

    @Test
    void generatesUrlSafeTokenOfThirtyTwoRandomBytes() {
        OpaqueToken token = opaqueTokens.generate();

        assertThat(token.getValue()).matches("[A-Za-z0-9_-]{43}");
        assertThat(Base64.getUrlDecoder().decode(token.getValue())).hasSize(32);
    }

    @Test
    void storesSha256HashOfTheValue() throws NoSuchAlgorithmException {
        OpaqueToken token = opaqueTokens.generate();

        byte[] expected = MessageDigest.getInstance("SHA-256").digest(token.getValue().getBytes(StandardCharsets.UTF_8));
        assertThat(token.getHash()).isEqualTo(expected);
    }

    @Test
    void hashesDeterministically() {
        assertThat(opaqueTokens.hash("same-value")).isEqualTo(opaqueTokens.hash("same-value"));
        assertThat(opaqueTokens.hash("same-value")).isNotEqualTo(opaqueTokens.hash("other-value"));
    }

    @Test
    void generatesDistinctTokens() {
        assertThat(opaqueTokens.generate().getValue()).isNotEqualTo(opaqueTokens.generate().getValue());
    }

    @Test
    void failsWhenDigestAlgorithmIsUnavailable() {
        OpaqueTokens broken = new OpaqueTokens(new SecureRandom(), "NO-SUCH-ALGORITHM");

        assertThatThrownBy(() -> broken.hash("value"))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(NoSuchAlgorithmException.class);
    }

    @Test
    void doesNotExposeValueInToString() {
        OpaqueToken token = opaqueTokens.generate();

        assertThat(token.toString()).doesNotContain(token.getValue());
    }
}
