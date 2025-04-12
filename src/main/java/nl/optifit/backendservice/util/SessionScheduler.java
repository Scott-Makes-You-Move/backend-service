package nl.optifit.backendservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.SessionStatus;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.SessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class SessionScheduler {

    private final AccountService accountService;
    private final SessionService sessionService;

    // 0 42 13 * * ? For testing purposes
    @Scheduled(cron = "0 0 10 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void createMorningSession() {
        createSessionsForAllAccounts();
    }

    @Scheduled(cron = "0 30 13 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void createLunchSession() {
        createSessionsForAllAccounts();
    }

    @Scheduled(cron = "0 0 15 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void createAfternoonSession() {
        createSessionsForAllAccounts();
    }

    @Scheduled(cron = "0 0 11 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void updateMorningSession() {
        updateSessionStatusForAllAccounts(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    @Scheduled(cron = "0 30 14 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void updateLunchSession() {
        updateSessionStatusForAllAccounts(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    @Scheduled(cron = "0 0 16 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void updateAfternoonSession() {
        updateSessionStatusForAllAccounts(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private void createSessionsForAllAccounts() {
        log.info("Creating new sessions for all accounts");
        accountService.createSessionsForAllAccounts();
    }

    private void updateSessionStatusForAllAccounts(LocalDateTime now) {
        log.info("Updating session status for all accounts at '{}'", now);
        sessionService.getByStatus(SessionStatus.NEW).forEach(session -> {
            accountService.updateSessionForAccount(session, now);
        });
    }
}
