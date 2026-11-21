package vn.t3tarot.api.identity.emailverification.client;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import vn.t3tarot.api.identity.emailverification.config.VerificationMailProperties;
import vn.t3tarot.api.identity.emailverification.policy.VerificationTokenPolicy;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;

@RequiredArgsConstructor
public class SmtpVerificationMailSender implements VerificationMailSender {

    public static final String SUBJECT = "Link xác nhận email T3Tarot";
    public static final String VERIFY_PAGE_PATH = "/auth/email/verify-page";

    private static final String TEMPLATE = "mail/email-verification";
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final VerificationMailProperties properties;
    private final IdentityProperties identityProperties;

    @Override
    public void send(String email, String verificationToken) {
        String html = templateEngine.process(TEMPLATE, context(verificationToken));
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(properties.from(), properties.fromName(), StandardCharsets.UTF_8.name()));
            helper.setTo(email);
            helper.setSubject(SUBJECT);
            helper.setText(html, true);
        } catch (MessagingException | UnsupportedEncodingException ex) {
            throw new IllegalStateException("verification mail could not be built", ex);
        }
        mailSender.send(message);
    }

    private Context context(String verificationToken) {
        Context context = new Context(VIETNAMESE);
        context.setVariable("verifyUrl", verifyUrl(verificationToken));
        context.setVariable("lifetime", VerificationTokenPolicy.describeLifetime(identityProperties.emailVerificationTtl()));
        return context;
    }

    private String verifyUrl(String verificationToken) {
        return UriComponentsBuilder.fromUriString(properties.publicBaseUrl())
                .path(VERIFY_PAGE_PATH)
                .queryParam("token", verificationToken)
                .build()
                .encode()
                .toUriString();
    }
}
