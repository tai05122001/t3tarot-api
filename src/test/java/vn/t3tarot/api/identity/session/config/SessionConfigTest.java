package vn.t3tarot.api.identity.session.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import vn.t3tarot.api.common.security.TestJwtFactory;
import vn.t3tarot.api.identity.IdentityTestProperties;

class SessionConfigTest {

    private final SessionConfig config = new SessionConfig();

    @Test
    void buildsEncoderWhenPrivateKeyMatchesPublicKey() {
        assertThat(config.accessTokenJwtEncoder(IdentityTestProperties.identity(), IdentityTestProperties.security()))
                .isNotNull();
    }

    @Test
    void buildsEncoderFromPemFiles(@TempDir Path tempDir) throws IOException {
        Path publicKeyFile = Files.writeString(tempDir.resolve("jwt-public.pem"), TestJwtFactory.trustedPublicKeyPem());
        Path privateKeyFile = Files.writeString(tempDir.resolve("jwt-private.pem"), trustedPrivateKeyPem());

        assertThat(config.accessTokenJwtEncoder(
                        IdentityTestProperties.identity(null, new FileSystemResource(privateKeyFile)),
                        IdentityTestProperties.security(null, new FileSystemResource(publicKeyFile))))
                .isNotNull();
    }

    private static String trustedPrivateKeyPem() {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(Base64.getDecoder().decode(TestJwtFactory.trustedPrivateKeyBase64()));
        return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";
    }

    @Test
    void failsFastWhenPrivateKeyDoesNotMatchPublicKey() {
        String otherPublicKey = Base64.getEncoder()
                .encodeToString(TestJwtFactory.generateKeyPair(2048).getPublic().getEncoded());

        assertThatThrownBy(() -> config.accessTokenJwtEncoder(
                        IdentityTestProperties.identity(), IdentityTestProperties.security(otherPublicKey)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match");
    }
}
