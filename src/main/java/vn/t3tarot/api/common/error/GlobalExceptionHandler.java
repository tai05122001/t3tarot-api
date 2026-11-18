package vn.t3tarot.api.common.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
        CommonErrorCode errorCode = CommonErrorCode.INVALID_PARAM;
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
        ProblemDetail body = ProblemDetail.forStatus(status);
        enrich(body, CommonErrorCode.INVALID_PARAM, violations);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = Stream.concat(
                        ex.getValueResults().stream().flatMap(GlobalExceptionHandler::toValueViolations),
                        ex.getBeanResults().stream().flatMap(GlobalExceptionHandler::toBeanViolations))
                .toList();
        ProblemDetail body = ProblemDetail.forStatus(status);
        enrich(body, CommonErrorCode.INVALID_PARAM, violations);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatus(status);
        enrich(body, CommonErrorCode.INVALID_PARAM,
                List.of(new FieldViolation(ex.getParameterName(), ViolationCodes.IS_MISSING)));
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
        ProblemDetail body = ProblemDetail.forStatus(status);
        enrich(body, CommonErrorCode.INVALID_PARAM, List.of(new FieldViolation(field, ViolationCodes.INVALID_TYPE)));
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
        body.setTitle("");
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

    private static Stream<FieldViolation> toValueViolations(ParameterValidationResult result) {
        String name = parameterName(result.getMethodParameter());
        return result.getResolvableErrors().stream()
                .map(error -> new FieldViolation(name, ViolationCodes.of(lastCode(error))));
    }

    private static Stream<FieldViolation> toBeanViolations(ParameterErrors errors) {
        return Stream.concat(
                errors.getFieldErrors().stream().map(GlobalExceptionHandler::toFieldViolation),
                errors.getGlobalErrors().stream().map(GlobalExceptionHandler::toGlobalViolation));
    }

    private static String lastCode(MessageSourceResolvable resolvable) {
        String[] codes = resolvable.getCodes();
        return codes == null || codes.length == 0 ? null : codes[codes.length - 1];
    }

    private static String parameterName(MethodParameter parameter) {
        String requestName = requestName(parameter);
        if (requestName != null) {
            return requestName;
        }
        String javaName = parameter.getParameterName();
        return javaName != null ? javaName : "arg" + parameter.getParameterIndex();
    }

    private static String requestName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null) {
            return firstNonEmpty(requestParam.name(), requestParam.value());
        }
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null) {
            return firstNonEmpty(pathVariable.name(), pathVariable.value());
        }
        RequestHeader requestHeader = parameter.getParameterAnnotation(RequestHeader.class);
        if (requestHeader != null) {
            return firstNonEmpty(requestHeader.name(), requestHeader.value());
        }
        return null;
    }

    private static String firstNonEmpty(String first, String second) {
        if (!first.isEmpty()) {
            return first;
        }
        return second.isEmpty() ? null : second;
    }

    private static FieldViolation toFieldViolation(FieldError error) {
        return new FieldViolation(error.getField(), ViolationCodes.of(error.getCode()));
    }

    private static FieldViolation toGlobalViolation(ObjectError error) {
        return new FieldViolation(error.getObjectName(), ViolationCodes.of(error.getCode()));
    }

    private static FieldViolation toConstraintViolation(ConstraintViolation<?> violation) {
        String constraint = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
        return new FieldViolation(lastNode(violation.getPropertyPath().toString()), ViolationCodes.of(constraint));
    }

    private static String lastNode(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
    }
}
