package vn.t3tarot.api.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.t3tarot.api.common.constant.CommonErrorCode;
import vn.t3tarot.api.common.constant.ErrorCode;
import vn.t3tarot.api.common.constant.ErrorResponseKeys;

class GlobalExceptionHandlerTest {

    private static final String REQUEST_ID = "req-test-0001";
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        MDC.put(ErrorResponseKeys.REQUEST_ID_MDC_KEY, REQUEST_ID);
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
                    .andExpect(jsonPath("$.code").value("error.resource.conflict"))
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
                    .andExpect(jsonPath("$.code").value("error.stub.limit_reached"));
        }

        @Test
        void returnsInternalCodeWhenErrorCodeIsServerSide() throws Exception {
            mockMvc.perform(get("/stub/upstream-timeout"))
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code").value("error.server.upstream_timeout"));
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
                    .andExpect(jsonPath("$.code").value("error.invalid_param.is_invalid"))
                    .andExpect(jsonPath("$.errors[0].field").value("question"))
                    .andExpect(jsonPath("$.errors[0].code").value("error.invalid_param.is_missing"))
                    .andExpect(jsonPath("$.errors[0].reason").doesNotExist())
                    .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
        }

        @Test
        void returnsInvalidRequestWhenBodyIsMalformedJson() throws Exception {
            mockMvc.perform(post("/stub/questions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid_request.malformed"))
                    .andExpect(jsonPath("$.detail").doesNotExist());
        }

        @Test
        void listsConstraintViolationsWhenMethodValidationFails() throws Exception {
            mockMvc.perform(get("/stub/constraint"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid_param.is_invalid"))
                    .andExpect(jsonPath("$.errors[0].field").value("question"))
                    .andExpect(jsonPath("$.errors[0].code").value("error.invalid_param.length_out_of_range"));
        }

        @Test
        void reportsMissingQueryParameterByName() throws Exception {
            mockMvc.perform(get("/stub/search"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid_param.is_invalid"))
                    .andExpect(jsonPath("$.errors[0].field").value("page"))
                    .andExpect(jsonPath("$.errors[0].code").value("error.invalid_param.is_missing"));
        }

        @Test
        void reportsQueryParameterWithWrongType() throws Exception {
            mockMvc.perform(get("/stub/search").param("page", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid_param.is_invalid"))
                    .andExpect(jsonPath("$.errors[0].field").value("page"))
                    .andExpect(jsonPath("$.errors[0].code").value("error.invalid_param.invalid_type"));
        }

        @Test
        void returnsUnsupportedMediaTypeWhenContentTypeIsWrong() throws Exception {
            mockMvc.perform(post("/stub/questions")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("question"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("error.http.unsupported_media_type"));
        }

        @Test
        void returnsMethodNotAllowedWhenVerbIsWrong() throws Exception {
            mockMvc.perform(post("/stub/conflict"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.code").value("error.http.method_not_allowed"))
                    .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
        }
    }

    @Nested
    class WhenControllerParameterViolatesConstraint {

        @Test
        void usesRequestParamNameAsFieldAndConstraintAsCode() throws Exception {
            mockMvc.perform(get("/stub/validated-search").param("q", "too long").param("page", "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid_param.is_invalid"))
                    .andExpect(jsonPath("$.errors.length()").value(1))
                    .andExpect(jsonPath("$.errors[0].field").value("q"))
                    .andExpect(jsonPath("$.errors[0].code").value("error.invalid_param.length_out_of_range"));
        }

        @Test
        void listsEveryViolatedParameter() throws Exception {
            mockMvc.perform(get("/stub/validated-search").param("q", "too long").param("page", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.length()").value(2))
                    .andExpect(jsonPath("$.errors[?(@.field=='q')].code")
                            .value("error.invalid_param.length_out_of_range"))
                    .andExpect(jsonPath("$.errors[?(@.field=='page')].code")
                            .value("error.invalid_param.out_of_range"));
        }

        @Test
        void usesPathVariableNameAsField() throws Exception {
            mockMvc.perform(get("/stub/items/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("itemId"))
                    .andExpect(jsonPath("$.errors[0].code").value("error.invalid_param.out_of_range"));
        }

        @Test
        void usesHeaderNameAsField() throws Exception {
            mockMvc.perform(get("/stub/with-header").header("X-Page-Size", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("X-Page-Size"))
                    .andExpect(jsonPath("$.errors[0].code").value("error.invalid_param.out_of_range"));
        }

        @Test
        void usesJavaParameterNameWhenNoRequestNameGiven() throws Exception {
            mockMvc.perform(get("/stub/unnamed").param("limit", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("limit"))
                    .andExpect(jsonPath("$.errors[0].code").value("error.invalid_param.out_of_range"));
        }

        @Test
        void listsBodyFieldViolationsWhenMethodAlsoHasConstrainedParameter() throws Exception {
            mockMvc.perform(post("/stub/validated-body")
                            .param("page", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("error.invalid_param.is_invalid"))
                    .andExpect(jsonPath("$.errors[0].field").value("question"))
                    .andExpect(jsonPath("$.errors[0].code").value("error.invalid_param.is_missing"));
        }

        @Test
        void neverEchoesRejectedValueOrMessage() throws Exception {
            mockMvc.perform(get("/stub/validated-search").param("q", "secret value").param("page", "1"))
                    .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist())
                    .andExpect(jsonPath("$.errors[0].message").doesNotExist())
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.messageCode").doesNotExist())
                    .andExpect(jsonPath("$.title").doesNotExist())
                    .andExpect(jsonPath("$.detail").doesNotExist());
        }
    }

    @Nested
    class ResponseNeverCarriesMessageFields {

        @ParameterizedTest
        @ValueSource(strings = {
                "/stub/conflict",
                "/stub/unexpected",
                "/stub/unauthenticated",
                "/stub/forbidden",
                "/stub/constraint",
                "/stub/search"
        })
        void returnsOnlyCodeBasedBody(String path) throws Exception {
            mockMvc.perform(get(path))
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.messageCode").doesNotExist())
                    .andExpect(jsonPath("$.title").doesNotExist())
                    .andExpect(jsonPath("$.detail").doesNotExist());
        }

        @Test
        void returnsOnlyCodeBasedBodyForValidationOfRequestBody() throws Exception {
            mockMvc.perform(post("/stub/questions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"\"}"))
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.messageCode").doesNotExist())
                    .andExpect(jsonPath("$.title").doesNotExist())
                    .andExpect(jsonPath("$.detail").doesNotExist())
                    .andExpect(jsonPath("$.errors[0].message").doesNotExist());
        }

        @Test
        void usesFormatAcceptedByErrorCodeFormatForEveryReturnedCode() throws Exception {
            String body = mockMvc.perform(get("/stub/constraint")).andReturn().getResponse().getContentAsString();

            assertThat(ErrorCodeFormat.isValid(JsonPath.read(body, "$.code"))).isTrue();
            assertThat(ErrorCodeFormat.isValid(JsonPath.read(body, "$.errors[0].code"))).isTrue();
        }
    }

    @Nested
    class WhenSecurityExceptionThrown {

        @Test
        void returnsUnauthenticatedWhenAuthenticationFails() throws Exception {
            mockMvc.perform(get("/stub/unauthenticated"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("error.auth.unauthenticated"));
        }

        @Test
        void returnsForbiddenWhenAccessDenied() throws Exception {
            mockMvc.perform(get("/stub/forbidden"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("error.auth.forbidden"));
        }
    }

    @Nested
    class WhenUnexpectedExceptionThrown {

        @Test
        void returnsInternalWithoutLeakingExceptionMessage() throws Exception {
            mockMvc.perform(get("/stub/unexpected"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("error.server.internal"))
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
            return "error.stub.limit_reached";
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

        @GetMapping("/stub/validated-search")
        String validatedSearch(@RequestParam("q") @Size(max = 3) String query, @RequestParam @Min(1) int page) {
            return query + page;
        }

        @GetMapping("/stub/items/{itemId}")
        String item(@PathVariable("itemId") @Min(1) int id) {
            return String.valueOf(id);
        }

        @GetMapping("/stub/with-header")
        String withHeader(@RequestHeader("X-Page-Size") @Min(1) int size) {
            return String.valueOf(size);
        }

        @GetMapping("/stub/unnamed")
        String unnamed(@RequestParam @Min(1) int limit) {
            return String.valueOf(limit);
        }

        @PostMapping("/stub/validated-body")
        String validatedBody(@Valid @RequestBody AskQuestionRequest request, @RequestParam @Min(1) int page) {
            return request.question() + page;
        }

        @GetMapping("/stub/search")
        String search(@RequestParam int page) {
            return String.valueOf(page);
        }

        @GetMapping("/stub/unexpected")
        String unexpected() {
            throw new IllegalStateException("db password is hunter2");
        }
    }
}
