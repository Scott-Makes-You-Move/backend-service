package nl.optifit.backendservice.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.service.LeaderboardService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class LeaderboardCronScheduler {

    public static final String EUROPE_AMSTERDAM = "Europe/Amsterdam";

    private final LeaderboardService leaderboardService;

    @Scheduled(cron = "#{@cronProperties.leaderboard.reset}", zone = EUROPE_AMSTERDAM)
    public void resetLeaderboard() {
        leaderboardService.resetLeaderboard();
    }
}
