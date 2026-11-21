package vn.t3tarot.api.identity.emailverification.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.t3tarot.api.common.time.VietnamClock;
import vn.t3tarot.api.identity.shared.enumeration.AccountStatus;
import vn.t3tarot.api.identity.shared.repository.UserIdentityHotPathRepository;
import vn.t3tarot.api.identity.shared.service.IdentityNormalizer;

@Component
@RequiredArgsConstructor
@Transactional
class ResendEmailVerification {

    private final VietnamClock clock;
    private final UserIdentityHotPathRepository identities;
    private final EmailVerificationIssuer emailVerificationIssuer;

    void execute(String email) {
        Instant now = clock.instant();
        String normalizedEmail = IdentityNormalizer.email(email);
        identities
                .findUnverifiedEmailIdentity(normalizedEmail)
                .filter(identity -> identity.status() == AccountStatus.ACTIVE)
                .ifPresent(identity ->
                        emailVerificationIssuer.issue(identity.userId(), identity.identityId(), normalizedEmail, now));
    }
}
