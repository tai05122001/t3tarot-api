package vn.t3tarot.api.common.security;

import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
public class JwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties) {
        SecurityProperties.Jwt jwtProperties = properties.jwt();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(RsaPublicKeys.parse(jwtProperties.publicKey()))
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(accessTokenValidator(jwtProperties));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> accessTokenValidator(SecurityProperties.Jwt jwtProperties) {
        List<OAuth2TokenValidator<Jwt>> validators = List.of(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(jwtProperties.issuer()),
                new JwtClaimValidator<Object>(JwtClaimNames.EXP, Objects::nonNull),
                new JwtClaimValidator<Object>(JwtClaimNames.SUB, Objects::nonNull),
                new JwtClaimValidator<List<String>>(
                        JwtClaimNames.AUD, audience -> audience != null && audience.contains(jwtProperties.audience())));
        return new DelegatingOAuth2TokenValidator<>(validators);
    }
}
