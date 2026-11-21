package vn.t3tarot.api.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import vn.t3tarot.api.common.constant.AccessTokenClaims;

public final class TestJwtFactory {

    public static final String ISSUER = "https://issuer.t3tarot.test";
    public static final String AUDIENCE = "t3tarot-api";
    public static final String USER_A = "3f2a9c1e-7b4d-4e8a-9c2f-1a2b3c4d5e6f";

    private static final Duration ISSUED_BEFORE_EXPIRY = Duration.ofMinutes(1);
    private static final KeyPair TRUSTED_KEY_PAIR = generateKeyPair(2048);
    private static final KeyPair UNTRUSTED_KEY_PAIR = generateKeyPair(2048);

    private TestJwtFactory() {
    }

    public static KeyPair generateKeyPair(int bits) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(bits);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String trustedPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(TRUSTED_KEY_PAIR.getPublic().getEncoded());
    }

    public static String trustedPrivateKeyBase64() {
        return Base64.getEncoder().encodeToString(TRUSTED_KEY_PAIR.getPrivate().getEncoded());
    }

    public static String trustedPublicKeyPem() {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(TRUSTED_KEY_PAIR.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + body + "\n-----END PUBLIC KEY-----\n";
    }

    public static Claims validClaims() {
        return new Claims(USER_A, ISSUER, List.of(AUDIENCE), Duration.ofMinutes(10), true);
    }

    public static String trustedToken(Claims claims) {
        return sign(TRUSTED_KEY_PAIR, claims, null);
    }

    public static String trustedTokenWithRoles(Claims claims, List<String> roles) {
        return sign(TRUSTED_KEY_PAIR, claims, roles);
    }

    public static String untrustedToken(Claims claims) {
        return sign(UNTRUSTED_KEY_PAIR, claims, null);
    }

    public static String unsignedToken(Claims claims) {
        String header = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"sub\":\"" + claims.subject() + "\",\"iss\":\"" + claims.issuer()
                + "\",\"aud\":[\"" + AUDIENCE + "\"],\"exp\":" + Instant.now().plusSeconds(600).getEpochSecond() + "}");
        return header + "." + payload + ".";
    }

    public static String hmacTokenSignedWithPublicKey(Claims claims) {
        try {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(claims.subject())
                    .issuer(claims.issuer())
                    .audience(claims.audience())
                    .expirationTime(Date.from(Instant.now().plus(claims.lifetime())))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            jwt.sign(new MACSigner(TRUSTED_KEY_PAIR.getPublic().getEncoded()));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String sign(KeyPair keyPair, Claims claims, List<String> roles) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        Instant now = Instant.now();
        Instant expiresAt = now.plus(claims.lifetime());
        Instant issuedAt = expiresAt.isBefore(now) ? expiresAt.minus(ISSUED_BEFORE_EXPIRY) : now;
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder().issuedAt(issuedAt);
        if (claims.subject() != null) {
            builder.subject(claims.subject());
        }
        if (claims.issuer() != null) {
            builder.issuer(claims.issuer());
        }
        if (claims.audience() != null) {
            builder.audience(claims.audience());
        }
        if (claims.withExpiration()) {
            builder.expiresAt(expiresAt);
        }
        if (roles != null) {
            builder.claim(AccessTokenClaims.ROLES, roles);
        }
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), builder.build()))
                .getTokenValue();
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public record Claims(String subject, String issuer, List<String> audience, Duration lifetime, boolean withExpiration) {

        public Claims withSubject(String newSubject) {
            return new Claims(newSubject, issuer, audience, lifetime, withExpiration);
        }

        public Claims withIssuer(String newIssuer) {
            return new Claims(subject, newIssuer, audience, lifetime, withExpiration);
        }

        public Claims withAudience(List<String> newAudience) {
            return new Claims(subject, issuer, newAudience, lifetime, withExpiration);
        }

        public Claims withLifetime(Duration newLifetime) {
            return new Claims(subject, issuer, audience, newLifetime, withExpiration);
        }

        public Claims withoutExpiration() {
            return new Claims(subject, issuer, audience, lifetime, false);
        }
    }
}
