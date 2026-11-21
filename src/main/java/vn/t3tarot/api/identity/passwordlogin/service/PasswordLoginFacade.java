package vn.t3tarot.api.identity.passwordlogin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.t3tarot.api.identity.passwordlogin.dto.request.LoginRequest;
import vn.t3tarot.api.identity.session.dto.response.TokenResponse;
import vn.t3tarot.api.identity.shared.entity.ClientInfo;

@Service
@RequiredArgsConstructor
public class PasswordLoginFacade {

    private final LoginWithPassword loginWithPassword;

    public TokenResponse login(LoginRequest request, ClientInfo clientInfo) {
        return loginWithPassword.execute(request, clientInfo);
    }
}
