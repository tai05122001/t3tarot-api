package vn.t3tarot.api.identity.session.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;
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
    void failsFastWhenPrivateKeyDoesNotMatchPublicKey() {
        String otherPublicKey = Base64.getEncoder()
                .encodeToString(TestJwtFactory.generateKeyPair(2048).getPublic().getEncoded());

        assertThatThrownBy(() -> config.accessTokenJwtEncoder(
                        IdentityTestProperties.identity(), IdentityTestProperties.security(otherPublicKey)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match");
    }
}
