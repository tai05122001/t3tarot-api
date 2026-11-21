package vn.t3tarot.api.identity.emailverification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.t3tarot.api.identity.emailverification.client.LoggingVerificationMailSender;
import vn.t3tarot.api.identity.emailverification.client.VerificationMailSender;

@Configuration(proxyBeanMethods = false)
public class EmailVerificationConfig {

    @Bean
    @ConditionalOnMissingBean(VerificationMailSender.class)
    public VerificationMailSender verificationMailSender() {
        return new LoggingVerificationMailSender();
    }
}
