package vn.t3tarot.api.identity.sociallogin.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import vn.t3tarot.api.common.constant.CommonErrorCode;
import vn.t3tarot.api.common.constant.ErrorCode;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.identity.GoogleTokenFactory;
import vn.t3tarot.api.identity.IdentityTestProperties;
import vn.t3tarot.api.identity.shared.enumeration.IdentityErrorCode;
import vn.t3tarot.api.identity.sociallogin.entity.GoogleProfile;

class GoogleIdTokenVerifierTest {

    private static final String SUBJECT = "109876543210987654321";
    private static final String EMAIL = "Alice@Example.com";

    private GoogleIdTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new GoogleIdTokenVerifier(GoogleTokenFactory.decoder());
    }

    private static void assertRejectedWith(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }

    @Nested
    class WhenTokenIsValid {

        @Test
        void returnsProfileWithLowercasedEmail() {
            GoogleProfile profile = verifier.verify(GoogleTokenFactory.validToken(SUBJECT, EMAIL));

            assertThat(profile.subject()).isEqualTo(SUBJECT);
            assertThat(profile.email()).isEqualTo("alice@example.com");
            assertThat(profile.displayName()).isEqualTo(GoogleTokenFactory.DISPLAY_NAME);
        }

        @Test
        void acceptsEmailVerifiedSentAsString() {
            String token = GoogleTokenFactory.token(
                    SUBJECT, EMAIL, "true", GoogleTokenFactory.CLIENT_ID, Duration.ofMinutes(5));

            assertThat(verifier.verify(token).subject()).isEqualTo(SUBJECT);
        }

        @Test
        void acceptsIssuerWithoutScheme() {
            String token = GoogleTokenFactory.tokenWithIssuer("accounts.google.com", SUBJECT, EMAIL);

            assertThat(verifier.verify(token).subject()).isEqualTo(SUBJECT);
        }
    }

    @Nested
    class WhenTokenIsRejected {

        @Test
        void rejectsTokenForAnotherClientId() {
            String token = GoogleTokenFactory.token(SUBJECT, EMAIL, Boolean.TRUE, "other-client", Duration.ofMinutes(5));

            assertRejectedWith(() -> verifier.verify(token), IdentityErrorCode.ID_TOKEN_INVALID);
        }

        @Test
        void rejectsExpiredToken() {
            String token = GoogleTokenFactory.token(
                    SUBJECT, EMAIL, Boolean.TRUE, GoogleTokenFactory.CLIENT_ID, Duration.ofMinutes(-10));

            assertRejectedWith(() -> verifier.verify(token), IdentityErrorCode.ID_TOKEN_INVALID);
        }

        @Test
        void rejectsTokenFromAnotherIssuer() {
            String token = GoogleTokenFactory.tokenWithIssuer("https://evil.example", SUBJECT, EMAIL);

            assertRejectedWith(() -> verifier.verify(token), IdentityErrorCode.ID_TOKEN_INVALID);
        }

        @Test
        void rejectsTokenSignedByAnotherKey() {
            String token = GoogleTokenFactory.strangerToken(SUBJECT, EMAIL);

            assertRejectedWith(() -> verifier.verify(token), IdentityErrorCode.ID_TOKEN_INVALID);
        }

        @Test
        void rejectsMalformedToken() {
            assertRejectedWith(() -> verifier.verify("not.a.jwt"), IdentityErrorCode.ID_TOKEN_INVALID);
        }

        @Test
        void rejectsUnverifiedEmail() {
            String token = GoogleTokenFactory.token(
                    SUBJECT, EMAIL, Boolean.FALSE, GoogleTokenFactory.CLIENT_ID, Duration.ofMinutes(5));

            assertRejectedWith(() -> verifier.verify(token), IdentityErrorCode.ID_TOKEN_INVALID);
        }

        @Test
        void rejectsMissingEmailVerifiedClaim() {
            String token =
                    GoogleTokenFactory.token(SUBJECT, EMAIL, null, GoogleTokenFactory.CLIENT_ID, Duration.ofMinutes(5));

            assertRejectedWith(() -> verifier.verify(token), IdentityErrorCode.ID_TOKEN_INVALID);
        }

        @Test
        void rejectsMissingEmail() {
            String token = GoogleTokenFactory.validToken(SUBJECT, null);

            assertRejectedWith(() -> verifier.verify(token), IdentityErrorCode.ID_TOKEN_INVALID);
        }

        @Test
        void rejectsMissingSubject() {
            String token = GoogleTokenFactory.validToken(null, EMAIL);

            assertRejectedWith(() -> verifier.verify(token), IdentityErrorCode.ID_TOKEN_INVALID);
        }
    }

    @Nested
    class WhenGoogleKeysAreUnavailable {

        @Test
        void reportsServiceUnavailable() {
            GoogleIdTokenVerifier unavailable = new GoogleIdTokenVerifier(token -> {
                throw new JwtException("jwks unreachable");
            });

            assertRejectedWith(() -> unavailable.verify("any"), CommonErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Nested
    class WhenBuiltFromProperties {

        @Test
        void rejectsMalformedTokenWithoutCallingGoogle() {
            GoogleIdTokenVerifier fromProperties = new GoogleIdTokenVerifier(IdentityTestProperties.identity());

            assertRejectedWith(() -> fromProperties.verify("not.a.jwt"), IdentityErrorCode.ID_TOKEN_INVALID);
        }
    }
}
