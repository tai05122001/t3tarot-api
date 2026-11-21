package vn.t3tarot.api.common.error;

import vn.t3tarot.api.common.constant.ErrorCode;

public class ApiException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.code());
    }

    public ApiException(ErrorCode errorCode, String debugMessage) {
        super(debugMessage);
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String debugMessage, Throwable cause) {
        super(debugMessage, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
