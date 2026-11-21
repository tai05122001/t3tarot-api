package vn.t3tarot.api.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import vn.t3tarot.api.common.security.SecurityProperties;
import vn.t3tarot.api.common.security.TestJwtFactory;
import vn.t3tarot.api.common.security.TestJwtFactory.Claims;

class JwtDecoderConfigTest {

    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Cors(List.of(), List.of(), List.of(), List.of(), 3600),
                new SecurityProperties.Jwt(
                        TestJwtFactory.ISSUER, TestJwtFactory.AUDIENCE, TestJwtFactory.trustedPublicKeyBase64(), null),
                false,
                List.of());
        decoder = new JwtDecoderConfig().jwtDecoder(properties);
    }

    @Test
    void acceptsTokenSignedByTrustedKeyWithExpectedClaims() {
        Jwt jwt = decoder.decode(TestJwtFactory.trustedToken(TestJwtFactory.validClaims()));

        assertThat(jwt.getSubject()).isEqualTo(TestJwtFactory.USER_A);
        assertThat(jwt.getIssuer()).hasToString(TestJwtFactory.ISSUER);
        assertThat(jwt.getAudience()).containsExactly(TestJwtFactory.AUDIENCE);
    }

    @Nested
    class WhenTokenIsRejected {

        @Test
        void rejectsExpiredToken() {
            Claims expired = TestJwtFactory.validClaims().withLifetime(Duration.ofMinutes(-10));

            assertThatThrownBy(() -> decoder.decode(TestJwtFactory.trustedToken(expired)))
                    .isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsTokenWithoutExpiration() {
            Claims noExpiration = TestJwtFactory.validClaims().withoutExpiration();

            assertThatThrownBy(() -> decoder.decode(TestJwtFactory.trustedToken(noExpiration)))
                    .isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsWrongIssuer() {
            Claims wrongIssuer = TestJwtFactory.validClaims().withIssuer("https://evil.example");

            assertThatThrownBy(() -> decoder.decode(TestJwtFactory.trustedToken(wrongIssuer)))
                    .isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsWrongAudience() {
            Claims wrongAudience = TestJwtFactory.validClaims().withAudience(List.of("another-api"));

            assertThatThrownBy(() -> decoder.decode(TestJwtFactory.trustedToken(wrongAudience)))
                    .isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsMissingAudience() {
            Claims noAudience = TestJwtFactory.validClaims().withAudience(null);

            assertThatThrownBy(() -> decoder.decode(TestJwtFactory.trustedToken(noAudience)))
                    .isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsMissingSubject() {
            Claims noSubject = TestJwtFactory.validClaims().withSubject(null);

            assertThatThrownBy(() -> decoder.decode(TestJwtFactory.trustedToken(noSubject)))
                    .isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsTokenSignedByUntrustedKey() {
            assertThatThrownBy(() -> decoder.decode(TestJwtFactory.untrustedToken(TestJwtFactory.validClaims())))
                    .isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsUnsignedAlgNoneToken() {
            assertThatThrownBy(() -> decoder.decode(TestJwtFactory.unsignedToken(TestJwtFactory.validClaims())))
                    .isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsHmacTokenSignedWithPublicKeyAsSecret() {
            assertThatThrownBy(
                            () -> decoder.decode(TestJwtFactory.hmacTokenSignedWithPublicKey(TestJwtFactory.validClaims())))
                    .isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsMalformedToken() {
            assertThatThrownBy(() -> decoder.decode("not.a.jwt")).isInstanceOf(BadJwtException.class);
        }
    }
}
