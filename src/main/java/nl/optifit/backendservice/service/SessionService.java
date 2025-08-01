package nl.optifit.backendservice.service;

import jakarta.persistence.criteria.*;
import jakarta.ws.rs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.*;
import nl.optifit.backendservice.dto.*;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.ExerciseVideoRepository;
import nl.optifit.backendservice.repository.MobilityRepository;
import nl.optifit.backendservice.repository.SessionRepository;
import org.apache.commons.lang3.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

import static java.time.format.DateTimeFormatter.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class SessionService {
    private final LeaderboardService leaderboardService;
    private final ZapierService zapierService;
    private final SessionRepository sessionRepository;
    private final ExerciseVideoRepository exerciseVideoRepository;
    private final MobilityRepository mobilityRepository;

    public PagedResponseDto<SessionDto> getSessionsForAccount(String accountId, String sessionStartDateString,
                                                              SessionStatus sessionStatus, int page, int size, String direction, String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));

        Page<SessionDto> sessionDtoPage = sessionRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("account").get("id"), accountId));

            if (StringUtils.isNotBlank(sessionStartDateString)) {
                LocalDate sessionDay = LocalDate.parse(sessionStartDateString, ISO_LOCAL_DATE);
                predicates.add(cb.between(root.get("sessionStart"),
                        sessionDay.atTime(LocalTime.MIN),
                        sessionDay.atTime(LocalTime.MAX)));
            }

            if (sessionStatus != null) {
                predicates.add(cb.equal(root.get("sessionStatus"), sessionStatus));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable).map(SessionDto::fromSession);

        return PagedResponseDto.fromPage(sessionDtoPage);
    }

    public void createSessionForAccount(Account account, ExerciseType exerciseType) {
        log.info("Creating session for account '{}'", account.getId());

        Optional<Session> newSessionOptional = createNewSession(account, exerciseType);

        if (newSessionOptional.isEmpty()) {
            return;
        }

        Session newSession = newSessionOptional.get();
        ResponseEntity<String> response = zapierService.sendNotification(newSession);

        String logMessage = Objects.nonNull(response) ?
                String.format("Created Zapier notification status '%s'", response.getStatusCode().value()) :
                "Zapier webhook returned null response code";

        log.debug(logMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSessionForAccount(String lastSessionId) {
        log.debug("Updating session '{}'", lastSessionId);

        Session lastSession = sessionRepository.findById(UUID.fromString(lastSessionId))
                .orElseThrow(() -> new NotFoundException(String.format("Session '%s' not found", lastSessionId)));

        if (!lastSession.getSessionStatus().equals(SessionStatus.NEW)) {
            log.warn("Session is already finished");
            throw new IllegalStateException("Session is already finished");
        }

        updateSession(lastSession);
        leaderboardService.updateLeaderboard(lastSession);
    }

    public Optional<Session> createNewSession(Account account, ExerciseType exerciseType) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).truncatedTo(ChronoUnit.MINUTES);

        Optional<Mobility> latestMeasurementOptional = mobilityRepository.findTopByAccountIdOrderByMeasuredOnDesc(account.getId());

        if (latestMeasurementOptional.isEmpty()) {
            log.debug("Could not find latest measurement for account '{}'. Skipping session creation.", account.getId());
            return Optional.empty();
        }

        Mobility latestMeasurement = latestMeasurementOptional.get();

        Integer score = switch (exerciseType) {
            case HIP -> Optional.ofNullable(latestMeasurement.getHip()).orElse(2);
            case SHOULDER -> Optional.ofNullable(latestMeasurement.getShoulder()).orElse(2);
            case BACK -> Optional.ofNullable(latestMeasurement.getBack()).orElse(2);
        };
        ExerciseVideo exerciseVideo = exerciseVideoRepository.findByExerciseTypeAndScoreEquals(exerciseType, score);

        Session session = Session.builder()
                .account(account)
                .sessionStart(now)
                .exerciseType(exerciseType)
                .sessionStatus(SessionStatus.NEW)
                .exerciseVideo(exerciseVideo)
                .build();

        return Optional.of(sessionRepository.save(session));
    }

    public void updateSession(Session latestSession) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam")).truncatedTo(ChronoUnit.MINUTES);
        ZonedDateTime lastSessionStart = latestSession.getSessionStart().truncatedTo(ChronoUnit.MINUTES);

        long differenceBetweenSessionStartAndNow = Duration.between(lastSessionStart, now).toMinutes();

        if (differenceBetweenSessionStartAndNow >= 60) {
            log.warn("An hour has already passed after session start");
            latestSession.setSessionStatus(SessionStatus.OVERDUE);
            latestSession.setSessionExecutionTime(null);
        } else {
            latestSession.setSessionStatus(SessionStatus.COMPLETED);
            latestSession.setSessionExecutionTime(now);
        }
        sessionRepository.save(latestSession);
    }

    public List<Session> getByStatus(SessionStatus sessionStatus) {
        return sessionRepository.findAllBySessionStatusEquals(sessionStatus);
    }

    public boolean sessionBelongsToAccount(String sessionId, String accountId) {
        log.debug("Checking if session '{}' belongs to account '{}'", sessionId, accountId);
        Session session = sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new NotFoundException(String.format("Could not find session '%s'", sessionId)));

        return session.getAccount().getId().equals(accountId);
    }

    public SessionDto getSingleSessionForAccount(String accountId, String sessionId) {
        return sessionRepository.findByIdAndAccountId(UUID.fromString(sessionId), accountId)
                .map(SessionDto::fromSession)
                .orElseThrow(() -> new NotFoundException(String.format("Could not find session '%s' for account '%s'", sessionId, accountId)));
    }
}
