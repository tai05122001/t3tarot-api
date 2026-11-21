package vn.t3tarot.api.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.t3tarot.api.common.security.TestJwtFactory;

class RsaPrivateKeysTest {

    @Nested
    class WhenKeyIsValid {

        @Test
        void parsesSingleLineBase64() {
            RSAPrivateKey key = RsaPrivateKeys.parse(TestJwtFactory.trustedPrivateKeyBase64());

            assertThat(key.getModulus().bitLength()).isGreaterThanOrEqualTo(RsaPrivateKeys.MIN_MODULUS_BITS);
        }

        @Test
        void parsesPemWithArmorAndNewlines() {
            String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                    .encodeToString(Base64.getDecoder().decode(TestJwtFactory.trustedPrivateKeyBase64()));
            String pem = "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";

            assertThat(RsaPrivateKeys.parse(pem)).isEqualTo(RsaPrivateKeys.parse(TestJwtFactory.trustedPrivateKeyBase64()));
        }

        @Test
        void parsesPemWithEscapedNewlines() {
            String body = TestJwtFactory.trustedPrivateKeyBase64();
            String escaped = "-----BEGIN PRIVATE KEY-----\\n" + body + "\\n-----END PRIVATE KEY-----";

            assertThat(RsaPrivateKeys.parse(escaped)).isEqualTo(RsaPrivateKeys.parse(body));
        }
    }

    @Nested
    class WhenKeyIsInvalid {

        @Test
        void rejectsKeyShorterThanMinimumModulus() {
            String shortKey = Base64.getEncoder()
                    .encodeToString(TestJwtFactory.generateKeyPair(1024).getPrivate().getEncoded());

            assertThatThrownBy(() -> RsaPrivateKeys.parse(shortKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2048");
        }

        @Test
        void rejectsInputThatIsNotBase64() {
            assertThatThrownBy(() -> RsaPrivateKeys.parse("not base64 !!!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Base64");
        }

        @Test
        void rejectsBytesThatAreNotPkcs8() {
            String garbage = Base64.getEncoder().encodeToString("garbage".getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> RsaPrivateKeys.parse(garbage))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PKCS#8");
        }

        @Test
        void rejectsPublicKeyBytes() {
            assertThatThrownBy(() -> RsaPrivateKeys.parse(TestJwtFactory.trustedPublicKeyBase64()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
