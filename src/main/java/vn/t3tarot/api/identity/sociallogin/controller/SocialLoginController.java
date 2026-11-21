package vn.t3tarot.api.identity.sociallogin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.t3tarot.api.identity.shared.controller.ClientInfoResolver;
import vn.t3tarot.api.identity.sociallogin.dto.request.GoogleLoginRequest;
import vn.t3tarot.api.identity.sociallogin.dto.response.GoogleLoginResponse;
import vn.t3tarot.api.identity.sociallogin.service.SocialLoginFacade;

@Tag(name = "auth")
@RestController
@RequestMapping("/auth/social")
@RequiredArgsConstructor
class SocialLoginController {

    private final SocialLoginFacade socialLoginFacade;

    @Operation(summary = "Log in with a Google ID token")
    @PostMapping("/google")
    GoogleLoginResponse loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request, HttpServletRequest httpRequest) {
        return socialLoginFacade.loginWithGoogle(request, ClientInfoResolver.resolve(httpRequest));
    }
}
