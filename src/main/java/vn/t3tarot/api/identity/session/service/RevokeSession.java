package vn.t3tarot.api.identity.session.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.t3tarot.api.common.constant.CommonErrorCode;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.common.time.VietnamClock;
import vn.t3tarot.api.identity.session.repository.AuthSessionHotPathRepository;

@Component
@RequiredArgsConstructor
@Transactional
class RevokeSession {

    private final VietnamClock clock;
    private final AuthSessionHotPathRepository sessions;

    void execute(UUID userId, UUID sessionId) {
        boolean isRevoked = sessions.revokeOfUser(userId, sessionId, clock.instant());
        if (!isRevoked && !sessions.existsOfUser(userId, sessionId)) {
            throw new ApiException(CommonErrorCode.NOT_FOUND);
        }
    }
}
