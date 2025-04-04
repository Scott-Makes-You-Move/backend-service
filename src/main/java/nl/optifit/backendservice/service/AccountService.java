package nl.optifit.backendservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.BiometricsMeasurementDTO;
import nl.optifit.backendservice.dto.MobilityMeasurementDTO;
import nl.optifit.backendservice.dto.UpdateLeaderboardDTO;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Biometrics;
import nl.optifit.backendservice.model.Leaderboard;
import nl.optifit.backendservice.model.Mobility;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.BiometricsRepository;
import nl.optifit.backendservice.repository.LeaderboardRepository;
import nl.optifit.backendservice.repository.MobilityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final BiometricsRepository biometricsRepository;
    private final MobilityRepository mobilityRepository;

    @Transactional
    public Account createAccountForId(String accountId) {
        log.info("Creating account for id [{}]", accountId);

        Account account = Account.builder().id(accountId).build();
        Leaderboard leaderboard = Leaderboard.builder().account(account).completionRate(0.0).currentStreak(0).longestStreak(0).build();
        account.setLeaderboard(leaderboard);

        Account savedAccount = accountRepository.save(account);

        log.info("Created account [{}]", savedAccount.getId());

        return savedAccount;
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
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Could not find user"));;
        Mobility mobility = MobilityMeasurementDTO.toMobility(account, mobilityMeasurementDTO);

        return mobilityRepository.save(mobility);
    }

    public Leaderboard updateLeaderboardForAccount(String accountId, UpdateLeaderboardDTO updateLeaderboardDTO) {
        log.debug("Updating leaderboard for account '{}'", accountId);

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Could not find user"));
        Leaderboard accountLeaderboard = account.getLeaderboard();
        accountLeaderboard.setCompletionRate(updateLeaderboardDTO.getCompletionRate());
        accountLeaderboard.setCurrentStreak(updateLeaderboardDTO.getCurrentStreak());
        accountLeaderboard.setLongestStreak(updateLeaderboardDTO.getLongestStreak());

        return leaderboardRepository.save(accountLeaderboard);
    }
}
