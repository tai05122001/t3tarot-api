package vn.t3tarot.api.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.t3tarot.api.common.constant.AccessTokenClaims;
import vn.t3tarot.api.common.security.AuthenticatedUser;

class AccessTokenAuthenticationTest {

    private static final UUID USER_A = UUID.fromString("3f2a9c1e-7b4d-4e8a-9c2f-1a2b3c4d5e6f");
    private static final UUID SESSION_OF_USER_A = UUID.fromString("0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b");

    private static Jwt jwtWithClaim(Object sessionClaim) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(USER_A.toString())
                .issuedAt(Instant.parse("2026-11-19T00:00:00Z"))
                .expiresAt(Instant.parse("2026-11-19T00:15:00Z"));
        if (sessionClaim != null) {
            builder.claim(AccessTokenClaims.SESSION_ID, sessionClaim);
        }
        return builder.build();
    }

    private static AccessTokenAuthentication authenticationWithClaim(Object sessionClaim) {
        return new AccessTokenAuthentication(
                jwtWithClaim(sessionClaim), new AuthenticatedUser(USER_A), List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void returnsSessionIdWhenClaimIsUuid() {
        assertThat(authenticationWithClaim(SESSION_OF_USER_A.toString()).sessionId()).contains(SESSION_OF_USER_A);
    }

    @Test
    void returnsEmptySessionIdWhenClaimIsMissing() {
        assertThat(authenticationWithClaim(null).sessionId()).isEmpty();
    }

    @Test
    void returnsEmptySessionIdWhenClaimIsNotUuid() {
        assertThat(authenticationWithClaim("not-a-uuid").sessionId()).isEmpty();
    }

    @Test
    void exposesAuthenticatedUserAndName() {
        AccessTokenAuthentication authentication = authenticationWithClaim(null);

        assertThat(authentication.user()).isEqualTo(new AuthenticatedUser(USER_A));
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthenticatedUser(USER_A));
        assertThat(authentication.getName()).isEqualTo(USER_A.toString());
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getCredentials()).isInstanceOf(Jwt.class);
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }
}
