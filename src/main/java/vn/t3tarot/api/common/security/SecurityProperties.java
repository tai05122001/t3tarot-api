package vn.t3tarot.api.common.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;
import vn.t3tarot.api.common.security.jwt.PemKeySource;

@Validated
@ConfigurationProperties(prefix = "t3tarot.security")
public record SecurityProperties(
        @DefaultValue @Valid Cors cors,
        @NotNull @Valid Jwt jwt,
        boolean swaggerPublic,
        @DefaultValue List<String> publicPaths) {

    public SecurityProperties {
        publicPaths = List.copyOf(publicPaths);
    }

    public record Cors(
            @DefaultValue List<String> allowedOrigins,
            @DefaultValue({"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}) List<String> allowedMethods,
            @DefaultValue({"Authorization", "Content-Type", "Idempotency-Key", "X-Client-Platform", "X-App-Version"})
                    List<String> allowedHeaders,
            @DefaultValue({"X-Request-Id", "Retry-After"}) List<String> exposedHeaders,
            @DefaultValue("3600") @Positive long maxAgeSeconds) {

        private static final Pattern ORIGIN = Pattern.compile("^https?://[A-Za-z0-9.-]+(:\\d{1,5})?$");

        public Cors {
            allowedOrigins = List.copyOf(allowedOrigins);
            allowedMethods = List.copyOf(allowedMethods);
            allowedHeaders = List.copyOf(allowedHeaders);
            exposedHeaders = List.copyOf(exposedHeaders);
            for (String origin : allowedOrigins) {
                if (!ORIGIN.matcher(origin).matches()) {
                    throw new IllegalArgumentException(
                            "t3tarot.security.cors.allowed-origins entries must be exact http(s) origins without wildcard or path");
                }
            }
        }
    }

    public record Jwt(
            @NotBlank String issuer, @NotBlank String audience, String publicKey, Resource publicKeyLocation) {

        public String publicKeyPem() {
            return PemKeySource.read(publicKey, publicKeyLocation, "JWT public key");
        }

        @Override
        public String toString() {
            return "Jwt[issuer=" + issuer + ", audience=" + audience + "]";
        }
    }
}
