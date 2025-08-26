package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.*;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.AccountRepository;
import org.keycloak.admin.client.resource.UserResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountService {
    private final DateTimeFormatter DATE_OF_BIRTH_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final LeaderboardService leaderboardService;
    private final AccountRepository accountRepository;
    private final KeycloakService keycloakService;
    private final BiometricsService biometricsService;

    @Transactional
    public AccountDto createAccount(String accountId) {
        log.info("Creating account '{}'", accountId);
        Account account = Account.builder().id(accountId).build();
        Leaderboard leaderboard = leaderboardService.createLeaderboardForAccount(account);
        account.setLeaderboard(leaderboard);
        Account savedAccount = accountRepository.save(account);

        return AccountDto.fromAccount(savedAccount);
    }

    @Transactional
    public void deleteAccount(String accountId) {
        log.info("Deleting account '{}'", accountId);
        accountRepository.deleteById(accountId);
    }

    public List<Account> findAllAccounts() {
        log.debug("Finding all accounts");
        return accountRepository.findAll();
    }

    public HealthIndexDto calculateHealthIndex(String accountId) {
        int maxVisceralHealthy = 12;
        int minVisceralHealthy = 1;

        UserHealthProfileDto userHealthProfile = calculateUserHealthProfile(accountId);
        double minBodyFat, maxBodyFat;

        switch (userHealthProfile.getGender()) {
            case "Male" -> {
                if (userHealthProfile.getAge() <= 39) {
                    minBodyFat = 7;
                    maxBodyFat = 20;
                } else if (userHealthProfile.getAge() <= 59) {
                    minBodyFat = 10;
                    maxBodyFat = 22;
                } else {
                    minBodyFat = 12;
                    maxBodyFat = 25;
                }
            }
            case "Female" -> {
                if (userHealthProfile.getAge() <= 39) {
                    minBodyFat = 21;
                    maxBodyFat = 33;
                } else if (userHealthProfile.getAge() <= 59) {
                    minBodyFat = 23;
                    maxBodyFat = 34;
                } else {
                    minBodyFat = 24;
                    maxBodyFat = 36;
                }
            }
            default -> throw new IllegalStateException("Invalid gender");
        }

        double fatPercentile = (userHealthProfile.getFat() - minBodyFat) / (maxBodyFat - minBodyFat);
        if (fatPercentile < 0 ) {
            fatPercentile = 0;
        } else if (fatPercentile > 1) {
            fatPercentile = 1;
        }

        double visceralFatScore = (maxVisceralHealthy - userHealthProfile.getVisceralFat()) / (double) (maxVisceralHealthy - minVisceralHealthy);
        if (visceralFatScore < 0) {
            visceralFatScore = 0;
        } else if (visceralFatScore > 1) {
            visceralFatScore = 1;
        }

        double healthIndex = (1 - fatPercentile) * visceralFatScore * 100;

        return HealthIndexDto.builder().healthIndex(healthIndex).build();
    }


    public UserHealthProfileDto calculateUserHealthProfile(String accountId) {
        UserResource userResource = keycloakService.findUserById(accountId).orElse(null);
        String dateOfBirthString = userResource.toRepresentation().getAttributes().get("dob").stream().findFirst().orElseThrow();
        String gender = userResource.toRepresentation().getAttributes().get("gender").stream().findFirst().orElseThrow();

        LocalDate dateOfBirth = LocalDate.parse(dateOfBirthString, DATE_OF_BIRTH_FORMATTER);
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();

        Biometrics recentBiometrics = biometricsService.findMostRecentBiometricsForAccount(accountId);

        return UserHealthProfileDto.builder()
                .gender(gender)
                .age(age)
                .weight(recentBiometrics.getWeight())
                .fat(recentBiometrics.getFat())
                .visceralFat(recentBiometrics.getVisceralFat())
                .build();
    }
}
