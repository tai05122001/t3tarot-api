package vn.t3tarot.api.identity.sociallogin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.t3tarot.api.identity.shared.entity.ClientInfo;
import vn.t3tarot.api.identity.sociallogin.dto.request.GoogleLoginRequest;
import vn.t3tarot.api.identity.sociallogin.dto.response.GoogleLoginResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLoginFacade {

    private final LoginWithGoogle loginWithGoogle;

    public GoogleLoginResponse loginWithGoogle(GoogleLoginRequest request, ClientInfo clientInfo) {
        GoogleLoginResponse response = loginWithGoogle.execute(request, clientInfo);
        log.info("login.google isNewUser={}", response.isNewUser());
        return response;
    }
}
