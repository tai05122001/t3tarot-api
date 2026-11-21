package vn.t3tarot.api.common.security.ratelimit;

public record RateLimitDecision(boolean isAllowed, long retryAfterSeconds) {

    private static final RateLimitDecision ALLOWED = new RateLimitDecision(true, 0);

    public static RateLimitDecision allowed() {
        return ALLOWED;
    }

    public static RateLimitDecision rejected(long retryAfterSeconds) {
        return new RateLimitDecision(false, retryAfterSeconds);
    }
}
