package vn.t3tarot.api.identity.emailverification.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailVerificationDtoTest {

    @Test
    void doesNotExposeTokenInVerifyRequestToString() {
        assertThat(new VerifyEmailRequest("verification-token").toString()).doesNotContain("verification-token");
    }

    @Test
    void doesNotExposeEmailInResendRequestToString() {
        assertThat(new ResendEmailVerificationRequest("alice@example.test").toString())
                .doesNotContain("alice@example.test");
    }
}
