package vn.t3tarot.api.identity.register.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.t3tarot.api.identity.register.dto.request.RegisterRequest;

@Service
@RequiredArgsConstructor
public class RegisterFacade {

    private final RegisterAccount registerAccount;

    public UUID register(RegisterRequest request) {
        return registerAccount.execute(request);
    }
}
