package vn.t3tarot.api.identity.emailverification.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import vn.t3tarot.api.identity.emailverification.client.VerificationMailSender;

class EmailVerificationConfigTest {

    @Test
    void providesMailSenderThatDoesNotFailWithoutProvider() {
        VerificationMailSender sender = new EmailVerificationConfig().verificationMailSender();

        assertThatCode(() -> sender.send("alice@example.com", "token")).doesNotThrowAnyException();
    }
}
