package vn.t3tarot.api.identity.emailverification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import vn.t3tarot.api.identity.emailverification.client.LoggingVerificationMailSender;
import vn.t3tarot.api.identity.emailverification.client.SmtpVerificationMailSender;
import vn.t3tarot.api.identity.emailverification.client.VerificationMailSender;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VerificationMailProperties.class)
public class EmailVerificationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "t3tarot.verification-mail", name = "enabled", havingValue = "true")
    public VerificationMailSender smtpVerificationMailSender(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            VerificationMailProperties properties,
            IdentityProperties identityProperties) {
        return new SmtpVerificationMailSender(mailSender, templateEngine, properties, identityProperties);
    }

    @Bean
    @ConditionalOnMissingBean(VerificationMailSender.class)
    public VerificationMailSender verificationMailSender() {
        return new LoggingVerificationMailSender();
    }
}
