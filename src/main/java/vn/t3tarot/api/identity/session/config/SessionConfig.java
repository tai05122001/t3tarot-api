package vn.t3tarot.api.identity.session.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import vn.t3tarot.api.common.security.SecurityProperties;
import vn.t3tarot.api.common.security.jwt.RsaPrivateKeys;
import vn.t3tarot.api.common.security.jwt.RsaPublicKeys;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;

@Configuration(proxyBeanMethods = false)
public class SessionConfig {

    @Bean
    public JwtEncoder accessTokenJwtEncoder(IdentityProperties properties, SecurityProperties securityProperties) {
        RSAPublicKey publicKey = RsaPublicKeys.parse(securityProperties.jwt().publicKeyPem());
        RSAPrivateKey privateKey = RsaPrivateKeys.parse(properties.privateKeyPem());
        if (!publicKey.getModulus().equals(privateKey.getModulus())) {
            throw new IllegalStateException("JWT private key does not match JWT public key");
        }
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(properties.keyId())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }
}
