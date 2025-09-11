package nl.optifit.backendservice.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.ExerciseType;
import nl.optifit.backendservice.model.Session;
import nl.optifit.backendservice.model.SessionStatus;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.LeaderboardService;
import nl.optifit.backendservice.service.SessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static nl.optifit.backendservice.model.ExerciseType.BACK;
import static nl.optifit.backendservice.model.ExerciseType.HIP;
import static nl.optifit.backendservice.model.ExerciseType.SHOULDER;

@Slf4j
@RequiredArgsConstructor
@Component
public class SessionCronScheduler {

    public static final String EUROPE_AMSTERDAM = "Europe/Amsterdam";

    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    private final AccountService accountService;
    private final SessionService sessionService;

    // 0 42 13 * * ? For testing purposes
    @Scheduled(cron = "#{@cronProperties.sessions.morning.create}", zone = EUROPE_AMSTERDAM)
    public void createMorningSession() {
        createSessionsForAllAccounts(HIP);
    }

    @Scheduled(cron = "#{@cronProperties.sessions.lunch.create}", zone = EUROPE_AMSTERDAM)
    public void createLunchSession() {
        createSessionsForAllAccounts(SHOULDER);
    }

    @Scheduled(cron = "#{@cronProperties.sessions.afternoon.create}", zone = EUROPE_AMSTERDAM)
    public void createAfternoonSession() {
        createSessionsForAllAccounts(BACK);
    }

    @Scheduled(cron = "#{@cronProperties.sessions.morning.update}", zone = EUROPE_AMSTERDAM)
    public void updateMorningSession() {
        updateSessionStatusForAllAccounts();
    }

    @Scheduled(cron = "#{@cronProperties.sessions.lunch.update}", zone = EUROPE_AMSTERDAM)
    public void updateLunchSession() {
        updateSessionStatusForAllAccounts();
    }

    @Scheduled(cron = "#{@cronProperties.sessions.afternoon.update}", zone = EUROPE_AMSTERDAM)
    public void updateAfternoonSession() {
        updateSessionStatusForAllAccounts();
    }

    private void createSessionsForAllAccounts(ExerciseType exerciseType) {
        accountService.findAllAccounts().forEach(account ->
                CompletableFuture.runAsync(() ->
                        sessionService.createSessionForAccount(account, exerciseType), executor
                )
        );
    }

    private void updateSessionStatusForAllAccounts() {
        sessionService.getByStatus(SessionStatus.NEW)
                .stream()
                .map(Session::getId)
                .map(UUID::toString)
                .forEach(sessionService::updateSessionForAccount);
    }
}
