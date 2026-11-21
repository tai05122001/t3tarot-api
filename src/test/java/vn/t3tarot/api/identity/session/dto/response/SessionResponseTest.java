package vn.t3tarot.api.identity.session.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.t3tarot.api.identity.session.entity.ActiveSession;

class SessionResponseTest {

    @Test
    void mapsActiveSession() {
        UUID sessionId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-19T00:00:00Z");
        Instant expiresAt = Instant.parse("2026-12-19T00:00:00Z");

        SessionResponse response =
                SessionResponse.from(new ActiveSession(sessionId, createdAt, expiresAt, "T3Tarot/1.0", true));

        assertThat(response).isEqualTo(new SessionResponse(sessionId, createdAt, expiresAt, "T3Tarot/1.0", true));
    }
}
