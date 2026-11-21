package vn.t3tarot.api.identity.passwordlogin.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginRequestTest {

    @Test
    void doesNotExposePasswordInToString() {
        LoginRequest request = new LoginRequest("alice", "correct-horse-battery");

        assertThat(request.toString()).contains("alice").doesNotContain("correct-horse-battery");
    }
}
