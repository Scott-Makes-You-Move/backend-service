package nl.optifit.backendservice.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.service.LeaderboardService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class LeaderboardCronScheduler {

    private final LeaderboardService leaderboardService;
    private final LeaderboardSchedule leaderboardSchedule;

    private static final List<String> TIME_ZONES = List.of("Europe/Amsterdam", "Australia/Sydney");

    @Scheduled(cron = "0 0 * * * *")
    public void run() {
        TIME_ZONES.forEach(this::processRegion);
    }

    private void processRegion(String zoneId) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(zoneId));
        LocalDate today = now.toLocalDate();
        LocalTime localTime = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);

        if (shouldReset(today, localTime)) {
            log.info("Resetting leaderboard for region {} ({} {})", zoneId, today, localTime);
            leaderboardService.resetLeaderboard(zoneId);
        }
    }

    private boolean shouldReset(LocalDate date, LocalTime now) {
        if (!date.getDayOfWeek().equals(leaderboardSchedule.getResetDayOfWeek())) return false;

        int weekOfMonth = date.get(ChronoField.ALIGNED_WEEK_OF_MONTH);
        if (weekOfMonth != leaderboardSchedule.getResetWeek()) return false;

        return now.equals(leaderboardSchedule.getResetTime());
    }
}
