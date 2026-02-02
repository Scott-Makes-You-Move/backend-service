package nl.optifit.backendservice.service;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.AccountDto;
import nl.optifit.backendservice.dto.HealthIndexDto;
import nl.optifit.backendservice.dto.PagedResponseDto;
import nl.optifit.backendservice.dto.UserDto;
import nl.optifit.backendservice.dto.UserHealthProfileDto;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Biometrics;
import nl.optifit.backendservice.model.Leaderboard;
import nl.optifit.backendservice.repository.AccountRepository;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountService {
    private final DateTimeFormatter DATE_OF_BIRTH_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final LeaderboardService leaderboardService;
    private final AccountRepository accountRepository;
    private final KeycloakService keycloakService;
    private final BiometricsService biometricsService;
    private final DriveService driveService;

    public PagedResponseDto<UserDto> findAccounts(int page, int size, String direction, String sortBy) {
        log.info("Finding accounts with page '{}', size '{}', direction '{}', sortBy '{}'", page, size, direction, sortBy);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));

        Page<Account> accounts = accountRepository.findAll(pageable);
        log.debug("Retrieved {} accounts from database and {} pages", accounts.getTotalElements(), accounts.getTotalPages() );

        return accounts.stream()
                .peek(account -> log.debug("Finding account in keycloak for id '{}'", account.getId()))
                .map(account -> keycloakService.findUserById(account.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(user -> log.debug("Found user. Trying to convert toRepresentation"))
                .peek(user -> log.debug("Found user: {}", user.toRepresentation().getUsername()))
                .map(UserResource::toRepresentation)
                .peek(userRepresentation -> log.debug("Converted user to representation. Mapping to UserDto"))
                .map(UserDto::fromUserRepresentation)
                .collect(Collectors.collectingAndThen(Collectors.toList(), users ->
                        new PagedResponseDto<>(users, page, size, accounts.getTotalElements(), accounts.getTotalPages())));
    }

    @Transactional
    public AccountDto createAccount(String accountId, String timezone) throws IOException {
        log.info("Creating account '{}' with timezone '{}'", accountId, timezone);
        Account account = Account.builder().id(accountId).timezone(timezone).build();
        Leaderboard leaderboard = leaderboardService.createLeaderboardForAccount(account);
        account.setLeaderboard(leaderboard);
        Account savedAccount = accountRepository.save(account);

        UserRepresentation user = keycloakService.findUserById(accountId)
                .orElseThrow(() -> new NotFoundException("User not found"))
                .toRepresentation();

        driveService.createDriveFolderInRoot(user.getUsername());

        return AccountDto.fromAccount(savedAccount);
    }

    @Transactional
    public void deleteAccount(String accountId) throws IOException {
        log.info("Deleting account '{}'", accountId);
        accountRepository.deleteById(accountId);

        UserRepresentation user = keycloakService.findUserById(accountId)
                .orElseThrow(() -> new NotFoundException("User not found"))
                .toRepresentation();

        driveService.deleteDriveFolderInRoot(user.getUsername());
    }

    public List<Account> findAllAccounts() {
        log.debug("Finding all accounts");
        return accountRepository.findAll();
    }

    public List<Account> findAllAccountsByTimezone(String timezone) {
        log.debug("Finding all accounts for timezone '{}'", timezone);
        return accountRepository.findAllByTimezone(timezone);
    }

    public HealthIndexDto calculateHealthIndex(String accountId) {
        int maxVisceralHealthy = 12;
        int minVisceralHealthy = 1;

        UserHealthProfileDto userHealthProfile = calculateUserHealthProfile(accountId);
        log.debug("Calculated health profile: {}", userHealthProfile);
        double minBodyFat, maxBodyFat;

        switch (userHealthProfile.sex()) {
            case "Male" -> {
                if (userHealthProfile.age() <= 39) {
                    minBodyFat = 7;
                    maxBodyFat = 20;
                } else if (userHealthProfile.age() <= 59) {
                    minBodyFat = 10;
                    maxBodyFat = 22;
                } else {
                    minBodyFat = 12;
                    maxBodyFat = 25;
                }
            }
            case "Female" -> {
                if (userHealthProfile.age() <= 39) {
                    minBodyFat = 21;
                    maxBodyFat = 33;
                } else if (userHealthProfile.age() <= 59) {
                    minBodyFat = 23;
                    maxBodyFat = 34;
                } else {
                    minBodyFat = 24;
                    maxBodyFat = 36;
                }
            }
            default -> throw new IllegalStateException("Invalid gender");
        }

        double fatPercentile = (userHealthProfile.fat() - minBodyFat) / (maxBodyFat - minBodyFat);
        if (fatPercentile < 0) {
            fatPercentile = 0;
        } else if (fatPercentile > 1) {
            fatPercentile = 1;
        }

        double visceralFatScore = (maxVisceralHealthy - userHealthProfile.visceralFat()) / (double) (maxVisceralHealthy - minVisceralHealthy);
        if (visceralFatScore < 0) {
            visceralFatScore = 0;
        } else if (visceralFatScore > 1) {
            visceralFatScore = 1;
        }

        double healthIndex = (1 - fatPercentile) * visceralFatScore * 100;

        return new HealthIndexDto(healthIndex);
    }

    public UserHealthProfileDto calculateUserHealthProfile(String accountId) {
        log.debug("Calculating health profile for account '{}'", accountId);
        UserResource userResource = keycloakService.findUserById(accountId).orElse(null);
        log.debug("Found user resource: {}", userResource);
        String dateOfBirthString = userResource.toRepresentation().getAttributes().get("dob").stream().findFirst().orElseThrow();
        log.debug("Found date of birth: {}", dateOfBirthString);
        String sex = userResource.toRepresentation().getAttributes().get("sex").stream().findFirst().orElseThrow();
        log.debug("Found sex: {}", sex);

        LocalDate dateOfBirth = LocalDate.parse(dateOfBirthString, DATE_OF_BIRTH_FORMATTER);
        log.debug("Parsed date of birth: {}", dateOfBirth);
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        log.debug("Calculated age: {}", age);

        Biometrics recentBiometrics = biometricsService.findMostRecentBiometricsForAccount(accountId);
        log.debug("Found most recent biometrics: {}", recentBiometrics);

        return new UserHealthProfileDto(sex, age, recentBiometrics.getWeight(), recentBiometrics.getFat(), recentBiometrics.getVisceralFat());
    }
}
