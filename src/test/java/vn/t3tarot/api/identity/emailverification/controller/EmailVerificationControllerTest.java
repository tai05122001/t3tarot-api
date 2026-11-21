package vn.t3tarot.api.identity.emailverification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import vn.t3tarot.api.identity.IdentityIntegrationTest;

class EmailVerificationControllerTest extends IdentityIntegrationTest {

    private static final String TOKEN_INVALID = "error.verification_token.invalid";

    private Account registerUnverified() throws Exception {
        Account account = newAccount();
        register(account).andExpect(status().isCreated());
        return account;
    }

    private ResultActions verifyWith(String token) throws Exception {
        return mockMvc.perform(postJson("/auth/email/verify", json("token", token)));
    }

    private ResultActions resend(String email) throws Exception {
        return mockMvc.perform(postJsonWithKey("/auth/email/resend", json("email", email)));
    }

    private void makeLastTokenOldEnoughToResend(Account account) {
        execute(
                "UPDATE email_verification SET created_at = now() - interval '2 minutes' WHERE user_id = ?",
                userIdOf(account));
    }

    @Nested
    class WhenVerifying {

        @Test
        void confirmsEmailAndAllowsLogin() throws Exception {
            Account account = registerUnverified();
            String token = mailSender.lastTokenOf(account.email()).orElseThrow();

            verifyWith(token).andExpect(status().isNoContent());

            assertThat(scalar(
                            Boolean.class,
                            "SELECT verified_at IS NOT NULL FROM user_identity WHERE provider_subject = ?",
                            account.email()))
                    .isTrue();
            loginAs(account).andExpect(status().isOk());
        }

        @Test
        void storesOnlyTheHashOfTheToken() throws Exception {
            Account account = registerUnverified();
            String token = mailSender.lastTokenOf(account.email()).orElseThrow();

            byte[] storedHash = scalar(
                    byte[].class, "SELECT token_hash FROM email_verification WHERE user_id = ?", userIdOf(account));

            assertThat(storedHash).isEqualTo(opaqueTokens.hash(token));
        }

        @Test
        void rejectsTokenThatWasAlreadyUsed() throws Exception {
            Account account = registerUnverified();
            String token = mailSender.lastTokenOf(account.email()).orElseThrow();
            verifyWith(token).andExpect(status().isNoContent());

            verifyWith(token)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(TOKEN_INVALID));
        }

        @Test
        void rejectsUnknownToken() throws Exception {
            verifyWith("unknown-token-value")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(TOKEN_INVALID));
        }

        @Test
        void rejectsExpiredToken() throws Exception {
            Account account = registerUnverified();
            String token = mailSender.lastTokenOf(account.email()).orElseThrow();
            execute(
                    "UPDATE email_verification SET expires_at = now() - interval '1 minute' WHERE user_id = ?",
                    userIdOf(account));

            verifyWith(token)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(TOKEN_INVALID));
        }

        @Test
        void reportsMissingToken() throws Exception {
            mockMvc.perform(postJson("/auth/email/verify", "{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("token"));
        }
    }

    @Nested
    class WhenResending {

        @Test
        void sendsNewTokenThatVerifiesTheEmail() throws Exception {
            Account account = registerUnverified();
            makeLastTokenOldEnoughToResend(account);

            resend(account.email()).andExpect(status().isAccepted());

            assertThat(mailSender.sentCountOf(account.email())).isEqualTo(2);
            verifyWith(mailSender.lastTokenOf(account.email()).orElseThrow()).andExpect(status().isNoContent());
        }

        @Test
        void ignoresEmailCase() throws Exception {
            Account account = registerUnverified();
            makeLastTokenOldEnoughToResend(account);

            resend(account.email().toUpperCase()).andExpect(status().isAccepted());

            assertThat(mailSender.sentCountOf(account.email())).isEqualTo(2);
        }

        @Test
        void sendsNothingWithinTheResendInterval() throws Exception {
            Account account = registerUnverified();

            resend(account.email()).andExpect(status().isAccepted());

            assertThat(mailSender.sentCountOf(account.email())).isEqualTo(1);
        }

        @Test
        void answersAcceptedAndSendsNothingForUnknownEmail() throws Exception {
            String email = newAccount().email();

            resend(email).andExpect(status().isAccepted());

            assertThat(mailSender.sentCountOf(email)).isZero();
        }

        @Test
        void sendsNothingWhenEmailIsAlreadyVerified() throws Exception {
            Account account = registerVerifiedAccount();
            makeLastTokenOldEnoughToResend(account);

            resend(account.email()).andExpect(status().isAccepted());

            assertThat(mailSender.sentCountOf(account.email())).isEqualTo(1);
        }

        @Test
        void sendsNothingForLockedAccount() throws Exception {
            Account account = registerUnverified();
            makeLastTokenOldEnoughToResend(account);
            execute("UPDATE app_user SET status = 'LOCKED' WHERE id = ?", userIdOf(account));

            resend(account.email()).andExpect(status().isAccepted());

            assertThat(mailSender.sentCountOf(account.email())).isEqualTo(1);
        }

        @Test
        void staysAcceptedWhenMailProviderFails() throws Exception {
            Account account = registerUnverified();
            makeLastTokenOldEnoughToResend(account);
            mailSender.failWhileSending(true);

            resend(account.email()).andExpect(status().isAccepted());
        }

        @Test
        void requiresIdempotencyKey() throws Exception {
            mockMvc.perform(postJson("/auth/email/resend", json("email", newAccount().email())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.idempotency_key.is_missing"));
        }

        @Test
        void rejectsMalformedEmail() throws Exception {
            mockMvc.perform(postJsonWithKey("/auth/email/resend", json("email", "not-an-email")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("email"));
        }
    }
}
