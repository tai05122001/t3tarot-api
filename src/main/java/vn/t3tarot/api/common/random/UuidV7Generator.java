package vn.t3tarot.api.common.random;

import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.t3tarot.api.common.time.VietnamClock;

@Component
@RequiredArgsConstructor
public class UuidV7Generator {

    private static final long TIMESTAMP_MASK = 0xFFFFFFFFFFFFL;
    private static final long VERSION_7 = 0x7000L;
    private static final long VARIANT = 0x8000000000000000L;
    private static final long LOW_62_BITS = 0x3FFFFFFFFFFFFFFFL;

    private final VietnamClock clock;
    private final SecureRandom secureRandom;

    public UUID next() {
        long millis = clock.instant().toEpochMilli();
        byte[] random = new byte[10];
        secureRandom.nextBytes(random);
        long randomHigh = ((random[0] & 0x0FL) << 8) | (random[1] & 0xFFL);
        long mostSignificantBits = ((millis & TIMESTAMP_MASK) << 16) | VERSION_7 | randomHigh;
        long randomLow = 0;
        for (int index = 2; index < random.length; index++) {
            randomLow = (randomLow << 8) | (random[index] & 0xFFL);
        }
        long leastSignificantBits = VARIANT | (randomLow & LOW_62_BITS);
        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
