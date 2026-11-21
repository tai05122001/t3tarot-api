package vn.t3tarot.api.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.common.constant.CommonErrorCode;

class IdempotencyKeyTest {

    private static final String VALID_KEY = "3f2a9c1e-7b4d-4e8a-9c2f-1a2b3c4d5e6f";

    @Nested
    class WhenHeaderIsValid {

        @Test
        void parsesCanonicalUuid() {
            IdempotencyKey key = IdempotencyKey.parse(VALID_KEY);

            assertThat(key.value()).isEqualTo(UUID.fromString(VALID_KEY));
            assertThat(key).hasToString(VALID_KEY);
        }

        @Test
        void ignoresSurroundingWhitespace() {
            assertThat(IdempotencyKey.parse("  " + VALID_KEY + " ").value()).isEqualTo(UUID.fromString(VALID_KEY));
        }

        @Test
        void treatsUppercaseAndLowercaseAsSameKey() {
            assertThat(IdempotencyKey.parse(VALID_KEY.toUpperCase())).isEqualTo(IdempotencyKey.parse(VALID_KEY));
        }
    }

    @Nested
    class WhenHeaderIsMissing {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void throwsMissingWhenHeaderIsBlank(String rawValue) {
            assertThatThrownBy(() -> IdempotencyKey.parse(rawValue))
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getErrorCode())
                    .isEqualTo(CommonErrorCode.IDEMPOTENCY_KEY_MISSING);
        }
    }

    @Nested
    class WhenHeaderIsMalformed {

        @ParameterizedTest
        @ValueSource(strings = {"not-a-uuid", "1-1-1-1-1", "3f2a9c1e7b4d4e8a9c2f1a2b3c4d5e6f", "3f2a9c1e-7b4d-4e8a-9c2f-1a2b3c4d5e6g"})
        void throwsInvalidWhenNotCanonicalUuid(String rawValue) {
            assertThatThrownBy(() -> IdempotencyKey.parse(rawValue))
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getErrorCode())
                    .isEqualTo(CommonErrorCode.IDEMPOTENCY_KEY_INVALID);
        }
    }

    @Test
    void rejectsNullValueWhenConstructedDirectly() {
        assertThatThrownBy(() -> new IdempotencyKey(null)).isInstanceOf(NullPointerException.class);
    }
}
