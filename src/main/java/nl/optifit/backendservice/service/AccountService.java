package nl.optifit.backendservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.BiometricsMeasurementDTO;
import nl.optifit.backendservice.dto.MobilityMeasurementDTO;
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

import java.util.Optional;
import java.util.UUID;

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

        Account account = Account.builder().accountId(accountId).build();
        Leaderboard leaderboard = Leaderboard.builder().account(account).completionRate(0.0).currentStreak(0).longestStreak(0).build();
        account.setLeaderboard(leaderboard);

        Account savedAccount = accountRepository.save(account);

        log.info("Created account [{}]", savedAccount.getAccountId());

        return savedAccount;
    }

    @Transactional
    public void deleteAccount(String accountId) {
        log.info("Deleting account [{}]", accountId);
        accountRepository.deleteByAccountId(accountId);
        log.info("Deleted account [{}]", accountId);
        leaderboardRepository.deleteByAccount_AccountId(accountId);
        log.info("Deleted leaderboard for account [{}]", accountId);
    }

    public Page<Biometrics> getBiometricsForAccount(UUID accountId, int page, int size, String direction, String sortBy) {
        log.debug("Retrieving biometrics with page [{}], size [{}], direction [{}], sortBy [{}]", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return biometricsRepository.findAllByAccountId(pageable, accountId);
    }

    public Page<Mobility> getMobilitiesForAccount(UUID accountId, int page, int size, String direction, String sortBy) {
        log.debug("Retrieving mobilities with page [{}], size [{}], direction [{}], sortBy [{}]", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return mobilityRepository.findAllByAccountId(pageable, accountId);
    }

    public Biometrics saveBiometricsForAccount(String accountId, BiometricsMeasurementDTO biometricsMeasurementDTO) {
        log.debug("Saving biometric for account '{}'", accountId);
        Optional<Account> optionalAccount = accountRepository.findByAccountId(accountId);

        Biometrics biometrics = optionalAccount.map(account -> BiometricsMeasurementDTO.toBiometrics(account, biometricsMeasurementDTO))
                .orElseThrow(() -> new RuntimeException("Could not find user"));

        return biometricsRepository.save(biometrics);
    }

    public Mobility saveMobilityForAccount(String accountId, MobilityMeasurementDTO mobilityMeasurementDTO) {
        log.debug("Saving mobility for account '{}'", accountId);
        Optional<Account> optionalAccount = accountRepository.findByAccountId(accountId);

        Mobility mobility = optionalAccount.map(account -> MobilityMeasurementDTO.toMobility(account, mobilityMeasurementDTO))
                .orElseThrow(() -> new RuntimeException("Could not find user"));

        return mobilityRepository.save(mobility);
    }
}
