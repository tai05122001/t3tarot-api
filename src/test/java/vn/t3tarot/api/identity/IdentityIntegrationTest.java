package vn.t3tarot.api.identity;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import vn.t3tarot.api.TestcontainersConfiguration;
import vn.t3tarot.api.common.idempotency.IdempotencyKey;
import vn.t3tarot.api.common.security.TestJwtFactory;
import vn.t3tarot.api.identity.shared.service.OpaqueTokens;
import vn.t3tarot.api.identity.sociallogin.client.GoogleIdTokenVerifier;

@SpringBootTest(
        properties = {
            "t3tarot.security.jwt.issuer=" + TestJwtFactory.ISSUER,
            "t3tarot.security.jwt.audience=" + TestJwtFactory.AUDIENCE,
            "t3tarot.identity.key-id=" + IdentityTestProperties.KEY_ID,
            "t3tarot.identity.password-hash-strength=4",
            "t3tarot.identity.google.client-ids=" + GoogleTokenFactory.CLIENT_ID,
            "t3tarot.security.rate-limit.enabled=false",
            "t3tarot.verification-mail.enabled=false"
        })
@Import({TestcontainersConfiguration.class, IdentityIntegrationTest.TestBeans.class})
public abstract class IdentityIntegrationTest {

    protected static final String PASSWORD = "correct-horse-battery";
    protected static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @DynamicPropertySource
    static void keys(DynamicPropertyRegistry registry) {
        registry.add("t3tarot.security.jwt.public-key", TestJwtFactory::trustedPublicKeyBase64);
        registry.add("t3tarot.identity.private-key", TestJwtFactory::trustedPrivateKeyBase64);
        registry.add("t3tarot.security.jwt.public-key-location", () -> "");
        registry.add("t3tarot.identity.private-key-location", () -> "");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        @Primary
        CapturingVerificationMailSender capturingVerificationMailSender() {
            return new CapturingVerificationMailSender();
        }

        @Bean
        @Primary
        GoogleIdTokenVerifier testGoogleIdTokenVerifier() {
            return new GoogleIdTokenVerifier(GoogleTokenFactory.decoder());
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    protected JdbcClient jdbcClient;

    @Autowired
    protected CapturingVerificationMailSender mailSender;

    @Autowired
    protected OpaqueTokens opaqueTokens;

    protected MockMvc mockMvc;

    @BeforeEach
    protected void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        mailSender.failWhileSending(false);
    }

    public record Account(String username, String email, String password) {
    }

    public record Tokens(String accessToken, String refreshToken) {
    }

    protected static Account newAccount() {
        String suffix = Long.toHexString(System.nanoTime()) + SEQUENCE.incrementAndGet();
        return new Account("user" + suffix, "user" + suffix + "@example.test", PASSWORD);
    }

    protected static String registerJson(Account account) {
        return json("username", account.username(), "email", account.email(), "password", account.password());
    }

    protected static String json(String... keyValues) {
        StringBuilder body = new StringBuilder("{");
        for (int index = 0; index < keyValues.length; index += 2) {
            if (index > 0) {
                body.append(',');
            }
            body.append('"').append(keyValues[index]).append("\":\"").append(keyValues[index + 1]).append('"');
        }
        return body.append('}').toString();
    }

    protected static MockHttpServletRequestBuilder postJson(String path, String body) {
        return post(path).contentType(JSON).content(body);
    }

    protected static MockHttpServletRequestBuilder postJsonWithKey(String path, String body) {
        return postJson(path, body).header(IdempotencyKey.HEADER_NAME, UUID.randomUUID().toString());
    }

    protected ResultActions register(Account account) throws Exception {
        return mockMvc.perform(postJsonWithKey("/auth/register", registerJson(account)));
    }

    protected void verifyEmailOf(Account account) throws Exception {
        String token = mailSender.lastTokenOf(account.email()).orElseThrow();
        mockMvc.perform(postJson("/auth/email/verify", json("token", token)))
                .andReturn();
    }

    protected Account registerVerifiedAccount() throws Exception {
        Account account = newAccount();
        register(account).andReturn();
        verifyEmailOf(account);
        return account;
    }

    protected ResultActions loginAs(Account account) throws Exception {
        return mockMvc.perform(postJson("/auth/login", json("username", account.username(), "password", account.password())));
    }

    protected Tokens loginTokens(Account account) throws Exception {
        String body = loginAs(account).andReturn().getResponse().getContentAsString();
        return new Tokens(JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }

    protected Tokens registerVerifiedAndLogin() throws Exception {
        return loginTokens(registerVerifiedAccount());
    }

    protected ResultActions refreshWith(String refreshToken) throws Exception {
        return mockMvc.perform(postJson("/auth/refresh", json("refreshToken", refreshToken)));
    }

    protected static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    protected static List<String> rolesOf(String accessToken) {
        String payload = new String(Base64.getUrlDecoder().decode(accessToken.split("\\.")[1]), StandardCharsets.UTF_8);
        return JsonPath.read(payload, "$.roles");
    }

    protected UUID userIdOf(Account account) {
        return jdbcClient.sql("SELECT user_id FROM password_credential WHERE username_normalized = :username")
                .param("username", account.username().toLowerCase(Locale.ROOT))
                .query(UUID.class)
                .single();
    }

    protected <T> T scalar(Class<T> type, String sql, Object... params) {
        return jdbcClient.sql(sql).params(params).query(type).single();
    }

    protected int execute(String sql, Object... params) {
        return jdbcClient.sql(sql).params(params).update();
    }
}
