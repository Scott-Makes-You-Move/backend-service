package nl.optifit.backendservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.SessionStatus;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.SessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class SessionScheduler {

    private final AccountService accountService;
    private final SessionService sessionService;

    // 0 42 13 * * ?
    // 0 0 10,13,15 ? * MON-FRI
    @Scheduled(cron = "0 0 10,13,15 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void createDailySessions() {
        LocalTime currentTime = LocalDateTime.now()
                .truncatedTo(ChronoUnit.SECONDS)
                .toLocalTime();

        if (isCreateSessionScheduledTime(currentTime)) {
            createSessionsForAllAccounts();
        }
    }

    // 0 0 11,14,16 ? * MON-FRI
    @Scheduled(cron = "0 0 11,14,16 ? * MON-FRI", zone = "Europe/Amsterdam")
    public void updateLastSessionStatus() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalTime currentTime = now.toLocalTime();

        if (isUpdateSessionStatusScheduledTime(currentTime)) {
            updateSessionStatusForAllAccounts(now);
        }
    }

    private boolean isCreateSessionScheduledTime(LocalTime currentTime) {
        return currentTime.truncatedTo(ChronoUnit.MINUTES).equals(LocalTime.of(10, 0)) ||
                currentTime.truncatedTo(ChronoUnit.MINUTES).equals(LocalTime.of(13, 0)) ||
                currentTime.truncatedTo(ChronoUnit.MINUTES).equals(LocalTime.of(15, 0));
    }

    private boolean isUpdateSessionStatusScheduledTime(LocalTime currentTime) {
        return currentTime.truncatedTo(ChronoUnit.MINUTES).equals(LocalTime.of(11, 0)) ||
                currentTime.truncatedTo(ChronoUnit.MINUTES).equals(LocalTime.of(14, 0)) ||
                currentTime.truncatedTo(ChronoUnit.MINUTES).equals(LocalTime.of(16, 0));
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
