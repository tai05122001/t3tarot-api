package vn.t3tarot.api.identity.session.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenRequestTest {

    @Test
    void doesNotExposeRefreshTokenInToString() {
        assertThat(new RefreshTokenRequest("refresh-value").toString()).doesNotContain("refresh-value");
    }
}
