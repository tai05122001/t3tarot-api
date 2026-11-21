package vn.t3tarot.api.identity.passwordlogin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.t3tarot.api.identity.IdentityIntegrationTest;

class PasswordLoginControllerTest extends IdentityIntegrationTest {

    private static final String INVALID_CREDENTIALS = "error.auth.invalid_credentials";

    private void wrongPasswordAttempt(Account account) throws Exception {
        mockMvc.perform(postJson("/auth/login", json("username", account.username(), "password", "wrong-password-value")))
                .andExpect(status().isUnauthorized());
    }

    @Nested
    class WhenCredentialsAreCorrect {

        @Test
        void returnsBearerTokenPair() throws Exception {
            Account account = registerVerifiedAccount();

            loginAs(account)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(900))
                    .andExpect(jsonPath("$.accessToken", matchesPattern("[\\w-]+\\.[\\w-]+\\.[\\w-]+")))
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        void storesOnlyTheHashOfTheRefreshToken() throws Exception {
            Account account = registerVerifiedAccount();

            Tokens tokens = loginTokens(account);

            byte[] storedHash = scalar(
                    byte[].class, "SELECT refresh_token_hash FROM auth_session WHERE user_id = ?", userIdOf(account));
            assertThat(storedHash).isEqualTo(opaqueTokens.hash(tokens.refreshToken()));
        }

        @Test
        void recordsUserAgentAndAddressOfTheSession() throws Exception {
            Account account = registerVerifiedAccount();

            mockMvc.perform(postJson("/auth/login", json("username", account.username(), "password", PASSWORD))
                            .header("User-Agent", "T3Tarot/1.0 (iOS)"))
                    .andExpect(status().isOk());

            assertThat(scalar(
                            String.class,
                            "SELECT user_agent FROM auth_session WHERE user_id = ?",
                            userIdOf(account)))
                    .isEqualTo("T3Tarot/1.0 (iOS)");
            assertThat(scalar(
                            String.class,
                            "SELECT host(ip) FROM auth_session WHERE user_id = ?",
                            userIdOf(account)))
                    .isEqualTo("127.0.0.1");
        }

        @Test
        void ignoresUsernameCase() throws Exception {
            Account account = registerVerifiedAccount();

            mockMvc.perform(postJson(
                            "/auth/login", json("username", account.username().toUpperCase(), "password", PASSWORD)))
                    .andExpect(status().isOk());
        }

        @Test
        void resetsFailedLoginCounter() throws Exception {
            Account account = registerVerifiedAccount();
            wrongPasswordAttempt(account);
            wrongPasswordAttempt(account);

            loginAs(account).andExpect(status().isOk());

            assertThat(scalar(
                            Integer.class,
                            "SELECT failed_login_count FROM password_credential WHERE user_id = ?",
                            userIdOf(account)))
                    .isZero();
        }

        @Test
        void issuesUserRoleByDefault() throws Exception {
            Account account = registerVerifiedAccount();

            assertThat(rolesOf(loginTokens(account).accessToken())).containsExactly("USER");
        }

        @Test
        void issuesAdminRoleAfterPromotion() throws Exception {
            Account account = registerVerifiedAccount();
            execute("UPDATE app_user SET role = 'ADMIN' WHERE id = ?", userIdOf(account));

            assertThat(rolesOf(loginTokens(account).accessToken())).containsExactly("ADMIN");
        }

        @Test
        void doesNotEchoPasswordInResponse() throws Exception {
            Account account = registerVerifiedAccount();

            loginAs(account).andExpect(content().string(not(containsString(account.password()))));
        }
    }

    @Nested
    class WhenCredentialsAreRejected {

        @Test
        void answersSameCodeForWrongPasswordAndUnknownUser() throws Exception {
            Account account = registerVerifiedAccount();

            mockMvc.perform(postJson("/auth/login", json("username", account.username(), "password", "wrong-password-value")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(INVALID_CREDENTIALS));
            mockMvc.perform(postJson("/auth/login", json("username", "nobody" + UUID.randomUUID().toString().substring(0, 8), "password", PASSWORD)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(INVALID_CREDENTIALS));
        }

        @Test
        void rejectsUnverifiedEmail() throws Exception {
            Account account = newAccount();
            register(account).andExpect(status().isCreated());

            loginAs(account)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("error.auth.email_not_verified"));
        }

        @Test
        void doesNotRevealUnverifiedEmailWhenPasswordIsWrong() throws Exception {
            Account account = newAccount();
            register(account).andExpect(status().isCreated());

            wrongPasswordAttempt(account);
        }

        @Test
        void rejectsLockedAccountStatus() throws Exception {
            Account account = registerVerifiedAccount();
            execute("UPDATE app_user SET status = 'LOCKED' WHERE id = ?", userIdOf(account));

            loginAs(account)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(INVALID_CREDENTIALS));
        }

        @Test
        void rejectsPasswordLongerThanBcryptByteLimitWithoutServerError() throws Exception {
            Account account = registerVerifiedAccount();

            mockMvc.perform(postJson("/auth/login", json("username", account.username(), "password", "ệ".repeat(30))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(INVALID_CREDENTIALS));
        }

        @Test
        void reportsMissingFields() throws Exception {
            mockMvc.perform(postJson("/auth/login", "{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("username", "password")));
        }
    }

    @Nested
    class WhenPasswordIsGuessed {

        @Test
        void locksAccountAfterMaximumFailuresEvenForCorrectPassword() throws Exception {
            Account account = registerVerifiedAccount();
            for (int attempt = 0; attempt < 5; attempt++) {
                wrongPasswordAttempt(account);
            }

            loginAs(account)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(INVALID_CREDENTIALS));

            assertThat(scalar(
                            Boolean.class,
                            "SELECT locked_until IS NOT NULL FROM password_credential WHERE user_id = ?",
                            userIdOf(account)))
                    .isTrue();
        }

        @Test
        void allowsLoginOnceLockoutHasExpired() throws Exception {
            Account account = registerVerifiedAccount();
            for (int attempt = 0; attempt < 5; attempt++) {
                wrongPasswordAttempt(account);
            }
            execute(
                    "UPDATE password_credential SET locked_until = now() - interval '1 minute' WHERE user_id = ?",
                    userIdOf(account));

            loginAs(account).andExpect(status().isOk());

            assertThat(scalar(
                            Integer.class,
                            "SELECT failed_login_count FROM password_credential WHERE user_id = ?",
                            userIdOf(account)))
                    .isZero();
        }

        @Test
        void keepsFailedCounterWithinSmallintRangeWhileLocked() throws Exception {
            Account account = registerVerifiedAccount();

            for (int attempt = 0; attempt < 7; attempt++) {
                wrongPasswordAttempt(account);
            }

            assertThat(scalar(
                            Integer.class,
                            "SELECT failed_login_count FROM password_credential WHERE user_id = ?",
                            userIdOf(account)))
                    .isLessThanOrEqualTo(5);
        }

        @Test
        void bodyOfLockedResponseHasNoFreeText() throws Exception {
            Account account = registerVerifiedAccount();
            for (int attempt = 0; attempt < 5; attempt++) {
                wrongPasswordAttempt(account);
            }

            loginAs(account)
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.detail").doesNotExist())
                    .andExpect(jsonPath("$.title").doesNotExist());
        }
    }
}
