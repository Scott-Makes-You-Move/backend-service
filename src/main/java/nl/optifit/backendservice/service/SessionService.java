package nl.optifit.backendservice.service;

import com.microsoft.graph.models.DateTimeTimeZone;
import jakarta.persistence.criteria.Predicate;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.PagedResponseDto;
import nl.optifit.backendservice.dto.SessionDto;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.ExerciseType;
import nl.optifit.backendservice.model.ExerciseVideo;
import nl.optifit.backendservice.model.Mobility;
import nl.optifit.backendservice.model.Session;
import nl.optifit.backendservice.model.SessionStatus;
import nl.optifit.backendservice.repository.ExerciseVideoRepository;
import nl.optifit.backendservice.repository.MobilityRepository;
import nl.optifit.backendservice.repository.SessionRepository;
import nl.optifit.backendservice.utility.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

@RequiredArgsConstructor
@Service
@Slf4j
public class SessionService {
    private final LeaderboardService leaderboardService;
    private final SessionRepository sessionRepository;
    private final ExerciseVideoRepository exerciseVideoRepository;
    private final MobilityRepository mobilityRepository;
    private final KeycloakService keycloakService;
    private final NotificationService notificationService;

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

        Optional<UserResource> userById = keycloakService.findUserById(account.getId());
        userById.ifPresentOrElse(userResource -> {
            UserRepresentation userRepresentation = userResource.toRepresentation();
            String email = userRepresentation.getEmail();
            String fullName = "%s %s".formatted(userRepresentation.getFirstName(), userRepresentation.getLastName());
            DateTimeTimeZone start = DateUtil.toGraphDateTime(newSession.getSessionStart());
            DateTimeTimeZone end = DateUtil.toGraphDateTime(newSession.getSessionStart().plusHours(1));

            log.debug("Sending event for account '{}' with start '{}' and end '{}'", account.getId(), start.dateTime, end.dateTime);
            notificationService.sendCalendarEventFrom(email, fullName, newSession.getId().toString(), start, end);
        }, () -> log.warn("Could not find user for account '{}'", account.getId()));
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
