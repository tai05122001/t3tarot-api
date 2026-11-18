package vn.t3tarot.api.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AuthenticatedUser user = AuthenticatedUser.parse(jwt.getSubject())
                .orElseThrow(() -> new InvalidBearerTokenException("subject is not a user id"));
        return new AccessTokenAuthentication(jwt, user);
    }
}
