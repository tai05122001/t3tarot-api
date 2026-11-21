package vn.t3tarot.api.common.security.ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RateLimitCounter {

    private static final String FIXED_WINDOW_SCRIPT =
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('TTL', KEYS[1])
            if ttl < 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
                ttl = tonumber(ARGV[1])
            end
            if current > tonumber(ARGV[2]) then
                return math.max(ttl, 1)
            end
            return 0
            """;

    private static final DefaultRedisScript<Long> FIXED_WINDOW =
            new DefaultRedisScript<>(FIXED_WINDOW_SCRIPT, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Counter redisErrorCounter;

    public RateLimitCounter(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.redisErrorCounter = Counter.builder("rate_limit_redis_error_total")
                .description("Rate limit checks skipped because Redis was unavailable")
                .register(meterRegistry);
    }

    public RateLimitDecision consume(String key, RateLimitProperties.Limit limit) {
        try {
            long retryAfterSeconds = redisTemplate.execute(
                    FIXED_WINDOW,
                    List.of(key),
                    String.valueOf(limit.windowSeconds()),
                    String.valueOf(limit.maxRequests()));
            if (retryAfterSeconds <= 0) {
                return RateLimitDecision.allowed();
            }
            return RateLimitDecision.rejected(retryAfterSeconds);
        } catch (DataAccessException ex) {
            redisErrorCounter.increment();
            log.warn("rate_limit_redis_unavailable errorType={}", ex.getClass().getSimpleName());
            return RateLimitDecision.allowed();
        }
    }
}
