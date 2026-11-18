package vn.t3tarot.api.common.error;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getTitle());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getTitle(), cause);
        this.errorCode = errorCode;
    }
}
