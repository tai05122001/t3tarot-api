package vn.t3tarot.api.identity.register.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.t3tarot.api.identity.IdentityIntegrationTest;

class RegisterControllerTest extends IdentityIntegrationTest {

    @Nested
    class WhenRequestIsValid {

        @Test
        void createsAccountAndSendsOneVerificationMail() throws Exception {
            Account account = newAccount();

            register(account).andExpect(status().isCreated()).andExpect(jsonPath("$.userId").isNotEmpty());

            assertThat(mailSender.sentCountOf(account.email())).isEqualTo(1);
        }

        @Test
        void storesBcryptHashAndNeverThePlainPassword() throws Exception {
            Account account = newAccount();

            register(account).andExpect(status().isCreated());

            String hash = scalar(
                    String.class,
                    "SELECT password_hash FROM password_credential WHERE username_normalized = ?",
                    account.username().toLowerCase());
            assertThat(hash).startsWith("$2").doesNotContain(account.password());
        }

        @Test
        void createsUnverifiedEmailIdentity() throws Exception {
            Account account = newAccount();

            register(account).andExpect(status().isCreated());

            Boolean isUnverified = scalar(
                    Boolean.class,
                    "SELECT verified_at IS NULL FROM user_identity WHERE provider = 'EMAIL' AND provider_subject = ?",
                    account.email());
            assertThat(isUnverified).isTrue();
        }

        @Test
        void doesNotEchoPasswordInResponse() throws Exception {
            Account account = newAccount();

            register(account).andExpect(content().string(not(containsString(account.password()))));
        }

        @Test
        void stillCreatesAccountWhenMailProviderFails() throws Exception {
            Account account = newAccount();
            mailSender.failWhileSending(true);

            register(account).andExpect(status().isCreated());

            assertThat(scalar(
                            Long.class,
                            "SELECT count(*) FROM password_credential WHERE username_normalized = ?",
                            account.username().toLowerCase()))
                    .isEqualTo(1L);
        }
    }

    @Nested
    class WhenAccountAlreadyExists {

        @Test
        void rejectsTakenUsernameIgnoringCase() throws Exception {
            Account first = newAccount();
            register(first).andExpect(status().isCreated());
            Account second = new Account(first.username().toUpperCase(), newAccount().email(), PASSWORD);

            register(second)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("error.username.already_taken"));

            assertThat(scalar(Long.class, "SELECT count(*) FROM user_identity WHERE provider_subject = ?", second.email()))
                    .isZero();
        }

        @Test
        void answersCreatedWithoutCreatingAnythingWhenEmailIsTaken() throws Exception {
            Account first = newAccount();
            register(first).andExpect(status().isCreated());
            Account second = new Account(newAccount().username(), first.email(), PASSWORD);

            register(second).andExpect(status().isCreated());

            assertThat(mailSender.sentCountOf(first.email())).isEqualTo(1);
            assertThat(scalar(
                            Long.class,
                            "SELECT count(*) FROM password_credential WHERE username_normalized = ?",
                            second.username().toLowerCase()))
                    .isZero();
        }

        @Test
        void treatsEmailCaseInsensitively() throws Exception {
            Account first = newAccount();
            register(first).andExpect(status().isCreated());
            Account second = new Account(newAccount().username(), first.email().toUpperCase(), PASSWORD);

            register(second).andExpect(status().isCreated());

            assertThat(scalar(
                            Long.class,
                            "SELECT count(*) FROM user_identity WHERE email_normalized = ?",
                            first.email()))
                    .isEqualTo(1L);
        }
    }

    @Nested
    class WhenRequestIsInvalid {

        @Test
        void requiresIdempotencyKey() throws Exception {
            mockMvc.perform(postJson("/auth/register", registerJson(newAccount())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.idempotency_key.is_missing"));
        }

        @Test
        void rejectsMalformedIdempotencyKey() throws Exception {
            mockMvc.perform(postJson("/auth/register", registerJson(newAccount()))
                            .header("Idempotency-Key", "not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.idempotency_key.is_invalid"));
        }

        @Test
        void reportsFieldCodesForInvalidFields() throws Exception {
            String body = json("username", "a", "email", "not-an-email", "password", "short");

            mockMvc.perform(postJsonWithKey("/auth/register", body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid_param.is_invalid"))
                    .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("username", "email", "password")));
        }

        @Test
        void reportsMissingFields() throws Exception {
            mockMvc.perform(postJsonWithKey("/auth/register", "{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("username", "email", "password")));
        }

        @Test
        void rejectsPasswordLongerThanBcryptByteLimit() throws Exception {
            Account account = new Account(newAccount().username(), newAccount().email(), "ệ".repeat(30));

            register(account)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid_param.is_invalid"));

            assertThat(scalar(
                            Long.class,
                            "SELECT count(*) FROM user_identity WHERE provider_subject = ?",
                            account.email()))
                    .isZero();
        }
    }
}
