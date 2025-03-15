package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.LeaderboardRepository;
import nl.optifit.backendservice.repository.ProgressRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/bootstrap")
@RestController
public class BootstrapController {

    public static final String KEYCLOAK_SUB_CLAIM = "sub";

    private final AccountRepository accountRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final ProgressRepository progressRepository;

    @GetMapping
    public String bootstrap() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String sub = jwt.getClaimAsString(KEYCLOAK_SUB_CLAIM);
        String userFullName = jwt.getClaimAsString("name");

        log.debug("Initializing data for user [{}]", userFullName);
        initiateAccount(sub);
        initiateProgress();
        initiateLeaderboard();
        log.debug("Finished initializing data for user [{}]", userFullName);

        return "Data initialized for user [" + userFullName  + "]";
    }

    private void initiateAccount(String sub) {
        log.debug("Bootstrapping account for sub '{}'", sub);
        Account savedAccount = accountRepository.save(Account.builder().accountId(sub).build());
        log.debug("Account '{}' saved successfully", savedAccount.getAccountId());
    }

    private void initiateProgress() {
        accountRepository.findAll().forEach(account -> {
            log.debug("Initiating progress for account: [{}]", account.getAccountId());
            LocalDateTime january = LocalDateTime.of(LocalDate.of(2025, Month.JANUARY, 3), LocalTime.of(12, 0, 0));
            LocalDateTime february = LocalDateTime.of(LocalDate.of(2025, Month.FEBRUARY, 7), LocalTime.of(12, 0, 0));
            LocalDateTime march = LocalDateTime.of(LocalDate.of(2025, Month.MARCH, 7), LocalTime.of(12, 0, 0));

            Progress progressForAccount = Progress.builder()
                    .account(account)
                    .biometrics(new ArrayList<>())
                    .mobilities(new ArrayList<>())
                    .build();

            Biometrics januaryBiometrics = initializeBiometrics(account, progressForAccount, january, 82.5, 14.2, 7);
            Biometrics februaryBiometrics = initializeBiometrics(account, progressForAccount, february, 83.1, 14.4, 7);
            Biometrics marchBiometrics = initializeBiometrics(account, progressForAccount, march, 82.9, 14.3, 7);

            Mobility januaryMobility = initializeMobility(account, progressForAccount, january, 2, 1, 2);
            Mobility februaryMobility = initializeMobility(account, progressForAccount, february, 2, 2, 2);
            Mobility marchMobility = initializeMobility(account, progressForAccount, march, 1, 1, 2);


            progressForAccount.getBiometrics().addAll(List.of(januaryBiometrics, februaryBiometrics, marchBiometrics));
            progressForAccount.getMobilities().addAll(List.of(januaryMobility, februaryMobility, marchMobility));

            progressRepository.save(progressForAccount);
        });
    }

    private Biometrics initializeBiometrics(Account account, Progress progress, LocalDateTime measuredOn, double weight, double fat, int visceralFat) {
        return Biometrics.builder()
                .account(account)
                .progress(progress)
                .measuredOn(measuredOn)
                .weight(weight)
                .fat(fat)
                .visceralFat(visceralFat)
                .build();
    }

    private Mobility initializeMobility(Account account, Progress progress, LocalDateTime measuredOn, int shoulder, int back, int hip) {
        return Mobility.builder()
                .account(account)
                .progress(progress)
                .measuredOn(measuredOn)
                .shoulder(shoulder)
                .back(back)
                .hip(hip)
                .build();
    }

    private void initiateLeaderboard() {
        accountRepository.findAll().forEach(account -> {
            Leaderboard leaderboard = Leaderboard.builder()
                    .account(account)
                    .completionRate(96.0)
                    .currentStreak(4)
                    .longestStreak(4)
                    .build();
            leaderboardRepository.save(leaderboard);
        });
    }
}