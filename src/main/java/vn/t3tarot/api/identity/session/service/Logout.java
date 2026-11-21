package vn.t3tarot.api.identity.session.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.t3tarot.api.common.time.VietnamClock;
import vn.t3tarot.api.identity.session.repository.AuthSessionHotPathRepository;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens;

@Component
@RequiredArgsConstructor
@Transactional
class Logout {

    private final VietnamClock clock;
    private final OpaqueTokens opaqueTokens;
    private final AuthSessionHotPathRepository sessions;

    void execute(String refreshToken) {
        sessions.revokeByHash(opaqueTokens.hash(refreshToken), clock.instant());
    }
}
