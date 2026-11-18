package vn.t3tarot.api.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "Idempotency-Key không hợp lệ"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy"),
    CONFLICT(HttpStatus.CONFLICT, "Xung đột trạng thái"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Quá nhiều yêu cầu"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống");

    private final HttpStatus status;
    private final String title;
}
