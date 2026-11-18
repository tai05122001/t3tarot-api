package vn.t3tarot.api.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ErrorCodeFormatTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "error.invalid_param.is_missing",
            "error.auth.forbidden",
            "error.followup.limit_reached",
            "error.idempotency_key.in_progress"
    })
    void acceptsErrorGroupReasonInSnakeCase(String code) {
        assertThat(ErrorCodeFormat.isValid(code)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "error",
            "error.forbidden",
            "error.invalid.param.is.missing",
            "error.Invalid_param.is_missing",
            "error.invalid-param.is_missing",
            "error.invalid_param.is_missing.extra",
            "error._invalid.is_missing",
            "error.invalid_.is_missing",
            "error.invalid__param.is_missing",
            "error.invalid_param.is_missing ",
            "ERROR.invalid_param.is_missing",
            "error.invalid_param.is_missing1"
    })
    void rejectsAnythingElse(String code) {
        assertThat(ErrorCodeFormat.isValid(code)).isFalse();
    }

    @Test
    void doesNotAcceptHumanReadableSentence() {
        assertThat(ErrorCodeFormat.isValid("error.invalid_param.Param is missing")).isFalse();
    }
}
