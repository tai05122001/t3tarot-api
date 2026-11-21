package vn.t3tarot.api.common.constant;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum CommonErrorCode implements ErrorCode {

    INVALID_REQUEST("error.invalid_request.malformed", HttpStatus.BAD_REQUEST),
    INVALID_PARAM("error.invalid_param.is_invalid", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_MISSING("error.idempotency_key.is_missing", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_INVALID("error.idempotency_key.is_invalid", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_REUSED("error.idempotency_key.is_reused", HttpStatus.UNPROCESSABLE_CONTENT),
    IDEMPOTENCY_KEY_IN_PROGRESS("error.idempotency_key.in_progress", HttpStatus.CONFLICT),
    UNAUTHENTICATED("error.auth.unauthenticated", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("error.auth.forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND("error.resource.not_found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("error.http.method_not_allowed", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE("error.http.unsupported_media_type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    CONFLICT("error.resource.conflict", HttpStatus.CONFLICT),
    RATE_LIMITED("error.rate_limit.exceeded", HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL("error.server.internal", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("error.server.unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    UPSTREAM_TIMEOUT("error.server.upstream_timeout", HttpStatus.GATEWAY_TIMEOUT);

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
