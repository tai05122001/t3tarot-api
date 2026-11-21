package vn.t3tarot.api.identity.emailverification.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.t3tarot.api.identity.emailverification.enumeration.EmailVerificationOutcome;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationFacade {

    private final EmailVerificationIssuer emailVerificationIssuer;
    private final VerifyEmail verifyEmail;
    private final VerifyEmailFromLink verifyEmailFromLink;
    private final ResendEmailVerification resendEmailVerification;

    public void issue(UUID userId, UUID identityId, String email, Instant now) {
        emailVerificationIssuer.issue(userId, identityId, email, now);
        log.info("email.verification.issue userId={}", userId);
    }

    public void verify(String token) {
        verifyEmail.execute(token);
        log.info("email.verification.verify");
    }

    public EmailVerificationOutcome verifyFromLink(String token) {
        EmailVerificationOutcome outcome = verifyEmailFromLink.execute(token);
        log.info("email.verification.verifyFromLink outcome={}", outcome);
        return outcome;
    }

    public void resend(String email) {
        resendEmailVerification.execute(email);
        log.info("email.verification.resend");
    }
}
