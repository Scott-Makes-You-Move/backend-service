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

    private final LeaderboardService leaderboardService;

    @Scheduled(cron = "${cron.leaderboard.reset}", zone = "UTC")
    public void run() {
        log.info("Resetting leaderboards");
        leaderboardService.resetLeaderboard();
    }
}
