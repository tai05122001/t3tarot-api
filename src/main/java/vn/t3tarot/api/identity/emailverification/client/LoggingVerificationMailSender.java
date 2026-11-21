package vn.t3tarot.api.identity.emailverification.client;

import lombok.extern.slf4j.Slf4j;
import vn.t3tarot.api.identity.emailverification.client.VerificationMailSender;

@Slf4j
public class LoggingVerificationMailSender implements VerificationMailSender {

    @Override
    public void send(String email, String verificationToken) {
        log.warn("verification_mail_not_delivered reason=no_mail_provider_configured");
    }
}
