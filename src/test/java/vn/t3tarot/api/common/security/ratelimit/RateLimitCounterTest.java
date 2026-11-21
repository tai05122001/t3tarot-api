package vn.t3tarot.api.common.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RateLimitCounterTest {

    private static final int REDIS_PORT = 6379;
    private static final RateLimitProperties.Limit LIMIT = new RateLimitProperties.Limit(3, 60);

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(REDIS_PORT);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final RateLimitCounter counter = new RateLimitCounter(redisTemplate, meterRegistry);

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        connectionFactory.destroy();
    }

    private static String newKey() {
        return "rl:test:" + UUID.randomUUID();
    }

    @Test
    void allowsWhenRequestsAreWithinLimit() {
        String key = newKey();

        for (int i = 0; i < LIMIT.maxRequests(); i++) {
            assertThat(counter.consume(key, LIMIT).isAllowed()).isTrue();
        }
    }

    @Test
    void rejectsWithRetryAfterWhenLimitIsExceeded() {
        String key = newKey();
        IntStream.range(0, LIMIT.maxRequests()).forEach(i -> counter.consume(key, LIMIT));

        RateLimitDecision decision = counter.consume(key, LIMIT);

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.retryAfterSeconds()).isBetween(1L, (long) LIMIT.windowSeconds());
    }

    @Test
    void countsEachKeySeparately() {
        String firstKey = newKey();
        String secondKey = newKey();
        IntStream.range(0, LIMIT.maxRequests() + 1).forEach(i -> counter.consume(firstKey, LIMIT));

        assertThat(counter.consume(secondKey, LIMIT).isAllowed()).isTrue();
    }

    @Test
    void setsExpiryOnFirstRequest() {
        String key = newKey();

        counter.consume(key, LIMIT);

        assertThat(redisTemplate.getExpire(key)).isBetween(1L, (long) LIMIT.windowSeconds());
    }

    @Test
    void restoresExpiryWhenKeyHasNoTimeToLive() {
        String key = newKey();
        redisTemplate.opsForValue().set(key, "1");

        counter.consume(key, LIMIT);

        assertThat(redisTemplate.getExpire(key)).isBetween(1L, (long) LIMIT.windowSeconds());
    }

    @Test
    void allowsExactlyMaxRequestsWhenCalledConcurrently() throws InterruptedException, ExecutionException {
        String key = newKey();
        int totalRequests = 100;
        AtomicInteger allowedCount = new AtomicInteger();
        CountDownLatch startSignal = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = IntStream.range(0, totalRequests)
                    .mapToObj(i -> executor.submit(() -> {
                        startSignal.await();
                        if (counter.consume(key, LIMIT).isAllowed()) {
                            allowedCount.incrementAndGet();
                        }
                        return null;
                    }))
                    .toList();
            startSignal.countDown();
            for (Future<Object> future : futures) {
                future.get();
            }
        }

        assertThat(allowedCount.get()).isEqualTo(LIMIT.maxRequests());
    }

    @Test
    @SuppressWarnings("unchecked")
    void allowsAndCountsErrorWhenRedisIsUnavailable() {
        StringRedisTemplate failingTemplate = mock(StringRedisTemplate.class);
        doThrow(new RedisConnectionFailureException("connection refused"))
                .when(failingTemplate)
                .execute(any(RedisScript.class), anyList(), any(), any());
        RateLimitCounter failingCounter = new RateLimitCounter(failingTemplate, meterRegistry);

        RateLimitDecision decision = failingCounter.consume(newKey(), LIMIT);

        assertThat(decision.isAllowed()).isTrue();
        assertThat(meterRegistry.get("rate_limit_redis_error_total").counter().count())
                .isEqualTo(1.0);
    }
}
