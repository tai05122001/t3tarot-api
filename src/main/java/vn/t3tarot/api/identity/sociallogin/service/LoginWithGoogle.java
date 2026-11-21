package vn.t3tarot.api.identity.sociallogin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.t3tarot.api.identity.shared.entity.ClientInfo;
import vn.t3tarot.api.identity.sociallogin.client.GoogleIdTokenVerifier;
import vn.t3tarot.api.identity.sociallogin.dto.request.GoogleLoginRequest;
import vn.t3tarot.api.identity.sociallogin.dto.response.GoogleLoginResponse;
import vn.t3tarot.api.identity.sociallogin.entity.GoogleProfile;

@Component
@RequiredArgsConstructor
class LoginWithGoogle {

    private final GoogleIdTokenVerifier verifier;
    private final CompleteGoogleLogin completeGoogleLogin;

    GoogleLoginResponse execute(GoogleLoginRequest request, ClientInfo clientInfo) {
        GoogleProfile profile = verifier.verify(request.idToken());
        return completeGoogleLogin.execute(profile, clientInfo);
    }
}
