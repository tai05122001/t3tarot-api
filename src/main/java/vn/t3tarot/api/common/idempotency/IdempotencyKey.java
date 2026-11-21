package vn.t3tarot.api.common.idempotency;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import vn.t3tarot.api.common.error.ApiException;
import vn.t3tarot.api.common.constant.CommonErrorCode;

public record IdempotencyKey(UUID value) {

    public static final String HEADER_NAME = "Idempotency-Key";

    private static final Pattern CANONICAL_UUID =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public IdempotencyKey {
        Objects.requireNonNull(value, "value");
    }

    public static IdempotencyKey parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new ApiException(CommonErrorCode.IDEMPOTENCY_KEY_MISSING);
        }
        String trimmedValue = rawValue.strip();
        if (!CANONICAL_UUID.matcher(trimmedValue).matches()) {
            throw new ApiException(CommonErrorCode.IDEMPOTENCY_KEY_INVALID);
        }
        return new IdempotencyKey(UUID.fromString(trimmedValue));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
