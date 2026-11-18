package vn.t3tarot.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SecurityPropertiesTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsExactHttpsAndLocalhostOrigins() {
        assertThatCode(() -> corsWithOrigins("https://t3tarot.vn", "http://localhost:5173", "https://app.t3tarot.vn:8443"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "https://*.t3tarot.vn", "t3tarot.vn", "https://t3tarot.vn/", "https://t3tarot.vn/app", "ftp://t3tarot.vn", ""})
    void rejectsWildcardOrMalformedOrigins(String origin) {
        assertThatThrownBy(() -> corsWithOrigins(origin)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void copiesListsDefensively() {
        List<String> origins = new java.util.ArrayList<>(List.of("https://t3tarot.vn"));
        SecurityProperties.Cors cors = corsWithOrigins(origins.toArray(String[]::new));

        origins.add("https://evil.example");

        assertThat(cors.allowedOrigins()).containsExactly("https://t3tarot.vn");
        assertThatThrownBy(() -> cors.allowedOrigins().add("https://x.example"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void reportsBlankJwtSettings() {
        SecurityProperties.Jwt jwt = new SecurityProperties.Jwt("", " ", "");

        assertThat(VALIDATOR.validate(jwt))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("issuer", "audience", "publicKey");
    }

    @Test
    void rejectsNonPositiveCorsMaxAge() {
        SecurityProperties.Cors cors = new SecurityProperties.Cors(List.of(), List.of(), List.of(), List.of(), 0);

        assertThat(VALIDATOR.validate(cors)).hasSize(1);
    }

    @Test
    void jwtToStringDoesNotExposePublicKey() {
        SecurityProperties.Jwt jwt = new SecurityProperties.Jwt("issuer", "audience", "KEY-MATERIAL");

        assertThat(jwt.toString()).doesNotContain("KEY-MATERIAL");
    }

    private static SecurityProperties.Cors corsWithOrigins(String... origins) {
        return new SecurityProperties.Cors(List.of(origins), List.of(), List.of(), List.of(), 3600);
    }
}
