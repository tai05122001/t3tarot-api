package vn.t3tarot.api.identity.session.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenResponseTest {

    @Test
    void buildsBearerResponse() {
        TokenResponse response = TokenResponse.bearer("access-value", "refresh-value", 900);

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
    }

    @Test
    void doesNotExposeTokensInToString() {
        TokenResponse response = TokenResponse.bearer("access-value", "refresh-value", 900);

        assertThat(response.toString()).doesNotContain("access-value").doesNotContain("refresh-value");
    }
}
