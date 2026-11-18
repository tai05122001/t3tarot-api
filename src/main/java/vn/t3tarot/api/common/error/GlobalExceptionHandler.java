package vn.t3tarot.api.common.error;

import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import vn.t3tarot.api.common.web.RequestIdFilter;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ProblemDetail problem = ProblemDetail.forStatus(errorCode.getStatus());
        problem.setTitle(errorCode.getTitle());
        problem.setProperty("code", errorCode.name());
        problem.setProperty("requestId", MDC.get(RequestIdFilter.MDC_KEY));
        return ResponseEntity.status(errorCode.getStatus()).body(problem);
    }
}
