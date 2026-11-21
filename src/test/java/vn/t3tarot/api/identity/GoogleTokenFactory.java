package vn.t3tarot.api.identity;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import vn.t3tarot.api.common.security.TestJwtFactory;
import vn.t3tarot.api.identity.sociallogin.client.GoogleIdTokenVerifier;

public final class GoogleTokenFactory {

    public static final String CLIENT_ID = "test-google-client-id";
    public static final String ISSUER = "https://accounts.google.com";
    public static final String DISPLAY_NAME = "Test Google User";

    private static final Duration ISSUED_BEFORE_EXPIRY = Duration.ofMinutes(1);
    private static final Duration VALID_LIFETIME = Duration.ofHours(1);
    private static final KeyPair GOOGLE_KEY_PAIR = TestJwtFactory.generateKeyPair(2048);
    private static final KeyPair STRANGER_KEY_PAIR = TestJwtFactory.generateKeyPair(2048);

    private GoogleTokenFactory() {
    }

    public static JwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) GOOGLE_KEY_PAIR.getPublic()).build();
        decoder.setJwtValidator(GoogleIdTokenVerifier.tokenValidator(Set.of(CLIENT_ID)));
        return decoder;
    }

    public static String validToken(String subject, String email) {
        return token(ISSUER, subject, email, Boolean.TRUE, CLIENT_ID, VALID_LIFETIME, GOOGLE_KEY_PAIR);
    }

    public static String token(
            String subject, String email, Object emailVerified, String audience, Duration lifetime) {
        return token(ISSUER, subject, email, emailVerified, audience, lifetime, GOOGLE_KEY_PAIR);
    }

    public static String tokenWithIssuer(String issuer, String subject, String email) {
        return token(issuer, subject, email, Boolean.TRUE, CLIENT_ID, VALID_LIFETIME, GOOGLE_KEY_PAIR);
    }

    public static String strangerToken(String subject, String email) {
        return token(ISSUER, subject, email, Boolean.TRUE, CLIENT_ID, VALID_LIFETIME, STRANGER_KEY_PAIR);
    }

    private static String token(
            String issuer,
            String subject,
            String email,
            Object emailVerified,
            String audience,
            Duration lifetime,
            KeyPair signingKeyPair) {
        Instant now = Clock.systemUTC().instant();
        Instant expiresAt = now.plus(lifetime);
        Instant issuedAt = expiresAt.isBefore(now) ? expiresAt.minus(ISSUED_BEFORE_EXPIRY) : now;
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("name", DISPLAY_NAME);
        if (subject != null) {
            builder.subject(subject);
        }
        if (email != null) {
            builder.claim("email", email);
        }
        if (emailVerified != null) {
            builder.claim("email_verified", emailVerified);
        }
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) signingKeyPair.getPublic())
                .privateKey((RSAPrivateKey) signingKeyPair.getPrivate())
                .build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256).build(), builder.build()))
                .getTokenValue();
    }
}
