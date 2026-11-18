package vn.t3tarot.api.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum CommonErrorCode implements ErrorCode {

    INVALID_REQUEST("error.invalid.request", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_MISSING("error.idempotency.key.missing", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_INVALID("error.idempotency.key.invalid", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_REUSED("error.idempotency.key.reused", HttpStatus.UNPROCESSABLE_CONTENT),
    IDEMPOTENCY_KEY_IN_PROGRESS("error.idempotency.key.in.progress", HttpStatus.CONFLICT),
    UNAUTHENTICATED("error.unauthenticated", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("error.forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND("error.not.found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("error.method.not.allowed", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE("error.unsupported.media.type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    CONFLICT("error.conflict", HttpStatus.CONFLICT),
    RATE_LIMITED("error.rate.limited", HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL("error.internal", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("error.service.unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    UPSTREAM_TIMEOUT("error.upstream.timeout", HttpStatus.GATEWAY_TIMEOUT);

    private final String code;
    private final HttpStatus httpStatus;

    CommonErrorCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public static CommonErrorCode fromStatus(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            return statusCode.is5xxServerError() ? INTERNAL : INVALID_REQUEST;
        }
        return switch (status) {
            case UNAUTHORIZED -> UNAUTHENTICATED;
            case FORBIDDEN -> FORBIDDEN;
            case NOT_FOUND -> NOT_FOUND;
            case METHOD_NOT_ALLOWED -> METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> UNSUPPORTED_MEDIA_TYPE;
            case CONFLICT -> CONFLICT;
            case TOO_MANY_REQUESTS -> RATE_LIMITED;
            case SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE;
            case GATEWAY_TIMEOUT -> UPSTREAM_TIMEOUT;
            default -> status.is5xxServerError() ? INTERNAL : INVALID_REQUEST;
        };
    }
}
