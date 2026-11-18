package vn.t3tarot.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import vn.t3tarot.api.common.error.GlobalExceptionHandler;
import vn.t3tarot.api.common.security.TestJwtFactory.Claims;

@SpringJUnitWebConfig(SecurityConfigTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "t3tarot.security.jwt.issuer=" + TestJwtFactory.ISSUER,
            "t3tarot.security.jwt.audience=" + TestJwtFactory.AUDIENCE,
            "t3tarot.security.cors.allowed-origins=https://app.t3tarot.test",
            "t3tarot.security.public-paths=/stub/public",
            "t3tarot.security.swagger-public=false",
            "management.server.port=8081",
            "server.port=8080"
        })
class SecurityConfigTest {

    private static final String ALLOWED_ORIGIN = "https://app.t3tarot.test";
    private static final String BEARER = "Bearer ";

    @DynamicPropertySource
    static void jwtPublicKey(DynamicPropertyRegistry registry) {
        registry.add("t3tarot.security.jwt.public-key", TestJwtFactory::trustedPublicKeyBase64);
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static String validBearer() {
        return BEARER + TestJwtFactory.trustedToken(TestJwtFactory.validClaims());
    }

    @Nested
    class WhenNoCredentials {

        @Test
        void returns401ProblemDetailWithBearerChallengeOnProtectedPath() throws Exception {
            mockMvc.perform(get("/stub/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(jsonPath("$.code").value("error.auth.unauthenticated"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").doesNotExist())
                    .andExpect(jsonPath("$.title").doesNotExist())
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.messageCode").doesNotExist());
        }

        @Test
        void deniesUnknownPathsByDefault() throws Exception {
            mockMvc.perform(get("/does-not-exist")).andExpect(status().isUnauthorized());
        }

        @Test
        void deniesActuatorOnApiPort() throws Exception {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
        }

        @Test
        void deniesSwaggerWhenNotMarkedPublic() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
        }

        @Test
        void allowsConfiguredPublicPath() throws Exception {
            mockMvc.perform(get("/stub/public")).andExpect(status().isOk()).andExpect(content().string("open"));
        }
    }

    @Nested
    class WhenTokenIsValid {

        @Test
        void exposesUserIdAsPrincipal() throws Exception {
            mockMvc.perform(get("/stub/me").header("Authorization", validBearer()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(TestJwtFactory.USER_A));
        }

        @Test
        void acceptsWriteWithoutCsrfToken() throws Exception {
            mockMvc.perform(post("/stub/echo").header("Authorization", validBearer()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("done"));
        }

        @Test
        void doesNotCreateHttpSession() throws Exception {
            MvcResult result = mockMvc.perform(get("/stub/me").header("Authorization", validBearer()))
                    .andReturn();

            assertThat(result.getRequest().getSession(false)).isNull();
            assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
        }

        @Test
        void returns403ProblemDetailWhenMethodRequiresMissingRole() throws Exception {
            mockMvc.perform(get("/stub/admin").header("Authorization", validBearer()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("error.auth.forbidden"));
        }
    }

    @Nested
    class WhenTokenIsInvalid {

        @Test
        void rejectsExpiredToken() throws Exception {
            Claims expired = TestJwtFactory.validClaims().withLifetime(Duration.ofMinutes(-10));

            assertUnauthorized(BEARER + TestJwtFactory.trustedToken(expired));
        }

        @Test
        void rejectsWrongIssuer() throws Exception {
            Claims claims = TestJwtFactory.validClaims().withIssuer("https://evil.example");

            assertUnauthorized(BEARER + TestJwtFactory.trustedToken(claims));
        }

        @Test
        void rejectsWrongAudience() throws Exception {
            Claims claims = TestJwtFactory.validClaims().withAudience(List.of("another-api"));

            assertUnauthorized(BEARER + TestJwtFactory.trustedToken(claims));
        }

        @Test
        void rejectsSubjectThatIsNotUserId() throws Exception {
            Claims claims = TestJwtFactory.validClaims().withSubject("admin");

            assertUnauthorized(BEARER + TestJwtFactory.trustedToken(claims));
        }

        @Test
        void rejectsTokenSignedByUntrustedKey() throws Exception {
            assertUnauthorized(BEARER + TestJwtFactory.untrustedToken(TestJwtFactory.validClaims()));
        }

        @Test
        void rejectsUnsignedToken() throws Exception {
            assertUnauthorized(BEARER + TestJwtFactory.unsignedToken(TestJwtFactory.validClaims()));
        }

        @Test
        void rejectsBasicAuthorizationScheme() throws Exception {
            assertUnauthorized("Basic dXNlcjpwYXNz");
        }

        @Test
        void ignoresTokenSentAsQueryParameter() throws Exception {
            String token = TestJwtFactory.trustedToken(TestJwtFactory.validClaims());

            mockMvc.perform(get("/stub/me").param("access_token", token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("error.auth.unauthenticated"));
        }

        @Test
        void doesNotLeakTokenOrFailureReasonInBody() throws Exception {
            Claims expired = TestJwtFactory.validClaims().withLifetime(Duration.ofMinutes(-10));
            String token = TestJwtFactory.trustedToken(expired);

            MvcResult result = mockMvc.perform(get("/stub/me").header("Authorization", BEARER + token))
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).doesNotContain(token).doesNotContain("expired");
        }

        private void assertUnauthorized(String authorization) throws Exception {
            mockMvc.perform(get("/stub/me").header("Authorization", authorization))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(jsonPath("$.code").value("error.auth.unauthenticated"));
        }
    }

    @Nested
    class SecurityHeaders {

        @Test
        void sendsHardenedHeadersOnApiResponses() throws Exception {
            MvcResult result = mockMvc.perform(get("/stub/me").header("Authorization", validBearer()))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("Cache-Control", containsString("no-store")))
                    .andExpect(header().string("Content-Security-Policy", SecurityConfig.CONTENT_SECURITY_POLICY))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                    .andExpect(header().string("Permissions-Policy", SecurityConfig.PERMISSIONS_POLICY))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andReturn();

            assertThat(result.getResponse().containsHeader("X-XSS-Protection")).isFalse();
        }

        @Test
        void sendsHardenedHeadersOnErrorResponsesToo() throws Exception {
            mockMvc.perform(get("/stub/me"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("Cache-Control", containsString("no-store")));
        }

        @Test
        void sendsHstsOnlyOverSecureRequests() throws Exception {
            mockMvc.perform(get("/stub/me").secure(true).header("Authorization", validBearer()))
                    .andExpect(header().string(
                            "Strict-Transport-Security",
                            "max-age=" + SecurityConfig.HSTS_MAX_AGE_SECONDS + " ; includeSubDomains"));
            mockMvc.perform(get("/stub/me").header("Authorization", validBearer()))
                    .andExpect(header().doesNotExist("Strict-Transport-Security"));
        }
    }

    @Nested
    class Cors {

        @Test
        void answersPreflightFromAllowedOriginWithoutToken() throws Exception {
            mockMvc.perform(options("/stub/echo")
                            .header("Origin", ALLOWED_ORIGIN)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "authorization,idempotency-key"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        }

        @Test
        void rejectsPreflightFromOtherOrigin() throws Exception {
            mockMvc.perform(options("/stub/echo")
                            .header("Origin", "https://evil.example")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }

        @Test
        void exposesRequestIdHeaderToAllowedOrigin() throws Exception {
            mockMvc.perform(get("/stub/me").header("Origin", ALLOWED_ORIGIN).header("Authorization", validBearer()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                    .andExpect(header().string("Access-Control-Expose-Headers", containsString("X-Request-Id")));
        }
    }

    @Nested
    class ManagementPort {

        @Test
        void permitsActuatorOnlyOnSeparateManagementPort() throws Exception {
            mockMvc.perform(get("/actuator/health").with(request -> {
                        request.setLocalPort(8081);
                        return request;
                    }))
                    .andExpect(status().isNotFound());
        }

        @Test
        void keepsApiPortProtectedWhenManagementPortIsSeparate() throws Exception {
            mockMvc.perform(get("/stub/me").with(request -> {
                        request.setLocalPort(8080);
                        return request;
                    }))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Configuration
    @EnableWebMvc
    @Import({
        SecurityConfig.class,
        JwtDecoderConfig.class,
        ProblemDetailSecurityHandler.class,
        AccessTokenAuthenticationConverter.class,
        GlobalExceptionHandler.class,
        StubController.class
    })
    static class TestApplication {
    }

    @RestController
    public static class StubController {

        @GetMapping("/stub/me")
        public String me(@AuthenticationPrincipal AuthenticatedUser user) {
            return user.userId().toString();
        }

        @GetMapping("/stub/public")
        public String publicEndpoint() {
            return "open";
        }

        @PostMapping("/stub/echo")
        public String echo() {
            return "done";
        }

        @GetMapping("/stub/admin")
        @PreAuthorize("hasRole('ADMIN')")
        public String admin() {
            return "admin";
        }
    }
}
