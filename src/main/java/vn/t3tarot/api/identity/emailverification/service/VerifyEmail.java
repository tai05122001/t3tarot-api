package vn.t3tarot.api.identity.emailverification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.common.time.VietnamClock;
import vn.t3tarot.api.identity.emailverification.repository.EmailVerificationHotPathRepository;
import vn.t3tarot.api.identity.shared.enumeration.IdentityErrorCode;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens;

@Component
@RequiredArgsConstructor
@Transactional
class VerifyEmail {

    private final VietnamClock clock;
    private final OpaqueTokens opaqueTokens;
    private final EmailVerificationHotPathRepository verifications;

    void execute(String token) {
        verifications
                .consume(opaqueTokens.hash(token), clock.instant())
                .orElseThrow(() -> new ApiException(IdentityErrorCode.VERIFICATION_TOKEN_INVALID));
    }
}
