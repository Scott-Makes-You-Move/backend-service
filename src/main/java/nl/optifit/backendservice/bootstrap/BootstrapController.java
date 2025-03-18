package nl.optifit.backendservice.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.LeaderboardRepository;
import nl.optifit.backendservice.repository.ProgressRepository;
import nl.optifit.backendservice.util.KeycloakService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/bootstrap")
@RestController
public class BootstrapController {

    private final AccountRepository accountRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final ProgressRepository progressRepository;
    private final KeycloakService keycloakService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final String BOOTSTRAP_PATH = "bootstrap/bootstrap-data.json";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static boolean alreadyInitialized = false;

    @PostMapping
    public String bootstrap() {
        if (alreadyInitialized) {
            return "Already Initialized!";
        }

        File jsonFile = new File(BOOTSTRAP_PATH);

        try {
            List<BootstrapDataModel> bootstrapData = objectMapper.readValue(jsonFile, objectMapper.getTypeFactory().constructCollectionType(List.class, BootstrapDataModel.class));
            bootstrapData.forEach(data -> {
                keycloakService.findUserByUsername(data.getUsername()).ifPresent(user -> {
                    Account savedAccount = accountRepository.save(Account.builder().accountId(user.getId()).build());
                    initiateLeaderboard(data, savedAccount);
                    initiateProgress(data, savedAccount);
                    alreadyInitialized = true;
                });
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "Data bootstrapped successfully";
    }

    private void initiateLeaderboard(BootstrapDataModel bootstrapData, Account savedAccount) {
        log.debug("Initiating leaderboard");
        Leaderboard leaderboard = Leaderboard.builder()
                .account(savedAccount)
                .completionRate(bootstrapData.getLeaderboard().getCompletionRate())
                .currentStreak(bootstrapData.getLeaderboard().getCurrentStreak())
                .longestStreak(bootstrapData.getLeaderboard().getLongestStreak())
                .build();
        leaderboardRepository.save(leaderboard);
        log.debug("Leaderboard initiated successfully");
    }

    private void initiateProgress(BootstrapDataModel bootstrapData, Account savedAccount) {
        log.debug("Initiating progress");
        Progress progressForAccount = Progress.builder()
                .account(savedAccount)
                .biometrics(new ArrayList<>())
                .mobilities(new ArrayList<>())
                .build();

        List<Biometrics> biometrics = new ArrayList<>();
        List<Mobility> mobilities = new ArrayList<>();

        bootstrapData.getMeasurements().forEach(measurement -> {
            LocalDate localDate = LocalDate.parse(measurement.getDate(), FORMATTER);
            LocalDateTime localDateTime = LocalDateTime.of(localDate, LocalTime.of(12, 0, 0));

            Biometrics biometric = initiateBiometric(savedAccount, measurement, progressForAccount, localDateTime);
            biometrics.add(biometric);
            Mobility mobility = initiateMobility(savedAccount, measurement, progressForAccount, localDateTime);
            mobilities.add(mobility);

            progressForAccount.getBiometrics().addAll(biometrics);
            progressForAccount.getMobilities().addAll(mobilities);

            progressRepository.save(progressForAccount);
            log.debug("Progress initiated successfully");
        });
    }

    private static Mobility initiateMobility(Account savedAccount, Measurement measurement, Progress progressForAccount, LocalDateTime localDateTime) {
        return Mobility.builder()
                .account(savedAccount)
                .progress(progressForAccount)
                .measuredOn(localDateTime)
                .shoulder(measurement.getShoulder())
                .back(measurement.getBack())
                .hip(measurement.getHip())
                .build();
    }

    private static Biometrics initiateBiometric(Account savedAccount, Measurement measurement, Progress progressForAccount, LocalDateTime localDateTime) {
        return Biometrics.builder()
                .account(savedAccount)
                .progress(progressForAccount)
                .measuredOn(localDateTime)
                .weight(measurement.getWeight())
                .fat(measurement.getFat())
                .visceralFat(measurement.getVisceralFat())
                .build();
    }
}