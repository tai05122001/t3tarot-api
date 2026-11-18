package vn.t3tarot.api.common.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String CODE_PROPERTY = "code";
    public static final String REQUEST_ID_PROPERTY = "requestId";
    public static final String ERRORS_PROPERTY = "errors";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        if (errorCode.httpStatus().is5xxServerError()) {
            log.error("Api exception code={}", errorCode.code(), ex);
        } else {
            log.debug("Api exception code={}", errorCode.code());
        }
        return toResponse(ProblemDetail.forStatus(errorCode.httpStatus()), errorCode, List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::toConstraintViolation)
                .toList();
        CommonErrorCode errorCode = CommonErrorCode.INVALID_REQUEST;
        return toResponse(ProblemDetail.forStatus(errorCode.httpStatus()), errorCode, violations);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication() {
        CommonErrorCode errorCode = CommonErrorCode.UNAUTHENTICATED;
        return toResponse(ProblemDetail.forStatus(errorCode.httpStatus()), errorCode, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied() {
        CommonErrorCode errorCode = CommonErrorCode.FORBIDDEN;
        return toResponse(ProblemDetail.forStatus(errorCode.httpStatus()), errorCode, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unexpected exception", ex);
        CommonErrorCode errorCode = CommonErrorCode.INTERNAL;
        return toResponse(ProblemDetail.forStatus(errorCode.httpStatus()), errorCode, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = Stream.concat(
                        ex.getBindingResult().getFieldErrors().stream().map(GlobalExceptionHandler::toFieldViolation),
                        ex.getBindingResult().getGlobalErrors().stream().map(GlobalExceptionHandler::toGlobalViolation))
                .toList();
        ProblemDetail body = ex.getBody();
        enrich(body, CommonErrorCode.INVALID_REQUEST, violations);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ProblemDetail problemDetail = body instanceof ProblemDetail detail ? detail : ProblemDetail.forStatus(statusCode);
        if (problemDetail.getProperties() == null || !problemDetail.getProperties().containsKey(CODE_PROPERTY)) {
            enrich(problemDetail, CommonErrorCode.fromStatus(statusCode), List.of());
        }
        return super.handleExceptionInternal(ex, problemDetail, headers, statusCode, request);
    }

    private static ResponseEntity<Object> toResponse(
            ProblemDetail body, ErrorCode errorCode, List<FieldViolation> violations) {
        enrich(body, errorCode, violations);
        return ResponseEntity.status(errorCode.httpStatus()).body(body);
    }

    private static void enrich(ProblemDetail body, ErrorCode errorCode, List<FieldViolation> violations) {
        body.setDetail(null);
        body.setProperty(CODE_PROPERTY, errorCode.code());
        String requestId = MDC.get(REQUEST_ID_MDC_KEY);
        if (requestId != null) {
            body.setProperty(REQUEST_ID_PROPERTY, requestId);
        }
        if (!violations.isEmpty()) {
            body.setProperty(ERRORS_PROPERTY, violations);
        }
    }

    private static FieldViolation toFieldViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getCode());
    }

    private static FieldViolation toGlobalViolation(ObjectError error) {
        return new FieldViolation(error.getObjectName(), error.getCode());
    }

    private static FieldViolation toConstraintViolation(ConstraintViolation<?> violation) {
        String reason = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
        return new FieldViolation(lastNode(violation.getPropertyPath().toString()), reason);
    }

    private static String lastNode(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
    }
}
