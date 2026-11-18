package vn.t3tarot.api.common.time;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VietnamClock {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Clock clock;

    public Instant instant() {
        return clock.instant();
    }

    public ZonedDateTime now() {
        return clock.instant().atZone(ZONE);
    }

    public LocalDate today() {
        return now().toLocalDate();
    }

    public LocalDate startOfCurrentWeek() {
        return today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
