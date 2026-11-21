package vn.t3tarot.api.identity.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("t3tarot.identity")
public record IdentityProperties(
        @NotBlank String privateKey,
        @NotBlank String keyId,
        @NotNull @DefaultValue("15m") Duration accessTokenTtl,
        @NotNull @DefaultValue("30d") Duration refreshTokenTtl,
        @NotNull @DefaultValue("10s") Duration refreshGrace,
        @NotNull @DefaultValue("24h") Duration emailVerificationTtl,
        @NotNull @DefaultValue("60s") Duration emailVerificationResendInterval,
        @DefaultValue("5") @Min(1) int maxFailedLogins,
        @NotNull @DefaultValue("15m") Duration lockoutDuration,
        @DefaultValue("12") @Min(4) @Max(31) int passwordHashStrength,
        @DefaultValue @Valid Google google) {

    @Override
    public String toString() {
        return "IdentityProperties[keyId=" + keyId + "]";
    }

    public record Google(
            @DefaultValue List<String> clientIds,
            @NotBlank @DefaultValue("https://www.googleapis.com/oauth2/v3/certs") String jwkSetUri,
            @NotNull @DefaultValue("2s") Duration connectTimeout,
            @NotNull @DefaultValue("3s") Duration readTimeout) {

        public Google {
            clientIds = List.copyOf(clientIds);
        }
    }
}
