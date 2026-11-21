package vn.t3tarot.api.identity.emailverification.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationFacade {

    private final EmailVerificationIssuer emailVerificationIssuer;
    private final VerifyEmail verifyEmail;
    private final ResendEmailVerification resendEmailVerification;

    public void issue(UUID userId, UUID identityId, String email, Instant now) {
        emailVerificationIssuer.issue(userId, identityId, email, now);
    }

    public void verify(String token) {
        verifyEmail.execute(token);
    }

    public void resend(String email) {
        resendEmailVerification.execute(email);
    }
}
