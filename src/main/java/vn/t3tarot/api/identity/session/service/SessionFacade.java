package vn.t3tarot.api.identity.session.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.t3tarot.api.identity.session.dto.response.SessionResponse;
import vn.t3tarot.api.identity.session.dto.response.TokenResponse;
import vn.t3tarot.api.identity.shared.entity.ClientInfo;

@Service
@RequiredArgsConstructor
public class SessionFacade {

    private final SessionIssuer sessionIssuer;
    private final RefreshSession refreshSession;
    private final Logout logout;
    private final LogoutAll logoutAll;
    private final ListSessions listSessions;
    private final RevokeSession revokeSession;

    public TokenResponse start(UUID userId, UUID identityId, ClientInfo clientInfo, Instant now) {
        return sessionIssuer.start(userId, identityId, clientInfo, now);
    }

    public TokenResponse refresh(String refreshToken) {
        return refreshSession.execute(refreshToken);
    }

    public void logout(String refreshToken) {
        logout.execute(refreshToken);
    }

    public void logoutAll(UUID userId) {
        logoutAll.execute(userId);
    }

    public List<SessionResponse> list(UUID userId, UUID currentSessionId) {
        return listSessions.execute(userId, currentSessionId);
    }

    public void revoke(UUID userId, UUID sessionId) {
        revokeSession.execute(userId, sessionId);
    }
}
