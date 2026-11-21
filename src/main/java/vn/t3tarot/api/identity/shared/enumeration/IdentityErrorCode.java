package vn.t3tarot.api.identity.shared.enumeration;

import org.springframework.http.HttpStatus;
import vn.t3tarot.api.common.constant.ErrorCode;

public enum IdentityErrorCode implements ErrorCode {

    USERNAME_ALREADY_TAKEN("error.username.already_taken", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("error.auth.invalid_credentials", HttpStatus.UNAUTHORIZED),
    EMAIL_NOT_VERIFIED("error.auth.email_not_verified", HttpStatus.FORBIDDEN),
    ID_TOKEN_INVALID("error.auth.id_token_invalid", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_REGISTERED("error.auth.email_already_registered", HttpStatus.CONFLICT),
    REFRESH_TOKEN_INVALID("error.auth.refresh_token_invalid", HttpStatus.UNAUTHORIZED),
    VERIFICATION_TOKEN_INVALID("error.verification_token.invalid", HttpStatus.BAD_REQUEST);

    private final String code;
    private final HttpStatus httpStatus;

    IdentityErrorCode(String code, HttpStatus httpStatus) {
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
}
