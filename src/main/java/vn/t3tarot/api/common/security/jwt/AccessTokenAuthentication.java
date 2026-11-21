package vn.t3tarot.api.common.security.jwt;

import java.io.Serial;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.t3tarot.api.common.constant.AccessTokenClaims;
import vn.t3tarot.api.common.security.AuthenticatedUser;

public class AccessTokenAuthentication extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Jwt jwt;
    private final AuthenticatedUser principal;

    public AccessTokenAuthentication(Jwt jwt, AuthenticatedUser principal) {
        super(List.of());
        this.jwt = jwt;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.userId().toString();
    }

    public AuthenticatedUser user() {
        return principal;
    }

    public Optional<UUID> sessionId() {
        String claim = jwt.getClaimAsString(AccessTokenClaims.SESSION_ID);
        if (claim == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(claim));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
