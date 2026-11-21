package vn.t3tarot.api.identity.session.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.t3tarot.api.identity.session.entity.ActiveSession;

public record SessionResponse(UUID sessionId, Instant createdAt, Instant expiresAt, String userAgent, boolean isCurrent) {

    public static SessionResponse from(ActiveSession session) {
        return new SessionResponse(
                session.sessionId(), session.createdAt(), session.expiresAt(), session.userAgent(), session.isCurrent());
    }
}
