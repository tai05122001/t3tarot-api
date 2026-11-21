package vn.t3tarot.api.identity.session.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.t3tarot.api.common.security.jwt.AccessTokenAuthentication;
import vn.t3tarot.api.identity.session.dto.response.SessionResponse;
import vn.t3tarot.api.identity.session.service.SessionFacade;

@Tag(name = "auth")
@RestController
@RequestMapping("/auth/sessions")
@RequiredArgsConstructor
class SessionController {

    private final SessionFacade sessionFacade;

    @Operation(summary = "List the active sessions of the caller")
    @GetMapping
    List<SessionResponse> list(AccessTokenAuthentication authentication) {
        UUID currentSessionId = authentication.sessionId().orElse(null);
        return sessionFacade.list(authentication.user().userId(), currentSessionId);
    }

    @Operation(summary = "Revoke one session of the caller")
    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(AccessTokenAuthentication authentication, @PathVariable UUID sessionId) {
        sessionFacade.revoke(authentication.user().userId(), sessionId);
    }
}
