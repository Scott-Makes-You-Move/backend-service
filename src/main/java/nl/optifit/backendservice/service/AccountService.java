package nl.optifit.backendservice.service;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.BiometricsMeasurementDto;
import nl.optifit.backendservice.dto.MobilityMeasurementDto;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final BiometricsRepository biometricsRepository;
    private final MobilityRepository mobilityRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public Account createAccount(String accountId) {
        log.info("Creating account '{}'", accountId);

        Account account = Account.builder().id(accountId).build();
        Leaderboard leaderboard = Leaderboard.builder()
                .account(account)
                .lastUpdated(LocalDateTime.now())
                .completionRate(0.0)
                .currentStreak(0)
                .longestStreak(0)
                .build();

        account.setLeaderboard(leaderboard);

        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(String accountId) {
        log.info("Deleting account '{}'", accountId);
        accountRepository.deleteById(accountId);
        log.info("Deleted account '{}'", accountId);
        leaderboardRepository.deleteByAccountId(accountId);
        log.info("Deleted leaderboard for account '{}'", accountId);
    }

    public Page<Biometrics> getBiometricsForAccount(String accountId, int page, int size, String direction, String sortBy) {
        log.debug("Retrieving biometrics with page '{}', size '{}', direction '{}', sortBy '{}'", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return biometricsRepository.findAllByAccountId(pageable, accountId);
    }

    public Page<Mobility> getMobilitiesForAccount(String accountId, int page, int size, String direction, String sortBy) {
        log.debug("Retrieving mobilities with page '{}', size '{}', direction '{}', sortBy '{}'", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return mobilityRepository.findAllByAccountId(pageable, accountId);
    }
    public Page<Session> getSessionsForAccount(String accountId, String sessionStartDateString, int page, int size, String direction, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));

        if (StringUtils.isNotBlank(sessionStartDateString)) {
            log.debug("Retrieving sessions on date '{}'  with page '{}', size '{}', direction '{}', sortBy '{}'", sessionStartDateString, page, size, direction, sortBy);
            LocalDate sessionDay = LocalDate.parse(sessionStartDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDateTime start = sessionDay.atTime(LocalTime.MIN);
            LocalDateTime end = sessionDay.atTime(LocalTime.MAX);
            return sessionRepository.findByAccountIdAndSessionStartBetween(accountId, start, end, pageable);
        } else {
            log.debug("Retrieving sessions with page '{}', size '{}', direction '{}', sortBy '{}'", page, size, direction, sortBy);
            return sessionRepository.findAllByAccountId(pageable, accountId);
        }
    }


    public Biometrics saveBiometricForAccount(String accountId, BiometricsMeasurementDto biometricsMeasurementDTO) {
        log.debug("Saving biometric for account '{}'", accountId);
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Could not find user"));
        Biometrics biometrics = BiometricsMeasurementDto.toBiometrics(account, biometricsMeasurementDTO);

        return biometricsRepository.save(biometrics);
    }

    public Mobility saveMobilityForAccount(String accountId, MobilityMeasurementDto mobilityMeasurementDTO) {
        log.debug("Saving mobility for account '{}'", accountId);
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Could not find user"));

        Mobility mobility = MobilityMeasurementDto.toMobility(account, mobilityMeasurementDTO);

        return mobilityRepository.save(mobility);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSessionsForAllAccounts(LocalDateTime sessionStart) {
        log.debug("Creating sessions for all accounts");

        accountRepository.findAll().forEach(account -> {
            Session newSession = Session.builder()
                    .account(account)
                    .sessionStart(sessionStart)
                    .exerciseType(determineExerciseType(sessionStart))
                    .sessionStatus(SessionStatus.NEW)
                    .build();

            sessionRepository.save(newSession);
            updateLeaderboardForAccount(account, newSession);
        });
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

    @Transactional
    public Session updateSessionForAccount(String accountId, LocalDateTime now) {
        Session session = sessionRepository.findByAccountIdAndSessionStatus(accountId, SessionStatus.NEW)
                .orElseThrow(() -> new NotFoundException("No session found for account"));

        return updateSessionForAccount(session, now);
    }

    @Transactional
    public Session updateSessionForAccount(Session lastSession, LocalDateTime now) {
        log.debug("Updating session '{}'", lastSession.getId());

        if (!lastSession.getSessionStatus().equals(SessionStatus.NEW)) {
            log.warn("Trying to update a session '{}' that is already finished", lastSession.getId());
            throw new IllegalStateException("Session is already finished");
        }

        updateSessionStatus(lastSession, now);
        updateLeaderboardForAccount(lastSession.getAccount(), lastSession);

        return lastSession;
    }

    private void updateSessionStatus(Session lastSession, LocalDateTime now) {
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
        sessionRepository.save(lastSession);
    }

    @Transactional
    protected void updateLeaderboardForAccount(Account account, Session lastSession) {
        log.debug("Updating leaderboard for account '{}'", account.getId());
        Leaderboard leaderboard = leaderboardRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new NotFoundException("No leaderboard found for account"));

        if (lastSession.getSessionStatus().equals(SessionStatus.COMPLETED)) {
            leaderboard.setCurrentStreak(leaderboard.getCurrentStreak() + 1);
            leaderboard.setLongestStreak(Math.max(leaderboard.getCurrentStreak(), leaderboard.getLongestStreak()));
        }
        if (lastSession.getSessionStatus().equals(SessionStatus.OVERDUE)) {
            leaderboard.setCurrentStreak(0);
        }
        leaderboard.setCompletionRate(calculateCompletionRate(account));
        leaderboard.setLastUpdated(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        leaderboardRepository.save(leaderboard);
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
}
