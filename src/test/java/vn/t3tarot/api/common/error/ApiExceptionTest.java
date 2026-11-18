package vn.t3tarot.api.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiExceptionTest {

    @Test
    void usesCodeAsMessageWhenNoDebugMessageGiven() {
        ApiException ex = new ApiException(CommonErrorCode.CONFLICT);

        assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("error.conflict");
    }

    @Test
    void keepsCauseWhenWrappingAnotherException() {
        IllegalStateException cause = new IllegalStateException("upstream");

        ApiException ex = new ApiException(CommonErrorCode.UPSTREAM_TIMEOUT, "llm timed out", cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).isEqualTo("llm timed out");
    }
}
