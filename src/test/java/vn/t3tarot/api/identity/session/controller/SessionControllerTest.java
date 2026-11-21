package vn.t3tarot.api.identity.session.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.t3tarot.api.identity.IdentityIntegrationTest;

class SessionControllerTest extends IdentityIntegrationTest {

    private Tokens loginWithUserAgent(Account account, String userAgent) throws Exception {
        String body = mockMvc.perform(postJson("/auth/login", json("username", account.username(), "password", PASSWORD))
                        .header("User-Agent", userAgent))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new Tokens(JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }

    private String sessionsOf(Tokens tokens) throws Exception {
        return mockMvc.perform(get("/auth/sessions").header("Authorization", bearer(tokens.accessToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private UUID sessionIdOfUserAgent(Account account, String userAgent) {
        return scalar(
                UUID.class,
                "SELECT id FROM auth_session WHERE user_id = ? AND user_agent = ?",
                userIdOf(account),
                userAgent);
    }

    @Nested
    class WhenListing {

        @Test
        void marksOnlyTheCallersOwnSessionAsCurrent() throws Exception {
            Account account = registerVerifiedAccount();
            Tokens phone = loginWithUserAgent(account, "phone-agent");
            loginWithUserAgent(account, "laptop-agent");

            String body = sessionsOf(phone);

            List<String> currentAgents = JsonPath.read(body, "$[?(@.isCurrent == true)].userAgent");
            assertThat(currentAgents).containsExactly("phone-agent");
            assertThat((List<?>) JsonPath.read(body, "$")).hasSize(2);
        }

        @Test
        void hidesRevokedAndExpiredSessions() throws Exception {
            Account account = registerVerifiedAccount();
            Tokens current = loginWithUserAgent(account, "current-agent");
            loginWithUserAgent(account, "revoked-agent");
            loginWithUserAgent(account, "expired-agent");
            execute(
                    "UPDATE auth_session SET revoked_at = now() WHERE user_id = ? AND user_agent = 'revoked-agent'",
                    userIdOf(account));
            execute(
                    "UPDATE auth_session SET expires_at = now() - interval '1 minute' WHERE user_id = ? AND user_agent = 'expired-agent'",
                    userIdOf(account));

            String body = sessionsOf(current);

            assertThat((List<?>) JsonPath.read(body, "$")).hasSize(1);
        }

        @Test
        void neverListsSessionsOfOtherUsers() throws Exception {
            Tokens caller = registerVerifiedAndLogin();
            registerVerifiedAndLogin();

            mockMvc.perform(get("/auth/sessions").header("Authorization", bearer(caller.accessToken())))
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void exposesNeitherTokenHashNorIpAddress() throws Exception {
            Tokens tokens = registerVerifiedAndLogin();

            String body = sessionsOf(tokens);

            assertThat(body).doesNotContain("refresh").doesNotContain("127.0.0.1").doesNotContain("ip");
        }

        @Test
        void requiresAccessToken() throws Exception {
            mockMvc.perform(get("/auth/sessions"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("error.auth.unauthenticated"));
        }
    }

    @Nested
    class WhenRevoking {

        @Test
        void revokesOneOwnSessionAndLeavesTheOthers() throws Exception {
            Account account = registerVerifiedAccount();
            Tokens phone = loginWithUserAgent(account, "phone-agent");
            Tokens laptop = loginWithUserAgent(account, "laptop-agent");
            UUID laptopSession = sessionIdOfUserAgent(account, "laptop-agent");

            mockMvc.perform(delete("/auth/sessions/" + laptopSession)
                            .header("Authorization", bearer(phone.accessToken())))
                    .andExpect(status().isNoContent());

            refreshWith(laptop.refreshToken()).andExpect(status().isUnauthorized());
            refreshWith(phone.refreshToken()).andExpect(status().isOk());
        }

        @Test
        void answersNoContentWhenSessionIsAlreadyRevoked() throws Exception {
            Account account = registerVerifiedAccount();
            Tokens tokens = loginWithUserAgent(account, "phone-agent");
            UUID sessionId = sessionIdOfUserAgent(account, "phone-agent");
            String path = "/auth/sessions/" + sessionId;

            mockMvc.perform(delete(path).header("Authorization", bearer(tokens.accessToken())))
                    .andExpect(status().isNoContent());
            mockMvc.perform(delete(path).header("Authorization", bearer(tokens.accessToken())))
                    .andExpect(status().isNoContent());
        }

        @Test
        void answersNotFoundForSessionOfAnotherUserAndKeepsItAlive() throws Exception {
            Account victim = registerVerifiedAccount();
            Tokens victimTokens = loginWithUserAgent(victim, "victim-agent");
            Tokens attacker = registerVerifiedAndLogin();
            UUID victimSession = sessionIdOfUserAgent(victim, "victim-agent");

            mockMvc.perform(delete("/auth/sessions/" + victimSession)
                            .header("Authorization", bearer(attacker.accessToken())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("error.resource.not_found"));

            refreshWith(victimTokens.refreshToken()).andExpect(status().isOk());
        }

        @Test
        void answersNotFoundForUnknownSession() throws Exception {
            Tokens tokens = registerVerifiedAndLogin();

            mockMvc.perform(delete("/auth/sessions/" + UUID.randomUUID())
                            .header("Authorization", bearer(tokens.accessToken())))
                    .andExpect(status().isNotFound());
        }

        @Test
        void rejectsSessionIdThatIsNotAUuid() throws Exception {
            Tokens tokens = registerVerifiedAndLogin();

            mockMvc.perform(delete("/auth/sessions/not-a-uuid").header("Authorization", bearer(tokens.accessToken())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid_param.is_invalid"));
        }

        @Test
        void requiresAccessToken() throws Exception {
            mockMvc.perform(delete("/auth/sessions/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        }
    }
}
