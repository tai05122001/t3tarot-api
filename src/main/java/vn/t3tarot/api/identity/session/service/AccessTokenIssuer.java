package vn.t3tarot.api.identity.session.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import vn.t3tarot.api.common.constant.AccessTokenClaims;
import vn.t3tarot.api.common.security.SecurityProperties;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;

@Component
@RequiredArgsConstructor
class AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties securityProperties;
    private final IdentityProperties identityProperties;

    String issue(UUID userId, UUID sessionId, Instant now) {
        SecurityProperties.Jwt jwt = securityProperties.jwt();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwt.issuer())
                .audience(List.of(jwt.audience()))
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(identityProperties.accessTokenTtl()))
                .claim(AccessTokenClaims.SESSION_ID, sessionId.toString())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(identityProperties.keyId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    long expiresInSeconds() {
        return identityProperties.accessTokenTtl().toSeconds();
    }
}
