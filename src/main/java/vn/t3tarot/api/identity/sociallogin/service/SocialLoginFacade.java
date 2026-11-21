package vn.t3tarot.api.identity.sociallogin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.t3tarot.api.identity.shared.entity.ClientInfo;
import vn.t3tarot.api.identity.sociallogin.dto.request.GoogleLoginRequest;
import vn.t3tarot.api.identity.sociallogin.dto.response.GoogleLoginResponse;

@Service
@RequiredArgsConstructor
public class SocialLoginFacade {

    private final LoginWithGoogle loginWithGoogle;

    public GoogleLoginResponse loginWithGoogle(GoogleLoginRequest request, ClientInfo clientInfo) {
        return loginWithGoogle.execute(request, clientInfo);
    }
}
