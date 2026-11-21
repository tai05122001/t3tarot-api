package vn.t3tarot.api.identity.emailverification.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("t3tarot.verification-mail.async")
public record VerificationMailAsyncProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("4") @Min(1) @Max(32) int poolSize,
        @DefaultValue("200") @Min(1) int queueCapacity,
        @NotNull @DefaultValue("10s") Duration shutdownAwait) {

    public VerificationMailAsyncProperties {
        if (shutdownAwait != null && shutdownAwait.isNegative()) {
            throw new IllegalArgumentException("t3tarot.verification-mail.async.shutdown-await must not be negative");
        }
    }
}
