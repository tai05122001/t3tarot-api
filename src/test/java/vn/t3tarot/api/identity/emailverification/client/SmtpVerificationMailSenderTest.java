package vn.t3tarot.api.identity.emailverification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Message.RecipientType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import vn.t3tarot.api.identity.IdentityTestProperties;
import vn.t3tarot.api.identity.emailverification.config.VerificationMailProperties;

class SmtpVerificationMailSenderTest {

    private static final String TOKEN = "abc-DEF_123";

    private JavaMailSender mailSender;
    private SmtpVerificationMailSender sender;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        sender = new SmtpVerificationMailSender(mailSender, engine, properties("https://api.t3tarot.test/"), IdentityTestProperties.identity());
    }

    private static VerificationMailProperties properties(String publicBaseUrl) {
        return new VerificationMailProperties(
                true,
                "no-reply@t3tarot.test",
                "T3Tarot",
                publicBaseUrl,
                "https://t3tarot.test",
                "/login",
                "/verify-email",
                "/");
    }

    private MimeMessage sentMessage() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void sendsHtmlMailToTheRecipientFromTheConfiguredSender() throws Exception {
        sender.send("alice@example.test", TOKEN);

        MimeMessage message = sentMessage();
        assertThat(message.getSubject()).isEqualTo(SmtpVerificationMailSender.SUBJECT);
        assertThat(((InternetAddress) message.getFrom()[0]).getAddress()).isEqualTo("no-reply@t3tarot.test");
        assertThat(((InternetAddress) message.getFrom()[0]).getPersonal()).isEqualTo("T3Tarot");
        assertThat(((InternetAddress) message.getRecipients(RecipientType.TO)[0]).getAddress())
                .isEqualTo("alice@example.test");
        message.saveChanges();
        assertThat(message.getContentType()).startsWith("text/html");
    }

    @Test
    void putsTheVerifyPageLinkWithTheTokenIntoTheBody() throws Exception {
        sender.send("alice@example.test", TOKEN);

        String html = (String) sentMessage().getContent();
        assertThat(html)
                .contains("https://api.t3tarot.test/auth/email/verify-page?token=" + TOKEN)
                .contains("Xác nhận email của bạn")
                .contains("24 giờ");
    }

    @Test
    void doesNotPutTheRecipientAddressIntoTheBody() throws Exception {
        sender.send("alice@example.test", TOKEN);

        assertThat((String) sentMessage().getContent()).doesNotContain("alice@example.test");
    }

    @Test
    void encodesTheTokenInTheLink() throws Exception {
        sender.send("alice@example.test", "a b&c");

        assertThat((String) sentMessage().getContent()).contains("token=a%20b%26c");
    }

    @Test
    void propagatesProviderFailureSoTheCallerCanLogIt() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender.send("alice@example.test", TOKEN)).isInstanceOf(MailSendException.class);
    }

    @Test
    void failsWhenTheRecipientAddressIsMalformed() {
        assertThatThrownBy(() -> sender.send("not an address <<", TOKEN)).isInstanceOf(IllegalStateException.class);
    }
}
