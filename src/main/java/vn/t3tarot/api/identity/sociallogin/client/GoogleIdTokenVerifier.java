package vn.t3tarot.api.identity.sociallogin.client;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import vn.t3tarot.api.common.constant.CommonErrorCode;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;
import vn.t3tarot.api.identity.shared.enumeration.IdentityErrorCode;
import vn.t3tarot.api.identity.sociallogin.entity.GoogleProfile;

@Slf4j
@Component
public class GoogleIdTokenVerifier {

    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final String EMAIL_CLAIM = "email";
    private static final String EMAIL_VERIFIED_CLAIM = "email_verified";
    private static final String NAME_CLAIM = "name";

    private final JwtDecoder decoder;

    @Autowired
    public GoogleIdTokenVerifier(IdentityProperties properties) {
        this(buildDecoder(properties.google()));
    }

    public GoogleIdTokenVerifier(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    public static OAuth2TokenValidator<Jwt> tokenValidator(Set<String> clientIds) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS, issuer -> issuer != null && ISSUERS.contains(issuer.toString())),
                new JwtClaimValidator<List<String>>(
                        JwtClaimNames.AUD,
                        audience -> audience != null && audience.stream().anyMatch(clientIds::contains)));
    }

    public GoogleProfile verify(String idToken) {
        Jwt jwt = decode(idToken);
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString(EMAIL_CLAIM);
        if (isBlank(subject) || isBlank(email) || !isEmailVerified(jwt.getClaim(EMAIL_VERIFIED_CLAIM))) {
            throw new ApiException(IdentityErrorCode.ID_TOKEN_INVALID);
        }
        return new GoogleProfile(subject, email.strip().toLowerCase(Locale.ROOT), jwt.getClaimAsString(NAME_CLAIM));
    }

    private Jwt decode(String idToken) {
        try {
            return decoder.decode(idToken);
        } catch (BadJwtException ex) {
            log.debug("google_id_token_rejected errorType={}", ex.getClass().getSimpleName());
            throw new ApiException(IdentityErrorCode.ID_TOKEN_INVALID, "google id token rejected", ex);
        } catch (JwtException ex) {
            log.warn("google_jwks_unavailable errorType={}", ex.getClass().getSimpleName());
            throw new ApiException(CommonErrorCode.SERVICE_UNAVAILABLE, "google jwks unavailable", ex);
        }
    }

    private static boolean isEmailVerified(Object claim) {
        return Boolean.TRUE.equals(claim) || "true".equalsIgnoreCase(String.valueOf(claim));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static NimbusJwtDecoder buildDecoder(IdentityProperties.Google google) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) google.connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) google.readTimeout().toMillis());
        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withJwkSetUri(google.jwkSetUri())
                .restOperations(new RestTemplate(requestFactory))
                .build();
        nimbusDecoder.setJwtValidator(tokenValidator(Set.copyOf(google.clientIds())));
        return nimbusDecoder;
    }
}
