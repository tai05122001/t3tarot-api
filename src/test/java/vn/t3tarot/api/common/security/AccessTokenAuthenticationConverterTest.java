package vn.t3tarot.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class AccessTokenAuthenticationConverterTest {

    private final AccessTokenAuthenticationConverter converter = new AccessTokenAuthenticationConverter();

    @Test
    void buildsAuthenticatedTokenWithUserPrincipalAndNoAuthorities() {
        AbstractAuthenticationToken authentication = converter.convert(jwtWithSubject(TestJwtFactory.USER_A));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedUser(UUID.fromString(TestJwtFactory.USER_A)));
        assertThat(authentication.getAuthorities()).isEmpty();
        assertThat(authentication.getName()).isEqualTo(TestJwtFactory.USER_A);
        assertThat(authentication.getCredentials()).isInstanceOf(Jwt.class);
    }

    @Test
    void throwsInvalidBearerTokenWhenSubjectIsNotUserId() {
        Jwt jwt = jwtWithSubject("admin");

        assertThatThrownBy(() -> converter.convert(jwt)).isInstanceOf(InvalidBearerTokenException.class);
    }

    private static Jwt jwtWithSubject(String subject) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(600))
                .build();
    }
}
