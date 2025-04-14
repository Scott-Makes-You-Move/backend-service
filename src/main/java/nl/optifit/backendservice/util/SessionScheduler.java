package nl.optifit.backendservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.SessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static nl.optifit.backendservice.model.ExerciseType.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class SessionScheduler {

    private final AccountService accountService;
    private final SessionService sessionService;

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

    private void createSessionsForAllAccounts(ExerciseType exerciseType) {
        accountService.findAllAccounts()
                .forEach(account -> accountService.createSessionForAccount(account, exerciseType));
    }

    private void updateSessionStatusForAllAccounts() {
        sessionService.getByStatus(SessionStatus.NEW)
                .forEach(accountService::updateSessionForAccount);
    }
}
