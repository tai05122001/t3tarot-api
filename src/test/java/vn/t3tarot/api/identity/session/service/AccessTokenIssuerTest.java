package vn.t3tarot.api.identity.session.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import vn.t3tarot.api.common.constant.AccessTokenClaims;
import vn.t3tarot.api.common.security.Role;
import vn.t3tarot.api.common.security.SecurityProperties;
import vn.t3tarot.api.common.security.TestJwtFactory;
import vn.t3tarot.api.common.security.jwt.JwtDecoderConfig;
import vn.t3tarot.api.common.security.jwt.RsaPublicKeys;
import vn.t3tarot.api.identity.IdentityTestProperties;
import vn.t3tarot.api.identity.session.config.SessionConfig;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;

class AccessTokenIssuerTest {

    private static final UUID USER_A = UUID.fromString("3f2a9c1e-7b4d-4e8a-9c2f-1a2b3c4d5e6f");
    private static final UUID SESSION_OF_USER_A = UUID.fromString("0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b");

    private SecurityProperties securityProperties;
    private AccessTokenIssuer issuer;
    private NimbusJwtDecoder decoder;

    @BeforeEach
    void setUp() {
        IdentityProperties identityProperties = IdentityTestProperties.identity();
        securityProperties = IdentityTestProperties.security();
        issuer = new AccessTokenIssuer(
                new SessionConfig().accessTokenJwtEncoder(identityProperties, securityProperties),
                securityProperties,
                identityProperties);
        RSAPublicKey publicKey = RsaPublicKeys.parse(TestJwtFactory.trustedPublicKeyBase64());
        decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        decoder.setJwtValidator(JwtDecoderConfig.accessTokenValidator(securityProperties.jwt()));
    }

    private String issueNow() {
        return issuer.issue(USER_A, SESSION_OF_USER_A, Role.USER, Clock.systemUTC().instant());
    }

    @Test
    void carriesRoleAsRolesClaim() {
        String adminToken = issuer.issue(USER_A, SESSION_OF_USER_A, Role.ADMIN, Clock.systemUTC().instant());

        assertThat(decoder.decode(adminToken).getClaimAsStringList(AccessTokenClaims.ROLES))
                .containsExactly("ADMIN");
        assertThat(decoder.decode(issueNow()).getClaimAsStringList(AccessTokenClaims.ROLES))
                .containsExactly("USER");
    }

    @Test
    void issuesTokenThatPassesTheApiAccessTokenValidator() {
        Jwt jwt = decoder.decode(issueNow());

        assertThat(jwt.getSubject()).isEqualTo(USER_A.toString());
        assertThat(jwt.getClaimAsString(AccessTokenClaims.SESSION_ID)).isEqualTo(SESSION_OF_USER_A.toString());
        assertThat(jwt.getIssuer()).hasToString(TestJwtFactory.ISSUER);
        assertThat(jwt.getAudience()).containsExactly(TestJwtFactory.AUDIENCE);
    }

    @Test
    void expiresAfterConfiguredLifetime() {
        Instant issuedAt = Clock.systemUTC().instant();
        Jwt jwt = decoder.decode(issuer.issue(USER_A, SESSION_OF_USER_A, Role.USER, issuedAt));

        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(issuer.expiresInSeconds()).isEqualTo(Duration.ofMinutes(15).toSeconds());
    }

    @Test
    void signsWithRs256AndConfiguredKeyId() {
        Jwt jwt = decoder.decode(issueNow());

        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256").containsEntry("kid", IdentityTestProperties.KEY_ID);
    }
}
