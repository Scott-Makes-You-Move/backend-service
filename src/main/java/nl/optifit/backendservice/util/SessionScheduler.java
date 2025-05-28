package nl.optifit.backendservice.util;

import lombok.*;
import lombok.extern.slf4j.*;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.service.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;

import java.util.*;

import static nl.optifit.backendservice.model.ExerciseType.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class SessionScheduler {

    private final AccountService accountService;
    private final SessionService sessionService;
    private final LeaderboardService leaderboardService;

    /**
     * Session Scheduler
     */

    // 0 42 13 * * ? For testing purposes
    @Scheduled(cron = "0 0 10 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void createMorningSession() {
        createSessionsForAllAccounts(HIP);
    }

    @Scheduled(cron = "0 30 13 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void createLunchSession() {
        createSessionsForAllAccounts(SHOULDER);
    }

    @Scheduled(cron = "0 0 15 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void createAfternoonSession() {
        createSessionsForAllAccounts(BACK);
    }

    @Scheduled(cron = "0 0 11 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void updateMorningSession() {
        updateSessionStatusForAllAccounts();
    }

    @Scheduled(cron = "0 30 14 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void updateLunchSession() {
        updateSessionStatusForAllAccounts();
    }

    @Scheduled(cron = "0 0 16 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void updateAfternoonSession() {
        updateSessionStatusForAllAccounts();
    }

    /**
     * Leaderboard Reset Scheduler
     */
    @Scheduled(cron = "0 0 17 ? * 5#1", zone = "Europe/Amsterdam")
    public void clearLeaderboard() {
        leaderboardService.resetLeaderboard();
    }

    private void createSessionsForAllAccounts(ExerciseType exerciseType) {
        accountService.findAllAccounts()
                .forEach(account -> sessionService.createSessionForAccount(account, exerciseType));
    }

    private void updateSessionStatusForAllAccounts() {
        sessionService.getByStatus(SessionStatus.NEW)
                .stream()
                .map(Session::getId)
                .map(UUID::toString)
                .forEach(sessionService::updateSessionForAccount);
    }
}
