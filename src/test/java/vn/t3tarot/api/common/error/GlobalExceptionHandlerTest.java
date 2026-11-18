package vn.t3tarot.api.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private static final String REQUEST_ID = "req-test-0001";
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        MDC.put(GlobalExceptionHandler.REQUEST_ID_MDC_KEY, REQUEST_ID);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    class WhenApiExceptionThrown {

        @Test
        void returnsStatusAndCodeOfErrorCode() throws Exception {
            mockMvc.perform(get("/stub/conflict"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("error.conflict"))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
        }

        @Test
        void hidesDebugMessageFromClient() throws Exception {
            mockMvc.perform(get("/stub/conflict"))
                    .andExpect(jsonPath("$.detail").doesNotExist());
        }

        @Test
        void returnsModuleSpecificCodeWhenModuleDefinesOwnErrorCode() throws Exception {
            mockMvc.perform(get("/stub/module-error"))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.code").value("error.stub.limit.reached"));
        }

        @Test
        void returnsInternalCodeWhenErrorCodeIsServerSide() throws Exception {
            mockMvc.perform(get("/stub/upstream-timeout"))
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code").value("error.upstream.timeout"));
        }
    }

    @Nested
    class WhenRequestInvalid {

        @Test
        void listsFieldViolationsWithoutRejectedValue() throws Exception {
            mockMvc.perform(post("/stub/questions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid.request"))
                    .andExpect(jsonPath("$.errors[0].field").value("question"))
                    .andExpect(jsonPath("$.errors[0].reason").value("NotBlank"))
                    .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
        }

        @Test
        void returnsInvalidRequestWhenBodyIsMalformedJson() throws Exception {
            mockMvc.perform(post("/stub/questions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid.request"))
                    .andExpect(jsonPath("$.detail").doesNotExist());
        }

        @Test
        void listsConstraintViolationsWhenMethodValidationFails() throws Exception {
            mockMvc.perform(get("/stub/constraint"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid.request"))
                    .andExpect(jsonPath("$.errors[0].field").value("question"))
                    .andExpect(jsonPath("$.errors[0].reason").value("Size"));
        }

        @Test
        void returnsUnsupportedMediaTypeWhenContentTypeIsWrong() throws Exception {
            mockMvc.perform(post("/stub/questions")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("question"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("error.unsupported.media.type"));
        }

        @Test
        void returnsMethodNotAllowedWhenVerbIsWrong() throws Exception {
            mockMvc.perform(post("/stub/conflict"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.code").value("error.method.not.allowed"))
                    .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
        }
    }

    @Nested
    class WhenSecurityExceptionThrown {

        @Test
        void returnsUnauthenticatedWhenAuthenticationFails() throws Exception {
            mockMvc.perform(get("/stub/unauthenticated"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("error.unauthenticated"));
        }

        @Test
        void returnsForbiddenWhenAccessDenied() throws Exception {
            mockMvc.perform(get("/stub/forbidden"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("error.forbidden"));
        }
    }

    @Nested
    class WhenUnexpectedExceptionThrown {

        @Test
        void returnsInternalWithoutLeakingExceptionMessage() throws Exception {
            mockMvc.perform(get("/stub/unexpected"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("error.internal"))
                    .andExpect(jsonPath("$.detail").doesNotExist())
                    .andExpect(jsonPath("$.errors").doesNotExist());
        }

        @Test
        void omitsRequestIdWhenFilterDidNotRun() throws Exception {
            MDC.clear();

            mockMvc.perform(get("/stub/unexpected"))
                    .andExpect(jsonPath("$.requestId").doesNotExist());
        }
    }

    enum StubErrorCode implements ErrorCode {
        LIMIT_REACHED;

        @Override
        public String code() {
            return "error.stub.limit.reached";
        }

        @Override
        public HttpStatus httpStatus() {
            return HttpStatus.UNPROCESSABLE_CONTENT;
        }
    }

    record AskQuestionRequest(@NotBlank String question) {
    }

    record SizedQuestion(@Size(max = 3) String question) {
    }

    @RestController
    static class StubController {

        @GetMapping("/stub/conflict")
        String conflict() {
            throw new ApiException(CommonErrorCode.CONFLICT, "secret debug message");
        }

        @GetMapping("/stub/module-error")
        String moduleError() {
            throw new ApiException(StubErrorCode.LIMIT_REACHED);
        }

        @GetMapping("/stub/upstream-timeout")
        String upstreamTimeout() {
            throw new ApiException(CommonErrorCode.UPSTREAM_TIMEOUT, "llm timed out", new IllegalStateException("io"));
        }

        @PostMapping("/stub/questions")
        String askQuestion(@Valid @RequestBody AskQuestionRequest request) {
            return request.question();
        }

        @GetMapping("/stub/constraint")
        String constraint() {
            throw new ConstraintViolationException(VALIDATOR.validate(new SizedQuestion("too long")));
        }

        @GetMapping("/stub/unauthenticated")
        String unauthenticated() {
            throw new BadCredentialsException("bad credentials");
        }

        @GetMapping("/stub/forbidden")
        String forbidden() {
            throw new AccessDeniedException("denied");
        }

        @GetMapping("/stub/unexpected")
        String unexpected() {
            throw new IllegalStateException("db password is hunter2");
        }
    }
}
