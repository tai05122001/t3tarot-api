package vn.t3tarot.api.identity.sociallogin.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoogleLoginRequestTest {

    @Test
    void doesNotExposeIdTokenInToString() {
        assertThat(new GoogleLoginRequest("secret-id-token").toString()).doesNotContain("secret-id-token");
    }
}
