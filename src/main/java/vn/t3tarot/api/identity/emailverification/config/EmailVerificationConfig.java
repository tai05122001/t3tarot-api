package vn.t3tarot.api.identity.emailverification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.TemplateEngine;
import vn.t3tarot.api.common.web.MdcTaskDecorator;
import vn.t3tarot.api.identity.emailverification.client.LoggingVerificationMailSender;
import vn.t3tarot.api.identity.emailverification.client.SmtpVerificationMailSender;
import vn.t3tarot.api.identity.emailverification.client.VerificationMailSender;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({VerificationMailProperties.class, VerificationMailAsyncProperties.class})
public class EmailVerificationConfig {

    static final String THREAD_NAME_PREFIX = "verification-mail-";

    @Bean
    public VerificationMailDispatcher verificationMailDispatcher(VerificationMailAsyncProperties properties) {
        if (!properties.enabled()) {
            return VerificationMailDispatcher.inline();
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.poolSize());
        executor.setMaxPoolSize(properties.poolSize());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setDaemon(true);
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationMillis(properties.shutdownAwait().toMillis());
        executor.initialize();
        return new VerificationMailDispatcher(executor, executor::shutdown);
    }

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
