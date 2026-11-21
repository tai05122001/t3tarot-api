package vn.t3tarot.api.identity.emailverification.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VerificationMailPropertiesTest {

    private static VerificationMailProperties of(boolean enabled, String from, String publicBaseUrl, String webBaseUrl) {
        return new VerificationMailProperties(
                enabled, from, "T3Tarot", publicBaseUrl, webBaseUrl, "/login", "/verify-email", "/");
    }

    @Test
    void stripsTrailingSlashFromBaseUrls() {
        VerificationMailProperties properties =
                of(false, null, "https://api.t3tarot.test/", " https://t3tarot.test/ ");

        assertThat(properties.publicBaseUrl()).isEqualTo("https://api.t3tarot.test");
        assertThat(properties.webBaseUrl()).isEqualTo("https://t3tarot.test");
    }

    @Test
    void buildsWebUrlsFromBaseUrlAndPaths() {
        VerificationMailProperties properties = of(false, null, "http://localhost:8080", "http://localhost:5173");

        assertThat(properties.webLoginUrl()).isEqualTo("http://localhost:5173/login");
        assertThat(properties.webResendUrl()).isEqualTo("http://localhost:5173/verify-email");
        assertThat(properties.webHomeUrl()).isEqualTo("http://localhost:5173/");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://t3tarot.test", "t3tarot.test", "javascript:alert(1)", "//t3tarot.test"})
    void rejectsBaseUrlThatIsNotHttp(String baseUrl) {
        assertThatThrownBy(() -> of(false, null, baseUrl, "http://localhost:5173"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http(s) URL");
        assertThatThrownBy(() -> of(false, null, "http://localhost:8080", baseUrl))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresSenderAddressWhenEnabled() {
        assertThatThrownBy(() -> of(true, " ", "http://localhost:8080", "http://localhost:5173"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
    }

    @Test
    void allowsMissingSenderAddressWhenDisabled() {
        assertThatCode(() -> of(false, null, "http://localhost:8080", "http://localhost:5173"))
                .doesNotThrowAnyException();
    }
}
