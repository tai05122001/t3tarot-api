package vn.t3tarot.api.common.security.jwt;

import java.util.List;
import java.util.Optional;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import vn.t3tarot.api.common.constant.AccessTokenClaims;
import vn.t3tarot.api.common.security.AuthenticatedUser;
import vn.t3tarot.api.common.security.Role;

@Component
public class AccessTokenAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AuthenticatedUser user = AuthenticatedUser.parse(jwt.getSubject())
                .orElseThrow(() -> new InvalidBearerTokenException("subject is not a user id"));
        return new AccessTokenAuthentication(jwt, user, authoritiesOf(jwt));
    }

    private static List<GrantedAuthority> authoritiesOf(Jwt jwt) {
        List<String> claim = Optional.ofNullable(jwt.getClaimAsStringList(AccessTokenClaims.ROLES)).orElse(List.of());
        List<GrantedAuthority> authorities = claim.stream()
                .map(Role::parse)
                .flatMap(Optional::stream)
                .distinct()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
        return authorities.isEmpty() ? List.of(new SimpleGrantedAuthority(Role.USER.authority())) : authorities;
    }
}
