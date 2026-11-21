package vn.t3tarot.api.identity;

import java.time.Duration;
import java.util.List;
import org.springframework.core.io.Resource;
import vn.t3tarot.api.common.security.SecurityProperties;
import vn.t3tarot.api.common.security.TestJwtFactory;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;

public final class IdentityTestProperties {

    public static final String KEY_ID = "test-key";

    private IdentityTestProperties() {
    }

    public static IdentityProperties identity(String privateKey) {
        return identity(privateKey, null);
    }

    public static IdentityProperties identity(String privateKey, Resource privateKeyLocation) {
        return new IdentityProperties(
                privateKey,
                privateKeyLocation,
                KEY_ID,
                Duration.ofMinutes(15),
                Duration.ofDays(30),
                Duration.ofSeconds(10),
                Duration.ofHours(24),
                Duration.ofSeconds(60),
                5,
                Duration.ofMinutes(15),
                4,
                new IdentityProperties.Google(
                        List.of(GoogleTokenFactory.CLIENT_ID),
                        "https://www.googleapis.com/oauth2/v3/certs",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(3)));
    }

    public static IdentityProperties identity() {
        return identity(TestJwtFactory.trustedPrivateKeyBase64());
    }

    public static SecurityProperties security(String publicKey) {
        return security(publicKey, null);
    }

    public static SecurityProperties security(String publicKey, Resource publicKeyLocation) {
        return new SecurityProperties(
                new SecurityProperties.Cors(List.of(), List.of(), List.of(), List.of(), 3600),
                new SecurityProperties.Jwt(TestJwtFactory.ISSUER, TestJwtFactory.AUDIENCE, publicKey, publicKeyLocation),
                false,
                List.of());
    }

    public static SecurityProperties security() {
        return security(TestJwtFactory.trustedPublicKeyBase64());
    }
}
