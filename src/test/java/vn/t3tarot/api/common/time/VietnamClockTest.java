package vn.t3tarot.api.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class VietnamClockTest {

    private static VietnamClock at(String utcInstant) {
        return new VietnamClock(Clock.fixed(Instant.parse(utcInstant), ZoneOffset.UTC));
    }

    @Test
    void todayIsStillCurrentDayOneSecondBeforeVietnamMidnight() {
        assertThat(at("2026-11-18T16:59:59Z").today()).isEqualTo(LocalDate.of(2026, 11, 18));
    }

    @Test
    void todayRollsOverAtVietnamMidnight() {
        assertThat(at("2026-11-18T17:00:00Z").today()).isEqualTo(LocalDate.of(2026, 11, 19));
    }

    @Test
    void todayDiffersFromUtcDateInTheEarlyVietnamMorning() {
        assertThat(at("2026-11-18T18:30:00Z").today()).isEqualTo(LocalDate.of(2026, 11, 19));
    }

    @Test
    void startOfCurrentWeekIsMondayInVietnam() {
        assertThat(at("2026-11-18T05:00:00Z").startOfCurrentWeek()).isEqualTo(LocalDate.of(2026, 11, 16));
    }

    @Test
    void startOfCurrentWeekUsesVietnamDateAtSundayNight() {
        assertThat(at("2026-11-22T17:00:00Z").startOfCurrentWeek()).isEqualTo(LocalDate.of(2026, 11, 23));
    }
}
