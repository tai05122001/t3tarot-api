package vn.t3tarot.api.identity.session.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.t3tarot.api.common.time.VietnamClock;
import vn.t3tarot.api.identity.session.repository.AuthSessionHotPathRepository;

@Component
@RequiredArgsConstructor
@Transactional
class LogoutAll {

    private final VietnamClock clock;
    private final AuthSessionHotPathRepository sessions;

    void execute(UUID userId) {
        sessions.revokeAllOfUser(userId, clock.instant());
    }
}
