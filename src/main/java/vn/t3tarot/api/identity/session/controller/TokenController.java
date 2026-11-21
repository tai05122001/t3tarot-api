package vn.t3tarot.api.identity.session.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.t3tarot.api.common.security.AuthenticatedUser;
import vn.t3tarot.api.identity.session.dto.request.RefreshTokenRequest;
import vn.t3tarot.api.identity.session.dto.response.TokenResponse;
import vn.t3tarot.api.identity.session.service.SessionFacade;

@Tag(name = "auth")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class TokenController {

    private final SessionFacade sessionFacade;

    @Operation(summary = "Exchange a refresh token for a new token pair")
    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return sessionFacade.refresh(request.refreshToken());
    }

    @Operation(summary = "Revoke the session of a refresh token")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshTokenRequest request) {
        sessionFacade.logout(request.refreshToken());
    }

    @Operation(summary = "Revoke every session of the caller")
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logoutAll(@AuthenticationPrincipal AuthenticatedUser user) {
        sessionFacade.logoutAll(user.userId());
    }
}
