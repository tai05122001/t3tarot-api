package vn.t3tarot.api.identity.emailverification.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import vn.t3tarot.api.identity.IdentityIntegrationTest;
import vn.t3tarot.api.identity.emailverification.client.SmtpVerificationMailSender;
import vn.t3tarot.api.identity.emailverification.client.VerificationMailSender;

class SmtpMailWiringTest extends IdentityIntegrationTest {

    @DynamicPropertySource
    static void smtpMail(DynamicPropertyRegistry registry) {
        registry.add("t3tarot.verification-mail.enabled", () -> "true");
        registry.add("t3tarot.verification-mail.from", () -> "no-reply@t3tarot.test");
        registry.add("t3tarot.verification-mail.public-base-url", () -> "https://api.t3tarot.test");
    }

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    @Qualifier("smtpVerificationMailSender")
    private VerificationMailSender smtpSender;

    @Test
    void createsTheSmtpSenderWhenMailIsEnabled() {
        assertThat(smtpSender).isInstanceOf(SmtpVerificationMailSender.class);
    }

    @Test
    void rendersTheMailWithSpringBootsTemplateEngine() throws Exception {
        when(javaMailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());

        smtpSender.send("alice@example.test", "token-value");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat((String) captor.getValue().getContent())
                .contains("https://api.t3tarot.test/auth/email/verify-page?token=token-value")
                .contains("24 giờ");
    }
}
