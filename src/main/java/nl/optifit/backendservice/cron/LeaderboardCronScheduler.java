package nl.optifit.backendservice.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.service.LeaderboardService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class LeaderboardCronScheduler {

    private final LeaderboardService leaderboardService;
    private final LeaderboardSchedule schedule;

    private static final List<String> REGIONS = List.of("Europe/Amsterdam", "Australia/Sydney");

    @Scheduled(cron = "0 */15 * * * *") // run every 15 minutes globally
    public void run() {
        REGIONS.forEach(this::processRegion);
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
        if (!date.getDayOfWeek().equals(schedule.resetDayOfWeek())) return false;

        // “5#1” means the *first* Friday, so week = 1
        int weekOfMonth = date.get(ChronoField.ALIGNED_WEEK_OF_MONTH);
        if (weekOfMonth != schedule.resetWeek()) return false;

        return now.equals(schedule.resetTime());
    }
}
