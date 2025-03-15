package nl.optifit.backendservice;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootApplication
public class BackendServiceApplication implements CommandLineRunner {

    public static final String TEST_USER_SUB = "3dac8d2f-a6d1-4e8e-8920-eeb8e77963a5";

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private LeaderboardRepository leaderboardRepository;
    @Autowired
    private ProgressRepository progressRepository;

    public static void main(String[] args) {
        SpringApplication.run(BackendServiceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        initiateAccount();
        initiateProgress();
        initiateLeaderboard();
    }

    private void initiateAccount() {
        log.debug("Initiating test account '{}'", TEST_USER_SUB);
        Account savedAccount = accountRepository.save(Account.builder().accountId(TEST_USER_SUB).build());
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