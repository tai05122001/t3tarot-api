package vn.t3tarot.api.identity.passwordlogin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.t3tarot.api.identity.passwordlogin.dto.request.LoginRequest;
import vn.t3tarot.api.identity.session.dto.response.TokenResponse;
import vn.t3tarot.api.identity.shared.entity.ClientInfo;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordLoginFacade {

    private final LoginWithPassword loginWithPassword;

    public TokenResponse login(LoginRequest request, ClientInfo clientInfo) {
        TokenResponse response = loginWithPassword.execute(request, clientInfo);
        log.info("login.password");
        return response;
    }
}
