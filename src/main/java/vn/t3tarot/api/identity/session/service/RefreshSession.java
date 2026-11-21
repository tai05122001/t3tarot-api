package vn.t3tarot.api.identity.session.service;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.common.time.VietnamClock;
import vn.t3tarot.api.identity.session.dto.response.TokenResponse;
import vn.t3tarot.api.identity.session.entity.RotatedSession;
import vn.t3tarot.api.identity.session.repository.AuthSessionHotPathRepository;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;
import vn.t3tarot.api.identity.shared.enumeration.IdentityErrorCode;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens.OpaqueToken;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens;

@Component
@RequiredArgsConstructor
@Transactional(noRollbackFor = ApiException.class)
class RefreshSession {

    private final VietnamClock clock;
    private final OpaqueTokens opaqueTokens;
    private final AuthSessionHotPathRepository sessions;
    private final SessionIssuer sessionIssuer;
    private final IdentityProperties properties;

    TokenResponse execute(String refreshToken) {
        Instant now = clock.instant();
        byte[] presentedHash = opaqueTokens.hash(refreshToken);
        OpaqueToken freshToken = opaqueTokens.generate();
        Optional<RotatedSession> rotated =
                sessions.rotate(presentedHash, freshToken.getHash(), now, now.minus(properties.refreshGrace()));
        if (rotated.isEmpty()) {
            sessions.revokeByPreviousHash(presentedHash, now);
            throw new ApiException(IdentityErrorCode.REFRESH_TOKEN_INVALID);
        }
        return sessionIssuer.issue(rotated.get().userId(), rotated.get().sessionId(), freshToken, now);
    }
}
