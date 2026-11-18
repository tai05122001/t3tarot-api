package vn.t3tarot.api.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.common.error.ErrorCode;

class IdempotencyKeyTest {

    @Test
    void parsesValidUuidWithSurroundingWhitespace() {
        UUID uuid = UUID.fromString("0b9f8f0e-6c0e-4e6e-9a54-2f4f0f6f8a11");

        assertThat(IdempotencyKey.parse("  " + uuid + " ").value()).isEqualTo(uuid);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "not-a-uuid", "12345"})
    void rejectsMissingOrMalformedKey(String raw) {
        assertThatThrownBy(() -> IdempotencyKey.parse(raw))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_IDEMPOTENCY_KEY));
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> new IdempotencyKey(null)).isInstanceOf(ApiException.class);
    }
}
