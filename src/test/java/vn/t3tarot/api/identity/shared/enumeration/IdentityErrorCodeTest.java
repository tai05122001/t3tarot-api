package vn.t3tarot.api.identity.shared.enumeration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.t3tarot.api.common.error.ErrorCodeFormat;

class IdentityErrorCodeTest {

    @Test
    void everyCodeMatchesTheCodeFormat() {
        assertThat(Arrays.stream(IdentityErrorCode.values()).map(IdentityErrorCode::code))
                .allMatch(ErrorCodeFormat::isValid);
    }

    @Test
    void codesAreUnique() {
        assertThat(Arrays.stream(IdentityErrorCode.values()).map(IdentityErrorCode::code))
                .doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @CsvSource({
        "USERNAME_ALREADY_TAKEN, error.username.already_taken, 409",
        "INVALID_CREDENTIALS, error.auth.invalid_credentials, 401",
        "EMAIL_NOT_VERIFIED, error.auth.email_not_verified, 403",
        "ID_TOKEN_INVALID, error.auth.id_token_invalid, 401",
        "EMAIL_ALREADY_REGISTERED, error.auth.email_already_registered, 409",
        "REFRESH_TOKEN_INVALID, error.auth.refresh_token_invalid, 401",
        "VERIFICATION_TOKEN_INVALID, error.verification_token.invalid, 400"
    })
    void mapsEachCodeToItsPublishedValueAndStatus(IdentityErrorCode errorCode, String code, int status) {
        assertThat(errorCode.code()).isEqualTo(code);
        assertThat(errorCode.httpStatus().value()).isEqualTo(status);
    }
}
