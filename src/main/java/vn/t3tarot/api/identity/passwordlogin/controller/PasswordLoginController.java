package vn.t3tarot.api.identity.passwordlogin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.t3tarot.api.identity.passwordlogin.dto.request.LoginRequest;
import vn.t3tarot.api.identity.passwordlogin.service.PasswordLoginFacade;
import vn.t3tarot.api.identity.session.dto.response.TokenResponse;
import vn.t3tarot.api.identity.shared.controller.ClientInfoResolver;

@Tag(name = "auth")
@RestController
@RequestMapping("/auth/login")
@RequiredArgsConstructor
class PasswordLoginController {

    private final PasswordLoginFacade passwordLoginFacade;

    @Operation(summary = "Log in with username and password")
    @PostMapping
    TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return passwordLoginFacade.login(request, ClientInfoResolver.resolve(httpRequest));
    }
}
