package vn.t3tarot.api.identity.emailverification.policy;

import java.time.Duration;
import org.springframework.util.StringUtils;

public final class VerificationTokenPolicy {

    public static final int MAX_LENGTH = 128;

    private static final long MINUTES_PER_HOUR = 60;

    private VerificationTokenPolicy() {
    }

    public static boolean isWellFormed(String token) {
        return StringUtils.hasText(token) && token.length() <= MAX_LENGTH;
    }

    public static String describeLifetime(Duration lifetime) {
        long minutes = lifetime.toMinutes();
        if (minutes >= MINUTES_PER_HOUR && minutes % MINUTES_PER_HOUR == 0) {
            return minutes / MINUTES_PER_HOUR + " giờ";
        }
        return minutes + " phút";
    }
}
