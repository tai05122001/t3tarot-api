package vn.t3tarot.api.identity.emailverification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import vn.t3tarot.api.identity.IdentityIntegrationTest;

class EmailVerificationPageControllerTest extends IdentityIntegrationTest {

    private static final String PAGE = "/auth/email/verify-page";

    private Account registerUnverified() throws Exception {
        Account account = newAccount();
        register(account).andExpect(status().isCreated());
        return account;
    }

    private ResultActions open(String token) throws Exception {
        return mockMvc.perform(get(PAGE).param("token", token));
    }

    private ResultActions confirm(String token) throws Exception {
        return mockMvc.perform(post(PAGE).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("token", token));
    }

    private boolean isEmailVerified(Account account) {
        return scalar(
                Boolean.class,
                "SELECT verified_at IS NOT NULL FROM user_identity WHERE provider_subject = ?",
                account.email());
    }

    @Nested
    class WhenOpeningTheLink {

        @Test
        void showsConfirmPageWithoutVerifyingTheEmail() throws Exception {
            Account account = registerUnverified();
            String token = mailSender.lastTokenOf(account.email()).orElseThrow();

            open(token)
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Xác nhận email")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"" + token + "\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"" + PAGE + "\"")));

            assertThat(isEmailVerified(account)).isFalse();
            assertThat(scalar(
                            Boolean.class,
                            "SELECT consumed_at IS NULL FROM email_verification WHERE user_id = ?",
                            userIdOf(account)))
                    .isTrue();
        }

        @Test
        void isReachableWithoutAccessToken() throws Exception {
            open("any-token-value").andExpect(status().isOk());
        }

        @Test
        void sendsPageSecurityHeadersInsteadOfTheApiDefaults() throws Exception {
            MvcResult result = open("any-token-value").andReturn();

            assertThat(result.getResponse().getHeaders("Content-Security-Policy"))
                    .containsExactly(EmailVerificationPageController.CONTENT_SECURITY_POLICY);
            assertThat(result.getResponse().getHeaders("Referrer-Policy")).containsExactly("no-referrer");
            assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
        }

        @Test
        void showsInvalidLinkPageWhenTokenIsMissing() throws Exception {
            mockMvc.perform(get(PAGE))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Link không hợp lệ")));
        }

        @Test
        void showsInvalidLinkPageWhenTokenIsTooLong() throws Exception {
            open("a".repeat(129))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Link không hợp lệ")));
        }

        @Test
        void escapesTheTokenInTheHtml() throws Exception {
            String hostile = "\"><script>alert(1)</script>";

            String html = open(hostile).andReturn().getResponse().getContentAsString();

            assertThat(html).doesNotContain("<script>alert(1)</script>").contains("&lt;script&gt;");
        }
    }

    @Nested
    class WhenConfirming {

        @Test
        void verifiesTheEmailAndAllowsLogin() throws Exception {
            Account account = registerUnverified();
            String token = mailSender.lastTokenOf(account.email()).orElseThrow();

            confirm(token)
                    .andExpect(status().isOk())
                    .andExpect(header().string("Referrer-Policy", "no-referrer"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã xác nhận email")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost:5173/login")));

            assertThat(isEmailVerified(account)).isTrue();
            loginAs(account).andExpect(status().isOk());
        }

        @Test
        void showsUsedLinkPageWhenTheLinkIsOpenedAgain() throws Exception {
            Account account = registerUnverified();
            String token = mailSender.lastTokenOf(account.email()).orElseThrow();
            confirm(token).andExpect(status().isOk());

            confirm(token)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Link này đã được dùng")));
        }

        @Test
        void showsExpiredLinkPageWithResendLink() throws Exception {
            Account account = registerUnverified();
            String token = mailSender.lastTokenOf(account.email()).orElseThrow();
            execute(
                    "UPDATE email_verification SET expires_at = now() - interval '1 minute' WHERE user_id = ?",
                    userIdOf(account));

            confirm(token)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Link đã hết hạn")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("24 giờ")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost:5173/verify-email")));
            assertThat(isEmailVerified(account)).isFalse();
        }

        @Test
        void showsAlreadyVerifiedPageWhenTheEmailWasVerifiedByAnotherLink() throws Exception {
            Account account = registerUnverified();
            String token = mailSender.lastTokenOf(account.email()).orElseThrow();
            execute(
                    "UPDATE email_verification SET expires_at = now() - interval '1 minute' WHERE user_id = ?",
                    userIdOf(account));
            execute("UPDATE user_identity SET verified_at = now() WHERE provider_subject = ?", account.email());

            confirm(token)
                    .andExpect(status().isOk())
                    .andExpect(content()
                            .string(org.hamcrest.Matchers.containsString("Email đã được xác nhận trước đó")));
        }

        @Test
        void showsInvalidLinkPageForUnknownToken() throws Exception {
            confirm("unknown-token-value")
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Link không hợp lệ")));
        }

        @Test
        void showsInvalidLinkPageForMissingToken() throws Exception {
            mockMvc.perform(post(PAGE).contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Link không hợp lệ")));
        }

        @Test
        void showsInvalidLinkPageForOversizedToken() throws Exception {
            confirm("a".repeat(129))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Link không hợp lệ")));
        }

        @Test
        void rejectsRequestThatIsNotAFormSubmission() throws Exception {
            mockMvc.perform(post(PAGE).contentType(JSON).content("{\"token\":\"x\"}"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        void doesNotLeakAnythingAboutTheAccountInThePage() throws Exception {
            Account account = registerUnverified();
            String token = mailSender.lastTokenOf(account.email()).orElseThrow();

            String html = confirm(token).andReturn().getResponse().getContentAsString();

            assertThat(html).doesNotContain(account.email()).doesNotContain(account.username());
        }
    }
}
