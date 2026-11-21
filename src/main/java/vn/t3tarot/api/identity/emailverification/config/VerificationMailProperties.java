package vn.t3tarot.api.identity.emailverification.config;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("t3tarot.verification-mail")
public record VerificationMailProperties(
        boolean enabled,
        String from,
        @NotBlank @DefaultValue("T3Tarot") String fromName,
        @NotBlank @DefaultValue("http://localhost:8080") String publicBaseUrl,
        @NotBlank @DefaultValue("http://localhost:5173") String webBaseUrl,
        @NotBlank @DefaultValue("/login") String webLoginPath,
        @NotBlank @DefaultValue("/verify-email") String webResendPath,
        @NotBlank @DefaultValue("/") String webHomePath) {

    public VerificationMailProperties {
        publicBaseUrl = requireHttpBaseUrl(publicBaseUrl, "public-base-url");
        webBaseUrl = requireHttpBaseUrl(webBaseUrl, "web-base-url");
        if (enabled && !StringUtils.hasText(from)) {
            throw new IllegalArgumentException("t3tarot.verification-mail.from is required when mail is enabled");
        }
    }

    public String webLoginUrl() {
        return webBaseUrl + webLoginPath;
    }

    public String webResendUrl() {
        return webBaseUrl + webResendPath;
    }

    public String webHomeUrl() {
        return webBaseUrl + webHomePath;
    }

    private static String requireHttpBaseUrl(String value, String name) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.strip();
        URI uri = URI.create(trimmed);
        boolean isHttp = "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
        if (!isHttp || uri.getHost() == null) {
            throw new IllegalArgumentException("t3tarot.verification-mail." + name + " must be an http(s) URL");
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
