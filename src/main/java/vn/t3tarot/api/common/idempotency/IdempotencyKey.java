package vn.t3tarot.api.common.idempotency;

import java.util.UUID;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.common.error.ErrorCode;

public record IdempotencyKey(UUID value) {

    public static final String HEADER = "Idempotency-Key";

    public IdempotencyKey {
        if (value == null) {
            throw new ApiException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
    }

    public static IdempotencyKey parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
        try {
            return new IdempotencyKey(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_IDEMPOTENCY_KEY, exception);
        }
    }
}
