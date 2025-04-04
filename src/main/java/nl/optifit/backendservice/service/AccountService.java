package nl.optifit.backendservice.service;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.BiometricsMeasurementDTO;
import nl.optifit.backendservice.dto.MobilityMeasurementDTO;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    public Account createAccountForId(String accountId) {
        log.info("Creating account for id [{}]", accountId);

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
        log.info("Deleting account [{}]", accountId);
        accountRepository.deleteById(accountId);
        log.info("Deleted account [{}]", accountId);
        leaderboardRepository.deleteByAccountId(accountId);
        log.info("Deleted leaderboard for account [{}]", accountId);
    }

    public Page<Biometrics> getBiometricsForAccount(String accountId, int page, int size, String direction, String sortBy) {
        log.debug("Retrieving biometrics with page [{}], size [{}], direction [{}], sortBy [{}]", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return biometricsRepository.findAllByAccountId(pageable, accountId);
    }

    public Page<Mobility> getMobilitiesForAccount(String accountId, int page, int size, String direction, String sortBy) {
        log.debug("Retrieving mobilities with page [{}], size [{}], direction [{}], sortBy [{}]", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return mobilityRepository.findAllByAccountId(pageable, accountId);
    }

    public Biometrics saveBiometricForAccount(String accountId, BiometricsMeasurementDTO biometricsMeasurementDTO) {
        log.debug("Saving biometric for account '{}'", accountId);
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Could not find user"));
        Biometrics biometrics = BiometricsMeasurementDTO.toBiometrics(account, biometricsMeasurementDTO);

        return biometricsRepository.save(biometrics);
    }

    public Mobility saveMobilityForAccount(String accountId, MobilityMeasurementDTO mobilityMeasurementDTO) {
        log.debug("Saving mobility for account '{}'", accountId);
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Could not find user"));
        ;
        Mobility mobility = MobilityMeasurementDTO.toMobility(account, mobilityMeasurementDTO);

        return mobilityRepository.save(mobility);
    }

    @Transactional
    public Session updateSessionForAccount(String accountId) {
        log.debug("Updating session for account '{}'", accountId);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        Session lastSession = sessionRepository.findFirstByAccountIdOrderBySessionStartDesc(accountId)
                .orElseThrow(() -> new NotFoundException("Could not find session for user"));

        if (Objects.nonNull(lastSession.getSessionExecutionTime())) {
            log.warn("Session already completed");
            return null;
        }

        if (now.isAfter(lastSession.getSessionStart().plusHours(1))) {
            log.warn("An hour has already passed after session start");
            return null;
        }
        lastSession.setSessionExecutionTime(now);
        updateLeaderboardForAccount(accountId);

        return sessionRepository.save(lastSession);
    }

    private void updateLeaderboardForAccount(String accountId) {
        log.debug("Updating leaderboard for account '{}'", accountId);
        LocalDateTime now = LocalDateTime.now();

        Account account = accountRepository.findAccountWithLeaderboard(accountId)
                .orElseThrow(() -> new RuntimeException("Could not find user"));
        Leaderboard leaderboard = account.getLeaderboard();

        sessionRepository.findFirstByAccountIdOrderBySessionStartDesc(accountId).ifPresentOrElse(lastSession -> {
            boolean lastSessionCompleted = Objects.nonNull(lastSession.getSessionExecutionTime());

            if (leaderboard.getLastUpdated().isAfter(lastSession.getSessionExecutionTime())) {
                log.warn("Tried to update leaderboard to an invalid state");
                return;
            }

            if (lastSessionCompleted) {
                int newCurrentStreak = leaderboard.getCurrentStreak() + 1;
                leaderboard.setCurrentStreak(newCurrentStreak);
                leaderboard.setLongestStreak(Math.max(newCurrentStreak, leaderboard.getLongestStreak()));
            } else {
                leaderboard.setCurrentStreak(0);
            }
            leaderboard.setCompletionRate(calculateCompletionRate(accountId));
            leaderboard.setLastUpdated(now);
        }, () -> log.warn("Could not find leaderboard for account '{}'", accountId));
    }

    private double calculateCompletionRate(String accountId) {
        List<Session> sessionsForAccount = sessionRepository.findAllByAccountId(accountId);

        long totalSessions = sessionsForAccount.size();
        long completedSessions = sessionsForAccount.stream()
                .map(Session::getSessionExecutionTime)
                .filter(Objects::nonNull)
                .count();

        double completionRate = totalSessions > 0
                ? (double) completedSessions / totalSessions * 100
                : 0;
        return completionRate;
    }
}
