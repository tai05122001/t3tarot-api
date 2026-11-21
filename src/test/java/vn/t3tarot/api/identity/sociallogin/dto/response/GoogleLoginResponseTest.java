package vn.t3tarot.api.identity.sociallogin.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import vn.t3tarot.api.identity.session.dto.response.TokenResponse;

class GoogleLoginResponseTest {

    @Test
    void copiesTokensAndNewUserFlag() {
        GoogleLoginResponse response = GoogleLoginResponse.from(TokenResponse.bearer("access-value", "refresh-value", 900), true);

        assertThat(response.accessToken()).isEqualTo("access-value");
        assertThat(response.refreshToken()).isEqualTo("refresh-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.isNewUser()).isTrue();
    }

    @Test
    void doesNotExposeTokensInToString() {
        GoogleLoginResponse response = GoogleLoginResponse.from(TokenResponse.bearer("access-value", "refresh-value", 900), false);

        assertThat(response.toString()).doesNotContain("access-value").doesNotContain("refresh-value");
    }
}
