package nl.optifit.backendservice.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.ExerciseType;
import nl.optifit.backendservice.model.Session;
import nl.optifit.backendservice.model.SessionStatus;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.LeaderboardService;
import nl.optifit.backendservice.service.NotificationService;
import nl.optifit.backendservice.service.SessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static nl.optifit.backendservice.model.ExerciseType.BACK;
import static nl.optifit.backendservice.model.ExerciseType.HIP;
import static nl.optifit.backendservice.model.ExerciseType.SHOULDER;

@Slf4j
@RequiredArgsConstructor
@Component
public class CronScheduler {

    private final AccountService accountService;
    private final SessionService sessionService;
    private final LeaderboardService leaderboardService;
    private final NotificationService notificationService;

    /**
     * SESSIONS
     */

    // 0 42 13 * * ? For testing purposes
    @Scheduled(cron = "#{@cronProperties.sessions.morning.create}", zone = "Europe/Amsterdam")
    public void createMorningSession() {
        createSessionsForAllAccounts(HIP);
    }

    @Scheduled(cron = "#{@cronProperties.sessions.lunch.create}", zone = "Europe/Amsterdam")
    public void createLunchSession() {
        createSessionsForAllAccounts(SHOULDER);
    }

    @Scheduled(cron = "#{@cronProperties.sessions.afternoon.create}", zone = "Europe/Amsterdam")
    public void createAfternoonSession() {createSessionsForAllAccounts(BACK); }

    @Scheduled(cron = "#{@cronProperties.sessions.morning.update}", zone = "Europe/Amsterdam")
    public void updateMorningSession() {
        updateSessionStatusForAllAccounts();
    }

    @Scheduled(cron = "#{@cronProperties.sessions.lunch.update}", zone = "Europe/Amsterdam")
    public void updateLunchSession() {
        updateSessionStatusForAllAccounts();
    }

    @Scheduled(cron = "#{@cronProperties.sessions.afternoon.update}", zone = "Europe/Amsterdam")
    public void updateAfternoonSession() {
        updateSessionStatusForAllAccounts();
    }

    /**
     * LEADERBOARD
     */

    @Scheduled(cron = "#{@cronProperties.leaderboard.reset}", zone = "Europe/Amsterdam")
    public void clearLeaderboard() {
        leaderboardService.resetLeaderboard();
    }

    private void createSessionsForAllAccounts(ExerciseType exerciseType) {
        accountService.findAllAccounts()
                .parallelStream()
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
