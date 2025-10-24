package nl.optifit.backendservice.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.ExerciseType;
import nl.optifit.backendservice.model.SessionStatus;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.SessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;

import static nl.optifit.backendservice.model.ExerciseType.BACK;
import static nl.optifit.backendservice.model.ExerciseType.HIP;
import static nl.optifit.backendservice.model.ExerciseType.SHOULDER;

@Slf4j
@RequiredArgsConstructor
@Component
public class SessionCronScheduler {

    private static final List<String> TIME_ZONES = List.of(
            "Europe/Amsterdam",
            "Australia/Sydney"
    );

    private final SessionSchedule sessionSchedule;
    private final AccountService accountService;
    private final SessionService sessionService;

    @Scheduled(cron = "0 */30 * * * *")
    public void processRegions() {
        TIME_ZONES.forEach(this::processRegion);
    }

    private void processRegion(String timezone) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
        LocalTime localTime = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);

        log.debug("Checking timezone '{}' at '{}'", timezone, localTime);

        if (matches(localTime, sessionSchedule.getMorning().create())) createSessions(timezone, HIP);
        if (matches(localTime, sessionSchedule.getMorning().update())) updateSessions(timezone);

        if (matches(localTime, sessionSchedule.getLunch().create())) createSessions(timezone, SHOULDER);
        if (matches(localTime, sessionSchedule.getLunch().update())) updateSessions(timezone);

        if (matches(localTime, sessionSchedule.getAfternoon().create())) createSessions(timezone, BACK);
        if (matches(localTime, sessionSchedule.getAfternoon().update())) updateSessions(timezone);
    }

    private boolean matches(LocalTime now, LocalTime target) {
        return now.equals(target);
    }

    private void createSessions(String timezone, ExerciseType type) {
        log.debug("Creating '{}' sessions for timezone '{}'", type, timezone);
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            accountService.findAllAccountsByTimezone(timezone)
                    .forEach(account -> exec.submit(() -> sessionService.createSessionForAccount(account, type)));
        }
    }

    private void updateSessions(String timezone) {
        log.debug("Updating NEW sessions for '{}'", timezone);
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            sessionService.getByStatus(SessionStatus.NEW).stream()
                    .filter(session -> timezone.equals(session.getAccount().getTimezone()))
                    .forEach(session -> exec.submit(() -> sessionService.updateSessionForAccount(session.getId().toString())));
        }
    }
}
