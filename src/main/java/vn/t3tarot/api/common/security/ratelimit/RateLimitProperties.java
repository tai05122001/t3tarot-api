package vn.t3tarot.api.common.security.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("t3tarot.security.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        @NotNull @Valid Limit publicLimit,
        @NotNull @Valid Limit authenticatedLimit) {

    public record Limit(@Min(1) int maxRequests, @Min(1) int windowSeconds) {}
}
