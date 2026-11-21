package vn.t3tarot.api.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.t3tarot.api.common.security.TestJwtFactory;

class RsaPublicKeysTest {

    @Nested
    class WhenKeyIsValid {

        @Test
        void parsesSingleLineBase64() {
            RSAPublicKey key = RsaPublicKeys.parse(TestJwtFactory.trustedPublicKeyBase64());

            assertThat(key.getModulus().bitLength()).isGreaterThanOrEqualTo(RsaPublicKeys.MIN_MODULUS_BITS);
        }

        @Test
        void parsesPemWithArmorAndNewlines() {
            RSAPublicKey fromPem = RsaPublicKeys.parse(TestJwtFactory.trustedPublicKeyPem());

            assertThat(fromPem).isEqualTo(RsaPublicKeys.parse(TestJwtFactory.trustedPublicKeyBase64()));
        }

        @Test
        void parsesPemWithEscapedNewlinesFromEnvFile() {
            String escaped = TestJwtFactory.trustedPublicKeyPem().trim().replace("\n", "\\n");

            assertThat(RsaPublicKeys.parse(escaped)).isEqualTo(RsaPublicKeys.parse(TestJwtFactory.trustedPublicKeyBase64()));
        }
    }

    @Nested
    class WhenKeyIsRejected {

        @Test
        void rejectsNonBase64WithoutEchoingInput() {
            assertThatThrownBy(() -> RsaPublicKeys.parse("secret-looking-value!!!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageNotContaining("secret-looking-value");
        }

        @Test
        void rejectsBase64ThatIsNotX509() {
            String garbage = Base64.getEncoder().encodeToString("not a key".getBytes());

            assertThatThrownBy(() -> RsaPublicKeys.parse(garbage))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("X.509");
        }

        @Test
        void rejectsKeyShorterThanMinimum() {
            KeyPair weak = TestJwtFactory.generateKeyPair(1024);
            String encoded = Base64.getEncoder().encodeToString(weak.getPublic().getEncoded());

            assertThatThrownBy(() -> RsaPublicKeys.parse(encoded))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2048");
        }

        @Test
        void rejectsEllipticCurveKey() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(256);
            String encoded = Base64.getEncoder().encodeToString(generator.generateKeyPair().getPublic().getEncoded());

            assertThatThrownBy(() -> RsaPublicKeys.parse(encoded)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
