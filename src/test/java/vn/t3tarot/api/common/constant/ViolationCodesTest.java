package vn.t3tarot.api.common.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.t3tarot.api.common.error.ErrorCodeFormat;

class ViolationCodesTest {

    @ParameterizedTest
    @CsvSource({
            "NotNull, error.invalid_param.is_missing",
            "NotBlank, error.invalid_param.is_missing",
            "NotEmpty, error.invalid_param.is_missing",
            "Size, error.invalid_param.length_out_of_range",
            "Length, error.invalid_param.length_out_of_range",
            "Min, error.invalid_param.out_of_range",
            "Max, error.invalid_param.out_of_range",
            "DecimalMin, error.invalid_param.out_of_range",
            "DecimalMax, error.invalid_param.out_of_range",
            "Range, error.invalid_param.out_of_range",
            "Positive, error.invalid_param.out_of_range",
            "PositiveOrZero, error.invalid_param.out_of_range",
            "Negative, error.invalid_param.out_of_range",
            "NegativeOrZero, error.invalid_param.out_of_range",
            "Pattern, error.invalid_param.invalid_format",
            "Email, error.invalid_param.invalid_format",
            "typeMismatch, error.invalid_param.invalid_type"
    })
    void mapsKnownConstraintToCode(String constraint, String expected) {
        assertThat(ViolationCodes.of(constraint)).isEqualTo(expected);
    }

    @Test
    void fallsBackToIsInvalidForUnknownConstraint() {
        assertThat(ViolationCodes.of("AssertTrue")).isEqualTo(ViolationCodes.IS_INVALID);
    }

    @Test
    void fallsBackToIsInvalidWhenConstraintIsNull() {
        assertThat(ViolationCodes.of(null)).isEqualTo(ViolationCodes.IS_INVALID);
    }

    @Test
    void everyViolationCodeFollowsErrorCodeFormat() {
        assertThat(new String[] {
                ViolationCodes.IS_MISSING,
                ViolationCodes.IS_INVALID,
                ViolationCodes.INVALID_FORMAT,
                ViolationCodes.INVALID_TYPE,
                ViolationCodes.OUT_OF_RANGE,
                ViolationCodes.LENGTH_OUT_OF_RANGE
        }).allMatch(ErrorCodeFormat::isValid);
    }
}
