package vn.t3tarot.api.identity.emailverification.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import vn.t3tarot.api.identity.IdentityTestProperties;
import vn.t3tarot.api.identity.emailverification.client.LoggingVerificationMailSender;
import vn.t3tarot.api.identity.emailverification.client.SmtpVerificationMailSender;
import vn.t3tarot.api.identity.emailverification.client.VerificationMailSender;

class EmailVerificationConfigTest {

    @Test
    void providesMailSenderThatDoesNotFailWithoutProvider() {
        VerificationMailSender sender = new EmailVerificationConfig().verificationMailSender();

        assertThat(sender).isInstanceOf(LoggingVerificationMailSender.class);
        assertThatCode(() -> sender.send("alice@example.com", "token")).doesNotThrowAnyException();
    }

    @Test
    void providesSmtpMailSenderWhenAskedFor() {
        VerificationMailProperties properties = new VerificationMailProperties(
                true, "no-reply@t3tarot.test", "T3Tarot", "http://localhost:8080", "http://localhost:5173", "/login",
                "/verify-email", "/");

        VerificationMailSender sender = new EmailVerificationConfig()
                .smtpVerificationMailSender(
                        mock(JavaMailSender.class),
                        mock(TemplateEngine.class),
                        properties,
                        IdentityTestProperties.identity());

        assertThat(sender).isInstanceOf(SmtpVerificationMailSender.class);
    }
}
