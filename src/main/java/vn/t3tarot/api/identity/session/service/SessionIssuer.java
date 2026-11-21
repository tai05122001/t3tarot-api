package vn.t3tarot.api.identity.session.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.t3tarot.api.common.random.UuidV7Generator;
import vn.t3tarot.api.identity.session.dto.response.TokenResponse;
import vn.t3tarot.api.identity.session.repository.AuthSessionHotPathRepository;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;
import vn.t3tarot.api.identity.shared.entity.ClientInfo;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens.OpaqueToken;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens;

@Component
@RequiredArgsConstructor
class SessionIssuer {

    private static final int MAX_USER_AGENT_LENGTH = 255;

    private final UuidV7Generator uuidGenerator;
    private final OpaqueTokens opaqueTokens;
    private final AccessTokenIssuer accessTokenIssuer;
    private final AuthSessionHotPathRepository sessions;
    private final IdentityProperties properties;

    TokenResponse start(UUID userId, UUID identityId, ClientInfo clientInfo, Instant now) {
        OpaqueToken refreshToken = opaqueTokens.generate();
        UUID sessionId = uuidGenerator.next();
        sessions.insert(
                sessionId,
                userId,
                identityId,
                refreshToken.getHash(),
                truncate(clientInfo.userAgent()),
                clientInfo.ipAddress(),
                now,
                now.plus(properties.refreshTokenTtl()));
        return issue(userId, sessionId, refreshToken, now);
    }

    TokenResponse issue(UUID userId, UUID sessionId, OpaqueToken refreshToken, Instant now) {
        return TokenResponse.bearer(
                accessTokenIssuer.issue(userId, sessionId, now),
                refreshToken.getValue(),
                accessTokenIssuer.expiresInSeconds());
    }

    private static String truncate(String userAgent) {
        if (userAgent == null || userAgent.length() <= MAX_USER_AGENT_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, MAX_USER_AGENT_LENGTH);
    }
}
