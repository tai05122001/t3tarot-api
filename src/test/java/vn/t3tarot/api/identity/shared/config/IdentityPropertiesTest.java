package vn.t3tarot.api.identity.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import vn.t3tarot.api.identity.IdentityTestProperties;

class IdentityPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static IdentityProperties withPrivateKeyAndStrength(String privateKey, int strength) {
        IdentityProperties base = IdentityTestProperties.identity(privateKey);
        return new IdentityProperties(
                base.privateKey(),
                base.keyId(),
                base.accessTokenTtl(),
                base.refreshTokenTtl(),
                base.refreshGrace(),
                base.emailVerificationTtl(),
                base.emailVerificationResendInterval(),
                base.maxFailedLogins(),
                base.lockoutDuration(),
                strength,
                base.google());
    }

    private Set<String> violatedProperties(IdentityProperties properties) {
        return validator.validate(properties).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void acceptsCompleteConfiguration() {
        assertThat(validator.validate(IdentityTestProperties.identity())).isEmpty();
    }

    @Test
    void rejectsBlankPrivateKey() {
        assertThat(violatedProperties(withPrivateKeyAndStrength(" ", 12))).containsExactly("privateKey");
    }

    @Test
    void rejectsPasswordHashStrengthBelowBcryptMinimum() {
        assertThat(violatedProperties(withPrivateKeyAndStrength("key", 3))).containsExactly("passwordHashStrength");
    }

    @Test
    void doesNotExposePrivateKeyInToString() {
        assertThat(withPrivateKeyAndStrength("super-secret-key", 12).toString()).doesNotContain("super-secret-key");
    }

    @Test
    void copiesGoogleClientIds() {
        List<String> clientIds = new ArrayList<>(List.of("client-a"));
        IdentityProperties.Google google = new IdentityProperties.Google(
                clientIds, "https://example.test/jwks", Duration.ofSeconds(1), Duration.ofSeconds(1));

        clientIds.add("client-b");

        assertThat(google.clientIds()).containsExactly("client-a");
    }
}
