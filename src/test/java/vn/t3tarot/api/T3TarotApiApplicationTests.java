package vn.t3tarot.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import vn.t3tarot.api.common.security.TestJwtFactory;

@SpringBootTest(
        properties = {
            "t3tarot.security.jwt.issuer=https://issuer.t3tarot.test",
            "t3tarot.security.jwt.audience=t3tarot-api"
        })
@Import(TestcontainersConfiguration.class)
class T3TarotApiApplicationTests {

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        registry.add("t3tarot.security.jwt.public-key", TestJwtFactory::trustedPublicKeyBase64);
        registry.add("t3tarot.identity.private-key", TestJwtFactory::trustedPrivateKeyBase64);
        registry.add("t3tarot.security.jwt.public-key-location", () -> "");
        registry.add("t3tarot.identity.private-key-location", () -> "");
        registry.add("t3tarot.verification-mail.enabled", () -> "false");
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext.getId()).isNotBlank();
    }
}
