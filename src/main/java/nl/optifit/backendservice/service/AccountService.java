package nl.optifit.backendservice.service;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.*;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.*;
import nl.optifit.backendservice.util.*;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.time.format.DateTimeFormatter.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountService {
    private final ZapierService zapierService;

    private final AccountRepository accountRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final BiometricsRepository biometricsRepository;
    private final MobilityRepository mobilityRepository;
    private final SessionRepository sessionRepository;
    private final KeycloakService keycloakService;

    public Page<Session> getSessionsForAccount(String accountId, String sessionStartDateString, int page, int size, String direction, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));

        if (StringUtils.isNotBlank(sessionStartDateString)) {
            log.debug("Retrieving sessions on date '{}'  with page '{}', size '{}', direction '{}', sortBy '{}'", sessionStartDateString, page, size, direction, sortBy);
            LocalDate sessionDay = LocalDate.parse(sessionStartDateString, ISO_LOCAL_DATE);
            LocalDateTime start = sessionDay.atTime(LocalTime.MIN);
            LocalDateTime end = sessionDay.atTime(LocalTime.MAX);
            return sessionRepository.findByAccountIdAndSessionStartBetween(accountId, start, end, pageable);
        } else {
            log.debug("Retrieving sessions with page '{}', size '{}', direction '{}', sortBy '{}'", page, size, direction, sortBy);
            return sessionRepository.findAllByAccountId(pageable, accountId);
        }
    }

    @Transactional
    public Session updateSessionForAccount(String accountId) {
        Session session = sessionRepository.findByAccountIdAndSessionStatus(accountId, SessionStatus.NEW)
                .orElseThrow(() -> new NotFoundException("No session found for account"));

        return updateSessionForAccount(session);
    }

    @Transactional
    public Account createAccount(String accountId) {
        log.info("Creating account '{}'", accountId);
        Account account = Account.builder().id(accountId).build();
        Leaderboard leaderboard = createNewLeaderboard(account);
        account.setLeaderboard(leaderboard);

        return accountRepository.save(account);
    }

    public Page<Mobility> getMobilitiesForAccount(String accountId, int page, int size, String direction, String sortBy) {
        log.debug("Retrieving mobilities with page '{}', size '{}', direction '{}', sortBy '{}'", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return mobilityRepository.findAllByAccountId(pageable, accountId);
    }

    public Mobility saveMobilityForAccount(String accountId, MobilityMeasurementDto mobilityMeasurementDTO) {
        log.debug("Saving mobility for account '{}'", accountId);
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Could not find user"));

        Mobility mobility = MobilityMeasurementDto.toMobility(account, mobilityMeasurementDTO);

        return mobilityRepository.save(mobility);
    }

    public Page<Biometrics> getBiometricsForAccount(String accountId, int page, int size, String direction, String sortBy) {
        log.debug("Retrieving biometrics with page '{}', size '{}', direction '{}', sortBy '{}'", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return biometricsRepository.findAllByAccountId(pageable, accountId);
    }

    public Biometrics saveBiometricForAccount(String accountId, BiometricsMeasurementDto biometricsMeasurementDTO) {
        log.debug("Saving biometric for account '{}'", accountId);
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Could not find user"));
        Biometrics biometrics = BiometricsMeasurementDto.toBiometrics(account, biometricsMeasurementDTO);

        return biometricsRepository.save(biometrics);
    }

    @Transactional
    public void deleteAccount(String accountId) {
        log.info("Deleting account '{}'", accountId);
        accountRepository.deleteById(accountId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSessionForAccount(Account account) {
        log.debug("Creating session for account '{}'", account.getId());

        Session newSession = createNewSessionForAccount(account);
        UsersWithMobilitiesDto userWithMobilityScoreDto = createUserWithMobilityScoreDto(newSession);

        Mono<ResponseEntity<String>> mono = zapierService.triggerZapierWebhook(userWithMobilityScoreDto);
        ResponseEntity<String> response = mono.block();

        String logMessage = Objects.nonNull(response) ?
                String.format("Created zapier notification status '%s'", response.getStatusCode().value()) :
                "Zapier webhook returned null response code";

        log.info(logMessage);
    }

    @Transactional
    public Session updateSessionForAccount(Session lastSession) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        log.debug("Updating session '{}'", lastSession.getId());

        if (!lastSession.getSessionStatus().equals(SessionStatus.NEW)) {
            log.warn("Trying to update a session '{}' that is already finished", lastSession.getId());
            throw new IllegalStateException("Session is already finished");
        }

        Session updatedSession = updateSessionStatus(lastSession, now);
        updateLeaderboardForAccount(lastSession);

        return updatedSession;
    }

    public List<Account> findAllAccounts() {
        return accountRepository.findAll();
    }

    private static Leaderboard createNewLeaderboard(Account account) {
        return Leaderboard.builder()
                .account(account)
                .lastUpdated(LocalDateTime.now())
                .completionRate(0.0)
                .currentStreak(0)
                .longestStreak(0)
                .build();
    }

    private Session createNewSessionForAccount(Account account) {
        Session createdSession = createAndSaveSession(account);
        updateLeaderboardForAccount(createdSession);
        return createdSession;
    }

    private void updateLeaderboardForAccount(Session session) {
        Account account = session.getAccount();

        log.debug("Updating leaderboard for account '{}'", account.getId());
        Leaderboard leaderboard = leaderboardRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new NotFoundException("No leaderboard found for account"));

        if (session.getSessionStatus().equals(SessionStatus.COMPLETED)) {
            leaderboard.setCurrentStreak(leaderboard.getCurrentStreak() + 1);
            leaderboard.setLongestStreak(Math.max(leaderboard.getCurrentStreak(), leaderboard.getLongestStreak()));
        }
        if (session.getSessionStatus().equals(SessionStatus.OVERDUE)) {
            leaderboard.setCurrentStreak(0);
        }
        leaderboard.setCompletionRate(calculateCompletionRate(account));
        leaderboard.setLastUpdated(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        leaderboardRepository.save(leaderboard);
    }

    private Session updateSessionStatus(Session lastSession, LocalDateTime now) {
        LocalDateTime lastSessionStart = lastSession.getSessionStart().truncatedTo(ChronoUnit.SECONDS);

        long differenceBetweenSessionStartAndNow = Duration.between(lastSessionStart, now).toMinutes();

        if (differenceBetweenSessionStartAndNow >= 60) {
            log.warn("An hour has already passed after session start");
            lastSession.setSessionStatus(SessionStatus.OVERDUE);
            lastSession.setSessionExecutionTime(null);
        } else {
            lastSession.setSessionStatus(SessionStatus.COMPLETED);
            lastSession.setSessionExecutionTime(now);
        }
        return sessionRepository.save(lastSession);
    }

    private UsersWithMobilitiesDto createUserWithMobilityScoreDto(Session session) {
        UserResource userResource = keycloakService.findUserById(session.getAccount().getId())
                .orElseThrow(() -> new NotFoundException("Could not find user"));
        UserRepresentation user = userResource.toRepresentation();

        int relevantScore = getRelevantScore(session);

        return UsersWithMobilitiesDto.builder()
                .name(user.getFirstName())
                .email(user.getEmail())
                .score(relevantScore)
                .exerciseType(session.getExerciseType().getDisplayName().toLowerCase())
                .build();
    }

    private int getRelevantScore(Session session) {
        Mobility mostRecent = mobilityRepository.findTopByAccountIdOrderByMeasuredOnDesc(session.getAccount().getId())
                .orElseThrow(() -> new RuntimeException("Could not find most recent mobility measurement"));

        return switch (session.getExerciseType()) {
            case HIP -> mostRecent.getHip();
            case BACK -> mostRecent.getBack();
            case SHOULDER -> mostRecent.getShoulder();
        };
    }

    private ExerciseType determineExerciseType(LocalDateTime time) {
        LocalTime localTime = time.toLocalTime();
        if (localTime.equals(LocalTime.of(10, 0))) {
            return ExerciseType.HIP;
        } else if (localTime.equals(LocalTime.of(13, 30))) {
            return ExerciseType.SHOULDER;
        } else {
            return ExerciseType.BACK;
        }
    }

    private double calculateCompletionRate(Account account) {
        List<Session> sessionsForAccount = sessionRepository.findAllByAccountId(account.getId());

        long totalSessions = sessionsForAccount.size();
        long completedSessions = sessionsForAccount.stream()
                .map(Session::getSessionExecutionTime)
                .filter(Objects::nonNull)
                .count();

        return totalSessions > 0
                ? (double) completedSessions / totalSessions * 100
                : 0;
    }

    private Session createAndSaveSession(Account account) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Session session = Session.builder()
                .account(account)
                .sessionStart(now)
                .exerciseType(determineExerciseType(now))
                .sessionStatus(SessionStatus.NEW)
                .build();

        return sessionRepository.save(session);
    }
}
