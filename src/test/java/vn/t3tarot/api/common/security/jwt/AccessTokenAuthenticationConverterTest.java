package vn.t3tarot.api.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import vn.t3tarot.api.common.constant.AccessTokenClaims;
import vn.t3tarot.api.common.security.AuthenticatedUser;
import vn.t3tarot.api.common.security.TestJwtFactory;

class AccessTokenAuthenticationConverterTest {

    private final AccessTokenAuthenticationConverter converter = new AccessTokenAuthenticationConverter();

    @Test
    void buildsAuthenticatedTokenWithUserPrincipalAndDefaultUserAuthority() {
        AbstractAuthenticationToken authentication = converter.convert(jwtWithSubject(TestJwtFactory.USER_A));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedUser(UUID.fromString(TestJwtFactory.USER_A)));
        assertThat(authorities(authentication)).containsExactly("ROLE_USER");
        assertThat(authentication.getName()).isEqualTo(TestJwtFactory.USER_A);
        assertThat(authentication.getCredentials()).isInstanceOf(Jwt.class);
    }

    @Test
    void mapsRolesClaimToAuthorities() {
        AbstractAuthenticationToken authentication =
                converter.convert(jwtWithRoles(List.of("ADMIN", "USER", "ADMIN")));

        assertThat(authorities(authentication)).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void doesNotGrantUnknownOrMisspelledRoles() {
        AbstractAuthenticationToken authentication =
                converter.convert(jwtWithRoles(List.of("SUPERUSER", "admin", "ROLE_ADMIN")));

        assertThat(authorities(authentication)).containsExactly("ROLE_USER");
    }

    @Test
    void fallsBackToUserWhenRolesClaimIsEmpty() {
        AbstractAuthenticationToken authentication = converter.convert(jwtWithRoles(List.of()));

        assertThat(authorities(authentication)).containsExactly("ROLE_USER");
    }

    @Test
    void throwsInvalidBearerTokenWhenSubjectIsNotUserId() {
        Jwt jwt = jwtWithSubject("admin");

        assertThatThrownBy(() -> converter.convert(jwt)).isInstanceOf(InvalidBearerTokenException.class);
    }

    private static List<String> authorities(AbstractAuthenticationToken authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    private static Jwt jwtWithRoles(List<String> roles) {
        return jwtBuilder(TestJwtFactory.USER_A).claim(AccessTokenClaims.ROLES, roles).build();
    }

    private static Jwt jwtWithSubject(String subject) {
        return jwtBuilder(subject).build();
    }

    private static Jwt.Builder jwtBuilder(String subject) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(600));
    }
}
