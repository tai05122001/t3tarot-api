package vn.t3tarot.api.identity.register.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegisterRequestTest {

    @Test
    void doesNotExposePasswordOrEmailInToString() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.test", "correct-horse-battery");

        assertThat(request.toString()).contains("alice").doesNotContain("correct-horse-battery").doesNotContain("@");
    }
}
