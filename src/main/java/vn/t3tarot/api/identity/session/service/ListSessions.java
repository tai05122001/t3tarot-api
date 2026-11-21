package vn.t3tarot.api.identity.session.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.t3tarot.api.common.time.VietnamClock;
import vn.t3tarot.api.identity.session.dto.response.SessionResponse;
import vn.t3tarot.api.identity.session.repository.AuthSessionHotPathRepository;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ListSessions {

    private final VietnamClock clock;
    private final AuthSessionHotPathRepository sessions;

    List<SessionResponse> execute(UUID userId, UUID currentSessionId) {
        return sessions.findActive(userId, currentSessionId, clock.instant()).stream()
                .map(SessionResponse::from)
                .toList();
    }
}
