package vn.t3tarot.api.identity.session.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.t3tarot.api.identity.IdentityIntegrationTest;

class TokenControllerTest extends IdentityIntegrationTest {

    private static final String REFRESH_TOKEN_INVALID = "error.auth.refresh_token_invalid";

    private Tokens refreshTokens(String refreshToken) throws Exception {
        String body = refreshWith(refreshToken).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Tokens(JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }

    @Nested
    class WhenRefreshing {

        @Test
        void returnsNewTokenPairThatAuthenticates() throws Exception {
            Tokens first = registerVerifiedAndLogin();

            Tokens second = refreshTokens(first.refreshToken());

            assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
            mockMvc.perform(get("/auth/sessions").header("Authorization", bearer(second.accessToken())))
                    .andExpect(status().isOk());
        }

        @Test
        void picksUpRoleChangedSinceLogin() throws Exception {
            Account account = registerVerifiedAccount();
            Tokens first = loginTokens(account);
            assertThat(rolesOf(first.accessToken())).containsExactly("USER");
            execute("UPDATE app_user SET role = 'ADMIN' WHERE id = ?", userIdOf(account));

            Tokens second = refreshTokens(first.refreshToken());

            assertThat(rolesOf(second.accessToken())).containsExactly("ADMIN");
        }

        @Test
        void keepsSameSessionAcrossRotations() throws Exception {
            Account account = registerVerifiedAccount();
            Tokens first = loginTokens(account);

            refreshTokens(first.refreshToken());

            assertThat(scalar(Long.class, "SELECT count(*) FROM auth_session WHERE user_id = ?", userIdOf(account)))
                    .isEqualTo(1L);
        }

        @Test
        void acceptsPreviousTokenWithinGraceWindow() throws Exception {
            Tokens first = registerVerifiedAndLogin();
            refreshTokens(first.refreshToken());

            refreshWith(first.refreshToken()).andExpect(status().isOk());
        }

        @Test
        void acceptsPreviousTokenRepeatedlyWithinGraceWindow() throws Exception {
            Tokens first = registerVerifiedAndLogin();
            refreshTokens(first.refreshToken());

            refreshWith(first.refreshToken()).andExpect(status().isOk());
            refreshWith(first.refreshToken()).andExpect(status().isOk());
            refreshWith(first.refreshToken()).andExpect(status().isOk());
        }

        @Test
        void revokesSessionWhenPreviousTokenIsReplayedAfterGraceWindow() throws Exception {
            Account account = registerVerifiedAccount();
            Tokens first = loginTokens(account);
            Tokens second = refreshTokens(first.refreshToken());
            execute(
                    "UPDATE auth_session SET rotated_at = now() - interval '1 minute' WHERE user_id = ?",
                    userIdOf(account));

            refreshWith(first.refreshToken())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(REFRESH_TOKEN_INVALID));

            refreshWith(second.refreshToken()).andExpect(status().isUnauthorized());
            assertThat(scalar(
                            Boolean.class,
                            "SELECT revoked_at IS NOT NULL FROM auth_session WHERE user_id = ?",
                            userIdOf(account)))
                    .isTrue();
        }

        @Test
        void rejectsUnknownToken() throws Exception {
            refreshWith("unknown-token-value")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(REFRESH_TOKEN_INVALID));
        }

        @Test
        void rejectsExpiredSession() throws Exception {
            Account account = registerVerifiedAccount();
            Tokens tokens = loginTokens(account);
            execute(
                    "UPDATE auth_session SET expires_at = now() - interval '1 minute' WHERE user_id = ?",
                    userIdOf(account));

            refreshWith(tokens.refreshToken())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(REFRESH_TOKEN_INVALID));
        }

        @Test
        void rejectsRefreshWhenAccountIsLocked() throws Exception {
            Account account = registerVerifiedAccount();
            Tokens tokens = loginTokens(account);
            execute("UPDATE app_user SET status = 'LOCKED' WHERE id = ?", userIdOf(account));

            refreshWith(tokens.refreshToken())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(REFRESH_TOKEN_INVALID));
        }

        @Test
        void reportsMissingRefreshToken() throws Exception {
            mockMvc.perform(postJson("/auth/refresh", "{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("refreshToken"));
        }
    }

    @Nested
    class WhenLoggingOut {

        @Test
        void revokesTheSessionOfTheRefreshToken() throws Exception {
            Tokens tokens = registerVerifiedAndLogin();

            mockMvc.perform(postJson("/auth/logout", json("refreshToken", tokens.refreshToken())))
                    .andExpect(status().isNoContent());

            refreshWith(tokens.refreshToken()).andExpect(status().isUnauthorized());
        }

        @Test
        void revokesSessionWhenGivenThePreviousToken() throws Exception {
            Tokens first = registerVerifiedAndLogin();
            Tokens second = refreshTokens(first.refreshToken());

            mockMvc.perform(postJson("/auth/logout", json("refreshToken", first.refreshToken())))
                    .andExpect(status().isNoContent());

            refreshWith(second.refreshToken()).andExpect(status().isUnauthorized());
        }

        @Test
        void answersNoContentForUnknownToken() throws Exception {
            mockMvc.perform(postJson("/auth/logout", json("refreshToken", "unknown-token-value")))
                    .andExpect(status().isNoContent());
        }

        @Test
        void answersNoContentWhenCalledTwice() throws Exception {
            Tokens tokens = registerVerifiedAndLogin();
            String body = json("refreshToken", tokens.refreshToken());

            mockMvc.perform(postJson("/auth/logout", body)).andExpect(status().isNoContent());
            mockMvc.perform(postJson("/auth/logout", body)).andExpect(status().isNoContent());
        }
    }

    @Nested
    class WhenLoggingOutEverywhere {

        @Test
        void revokesEverySessionOfTheCaller() throws Exception {
            Account account = registerVerifiedAccount();
            Tokens phone = loginTokens(account);
            Tokens laptop = loginTokens(account);

            mockMvc.perform(
                            post("/auth/logout-all")
                                    .header("Authorization", bearer(phone.accessToken())))
                    .andExpect(status().isNoContent());

            refreshWith(phone.refreshToken()).andExpect(status().isUnauthorized());
            refreshWith(laptop.refreshToken()).andExpect(status().isUnauthorized());
        }

        @Test
        void leavesSessionsOfOtherUsersUntouched() throws Exception {
            Tokens caller = registerVerifiedAndLogin();
            Tokens other = registerVerifiedAndLogin();

            mockMvc.perform(
                            post("/auth/logout-all")
                                    .header("Authorization", bearer(caller.accessToken())))
                    .andExpect(status().isNoContent());

            refreshWith(other.refreshToken()).andExpect(status().isOk());
        }

        @Test
        void requiresAccessToken() throws Exception {
            mockMvc.perform(post("/auth/logout-all"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("error.auth.unauthenticated"));
        }
    }
}
