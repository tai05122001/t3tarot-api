package vn.t3tarot.api.common.constant;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String code();

    HttpStatus httpStatus();
}
