package nl.optifit.backendservice.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.ExerciseType;
import nl.optifit.backendservice.model.Session;
import nl.optifit.backendservice.model.SessionStatus;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.SessionService;
import org.keycloak.admin.client.resource.UserResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
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
        log.info("Creating '{}' session for all accounts", exerciseType.getDisplayName());
        long startSyncTime = System.nanoTime();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            accountService.findAllAccounts()
                    .forEach(account -> executor.submit(() -> sessionService.createSessionForAccount(account, exerciseType)));
        }

        log.info("Finished creating sessions in {} ms", (System.nanoTime() - startSyncTime) / 1000000);
    }

    private void updateSessionStatusForAllAccounts() {
        log.info("Updating sessions with status NEW for all accounts");
        long startSyncTime = System.nanoTime();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            sessionService.getByStatus(SessionStatus.NEW)
                    .stream()
                    .map(session -> session.getId().toString())
                    .forEach(uuidString -> executor.submit(() -> sessionService.updateSessionForAccount(uuidString)));
        }
        log.info("Finished updating sessions in {} ms", (System.nanoTime() - startSyncTime) / 1000000);
    }
}
