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

    private final SessionSchedule schedule;
    private final AccountService accountService;
    private final SessionService sessionService;

    @Scheduled(cron = "0 */30 * * * *")
    public void processRegions() {
        TIME_ZONES.forEach(this::processRegion);
    }

    private void processRegion(String zoneId) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(zoneId));
        LocalTime localTime = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);

        log.debug("Checking region '{}' at '{}'", zoneId, localTime);

        if (matches(localTime, schedule.morning().create())) createSessions(zoneId, HIP);
        if (matches(localTime, schedule.morning().update())) updateSessions(zoneId);

        if (matches(localTime, schedule.lunch().create())) createSessions(zoneId, SHOULDER);
        if (matches(localTime, schedule.lunch().update())) updateSessions(zoneId);

        if (matches(localTime, schedule.afternoon().create())) createSessions(zoneId, BACK);
        if (matches(localTime, schedule.afternoon().update())) updateSessions(zoneId);
    }

    private boolean matches(LocalTime now, LocalTime target) {
        return now.equals(target);
    }

    private void createSessions(String zoneId, ExerciseType type) {
        log.debug("Creating '{}' sessions for timezone '{}'", type, zoneId);
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            accountService.findAllAccountsByTimezone(zoneId)
                    .forEach(account -> exec.submit(() -> sessionService.createSessionForAccount(account, type)));
        }
    }

    private void updateSessions(String zoneId) {
        log.debug("Updating NEW sessions for '{}'", zoneId);
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            sessionService.getByStatus(SessionStatus.NEW).stream()
                    .filter(session -> zoneId.equals(session.getAccount().getTimezone()))
                    .forEach(session -> exec.submit(() -> sessionService.updateSessionForAccount(session.getId().toString())));
        }
    }
}
