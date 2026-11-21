package vn.t3tarot.api.identity.sociallogin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import vn.t3tarot.api.identity.GoogleTokenFactory;
import vn.t3tarot.api.identity.IdentityIntegrationTest;

class SocialLoginControllerTest extends IdentityIntegrationTest {

    private static final String ID_TOKEN_INVALID = "error.auth.id_token_invalid";

    private static String newSubject() {
        return "google-" + UUID.randomUUID();
    }

    private static String newEmail() {
        return "google-" + UUID.randomUUID() + "@example.test";
    }

    private ResultActions loginWithGoogle(String idToken) throws Exception {
        return mockMvc.perform(postJson("/auth/social/google", json("idToken", idToken)));
    }

    @Nested
    class WhenTokenIsValid {

        @Test
        void createsAccountOnFirstLogin() throws Exception {
            String subject = newSubject();
            String email = newEmail();

            loginWithGoogle(GoogleTokenFactory.validToken(subject, email))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isNewUser").value(true))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());

            assertThat(scalar(
                            Boolean.class,
                            "SELECT verified_at IS NOT NULL FROM user_identity WHERE provider = 'GOOGLE' AND provider_subject = ?",
                            subject))
                    .isTrue();
            assertThat(scalar(
                            String.class,
                            "SELECT u.display_name FROM app_user u JOIN user_identity ui ON ui.user_id = u.id WHERE ui.provider_subject = ?",
                            subject))
                    .isEqualTo(GoogleTokenFactory.DISPLAY_NAME);
        }

        @Test
        void signsInSameAccountOnSecondLogin() throws Exception {
            String subject = newSubject();
            String email = newEmail();
            loginWithGoogle(GoogleTokenFactory.validToken(subject, email)).andExpect(status().isOk());

            loginWithGoogle(GoogleTokenFactory.validToken(subject, email))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isNewUser").value(false));

            assertThat(scalar(Long.class, "SELECT count(*) FROM user_identity WHERE provider_subject = ?", subject))
                    .isEqualTo(1L);
            assertThat(scalar(
                            Long.class,
                            "SELECT count(*) FROM auth_session s JOIN user_identity ui ON ui.user_id = s.user_id WHERE ui.provider_subject = ?",
                            subject))
                    .isEqualTo(2L);
        }

        @Test
        void issuesRefreshTokenThatCanBeExchanged() throws Exception {
            String body = loginWithGoogle(GoogleTokenFactory.validToken(newSubject(), newEmail()))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            String refreshToken = JsonPath.read(body, "$.refreshToken");

            refreshWith(refreshToken).andExpect(status().isOk());
        }
    }

    @Nested
    class WhenTokenIsRejected {

        @Test
        void rejectsTokenIssuedForAnotherClient() throws Exception {
            String token = GoogleTokenFactory.token(
                    newSubject(), newEmail(), Boolean.TRUE, "other-client", Duration.ofMinutes(5));

            loginWithGoogle(token)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ID_TOKEN_INVALID));
        }

        @Test
        void rejectsExpiredToken() throws Exception {
            String token = GoogleTokenFactory.token(
                    newSubject(), newEmail(), Boolean.TRUE, GoogleTokenFactory.CLIENT_ID, Duration.ofMinutes(-10));

            loginWithGoogle(token)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ID_TOKEN_INVALID));
        }

        @Test
        void rejectsUnverifiedGoogleEmail() throws Exception {
            String token = GoogleTokenFactory.token(
                    newSubject(), newEmail(), Boolean.FALSE, GoogleTokenFactory.CLIENT_ID, Duration.ofMinutes(5));

            loginWithGoogle(token)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ID_TOKEN_INVALID));
        }

        @Test
        void rejectsMalformedToken() throws Exception {
            loginWithGoogle("not.a.jwt")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ID_TOKEN_INVALID));
        }

        @Test
        void reportsMissingIdToken() throws Exception {
            mockMvc.perform(postJson("/auth/social/google", "{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("idToken"));
        }
    }

    @Nested
    class WhenAccountConflicts {

        @Test
        void rejectsEmailAlreadyRegisteredWithPassword() throws Exception {
            Account account = registerVerifiedAccount();

            loginWithGoogle(GoogleTokenFactory.validToken(newSubject(), account.email()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("error.auth.email_already_registered"));
        }

        @Test
        void rejectsLockedAccount() throws Exception {
            String subject = newSubject();
            String email = newEmail();
            loginWithGoogle(GoogleTokenFactory.validToken(subject, email)).andExpect(status().isOk());
            execute(
                    "UPDATE app_user SET status = 'LOCKED' WHERE id = (SELECT user_id FROM user_identity WHERE provider_subject = ?)",
                    subject);

            loginWithGoogle(GoogleTokenFactory.validToken(subject, email))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("error.auth.invalid_credentials"));
        }
    }
}
