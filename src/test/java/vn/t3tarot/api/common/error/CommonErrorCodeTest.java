package vn.t3tarot.api.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

class CommonErrorCodeTest {

    @Test
    void everyCodeFollowsErrorGroupReasonFormat() {
        assertThat(CommonErrorCode.values())
                .extracting(CommonErrorCode::code)
                .allMatch(ErrorCodeFormat::isValid);
    }

    @Test
    void invalidParamCodeMatchesDocumentedValue() {
        assertThat(CommonErrorCode.INVALID_PARAM.code()).isEqualTo("error.invalid_param.is_invalid");
    }

    @Test
    void everyCodeIsUnique() {
        long distinctCount = Arrays.stream(CommonErrorCode.values()).map(CommonErrorCode::code).distinct().count();

        assertThat(distinctCount).isEqualTo(CommonErrorCode.values().length);
    }

    @Test
    void noCodeUsesSuccessStatus() {
        assertThat(CommonErrorCode.values())
                .allMatch(errorCode -> errorCode.httpStatus().isError());
    }

    @ParameterizedTest
    @CsvSource({
            "400, INVALID_REQUEST",
            "401, UNAUTHENTICATED",
            "403, FORBIDDEN",
            "404, NOT_FOUND",
            "405, METHOD_NOT_ALLOWED",
            "406, INVALID_REQUEST",
            "409, CONFLICT",
            "415, UNSUPPORTED_MEDIA_TYPE",
            "429, RATE_LIMITED",
            "500, INTERNAL",
            "502, INTERNAL",
            "503, SERVICE_UNAVAILABLE",
            "504, UPSTREAM_TIMEOUT",
            "499, INVALID_REQUEST",
            "599, INTERNAL"
    })
    void mapsHttpStatusToCommonCodeWhenNoSpecificCodeGiven(int status, CommonErrorCode expected) {
        assertThat(CommonErrorCode.fromStatus(HttpStatusCode.valueOf(status))).isEqualTo(expected);
    }
}
