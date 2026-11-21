package vn.t3tarot.api.identity.emailverification.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.t3tarot.api.common.time.VietnamClock;
import vn.t3tarot.api.identity.emailverification.enumeration.EmailVerificationOutcome;
import vn.t3tarot.api.identity.emailverification.policy.VerificationTokenPolicy;
import vn.t3tarot.api.identity.emailverification.repository.EmailVerificationHotPathRepository;
import vn.t3tarot.api.identity.emailverification.repository.EmailVerificationHotPathRepository.TokenState;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens;

@Component
@RequiredArgsConstructor
@Transactional
class VerifyEmailFromLink {

    private final VietnamClock clock;
    private final OpaqueTokens opaqueTokens;
    private final EmailVerificationHotPathRepository verifications;

    EmailVerificationOutcome execute(String token) {
        if (!VerificationTokenPolicy.isWellFormed(token)) {
            return EmailVerificationOutcome.LINK_INVALID;
        }
        byte[] tokenHash = opaqueTokens.hash(token);
        Instant now = clock.instant();
        if (verifications.consume(tokenHash, now).isPresent()) {
            return EmailVerificationOutcome.VERIFIED;
        }
        return verifications
                .findState(tokenHash)
                .map(state -> classifyRejected(state, now))
                .orElse(EmailVerificationOutcome.LINK_INVALID);
    }

    private static EmailVerificationOutcome classifyRejected(TokenState state, Instant now) {
        if (state.consumedAt() != null) {
            return EmailVerificationOutcome.LINK_USED;
        }
        if (state.identityVerifiedAt() != null) {
            return EmailVerificationOutcome.ALREADY_VERIFIED;
        }
        if (!state.expiresAt().isAfter(now)) {
            return EmailVerificationOutcome.LINK_EXPIRED;
        }
        return EmailVerificationOutcome.LINK_INVALID;
    }
}
