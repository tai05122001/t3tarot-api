package vn.t3tarot.api.common.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.t3tarot.api.common.time.VietnamClock;

class UuidV7GeneratorTest {

    private static final Instant MOMENT = Instant.parse("2026-11-19T01:02:03.456Z");

    private static UuidV7Generator generatorAt(Instant instant) {
        return new UuidV7Generator(new VietnamClock(Clock.fixed(instant, ZoneOffset.UTC)), new SecureRandom());
    }

    @Test
    void setsVersionSevenAndRfcVariant() {
        UUID uuid = generatorAt(MOMENT).next();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void encodesCurrentMillisecondsInFirstFortyEightBits() {
        UUID uuid = generatorAt(MOMENT).next();

        assertThat(uuid.getMostSignificantBits() >>> 16).isEqualTo(MOMENT.toEpochMilli());
    }

    @Test
    void sortsByCreationTimeAsString() {
        UUID earlier = generatorAt(MOMENT).next();
        UUID later = generatorAt(MOMENT.plusMillis(1)).next();

        assertThat(later.toString()).isGreaterThan(earlier.toString());
    }

    @Test
    void producesDistinctValuesWithinSameMillisecond() {
        UuidV7Generator generator = generatorAt(MOMENT);
        Set<UUID> values = new HashSet<>();

        for (int index = 0; index < 1000; index++) {
            values.add(generator.next());
        }

        assertThat(values).hasSize(1000);
    }
}
