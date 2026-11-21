package vn.t3tarot.api.identity.register.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.t3tarot.api.identity.register.dto.request.RegisterRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterFacade {

    private final RegisterAccount registerAccount;

    public UUID register(RegisterRequest request) {
        UUID userId = registerAccount.execute(request);
        log.info("register userId={}", userId);
        return userId;
    }
}
