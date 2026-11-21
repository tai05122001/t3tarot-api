package vn.t3tarot.api.identity.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import vn.t3tarot.api.identity.IdentityIntegrationTest;

class RefreshSessionConcurrencyTest extends IdentityIntegrationTest {

    private static final int PARALLEL_REQUESTS = 6;

    @Test
    void leavesExactlyOneCurrentTokenWhenSameTokenIsRefreshedInParallel() throws Exception {
        Account account = registerVerifiedAccount();
        Tokens tokens = loginTokens(account);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_REQUESTS);
        List<Future<MvcResult>> results = new ArrayList<>();
        try {
            for (int index = 0; index < PARALLEL_REQUESTS; index++) {
                Callable<MvcResult> call = () -> {
                    start.await();
                    return refreshWith(tokens.refreshToken()).andReturn();
                };
                results.add(executor.submit(call));
            }
            start.countDown();
            List<String> issuedRefreshTokens = new ArrayList<>();
            for (Future<MvcResult> future : results) {
                MvcResult result = future.get();
                assertThat(result.getResponse().getStatus()).isEqualTo(200);
                issuedRefreshTokens.add(JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken"));
            }

            byte[] currentHash = scalar(
                    byte[].class, "SELECT refresh_token_hash FROM auth_session WHERE user_id = ?", userIdOf(account));
            long currentTokenCount = issuedRefreshTokens.stream()
                    .filter(token -> Arrays.equals(opaqueTokens.hash(token), currentHash))
                    .count();
            assertThat(currentTokenCount).isEqualTo(1);
            assertThat(scalar(
                            Boolean.class,
                            "SELECT revoked_at IS NULL FROM auth_session WHERE user_id = ?",
                            userIdOf(account)))
                    .isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void stillServesTheNewestTokenAfterParallelRefresh() throws Exception {
        Account account = registerVerifiedAccount();
        Tokens tokens = loginTokens(account);
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_REQUESTS);
        try {
            List<Future<MvcResult>> results = new ArrayList<>();
            for (int index = 0; index < PARALLEL_REQUESTS; index++) {
                results.add(executor.submit(() -> refreshWith(tokens.refreshToken()).andReturn()));
            }
            for (Future<MvcResult> future : results) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        loginAs(account).andExpect(status().isOk());
    }
}
