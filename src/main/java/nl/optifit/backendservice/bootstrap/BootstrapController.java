package nl.optifit.backendservice.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.BiometricsRepository;
import nl.optifit.backendservice.repository.LeaderboardRepository;
import nl.optifit.backendservice.util.KeycloakService;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
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
    private final KeycloakService keycloakService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final ClassPathResource BOOTSTRAP_DATA_RESOURCE = new ClassPathResource("bootstrap/bootstrap-data.json");
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostMapping
    public String bootstrap() throws IOException {
        File bootstrapDataFile = BOOTSTRAP_DATA_RESOURCE.getFile();

        try {
            List<BootstrapDataModel> bootstrapData = objectMapper.readValue(bootstrapDataFile, objectMapper.getTypeFactory().constructCollectionType(List.class, BootstrapDataModel.class));
            bootstrapData.forEach(data -> {
                keycloakService.findUserByUsername(data.getUsername()).ifPresent(user -> {
                    initiateAccount(user, data);
                });
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "Data bootstrapped successfully";
    }

    @DeleteMapping
    public String deleteExistingData() throws IOException {
        accountRepository.deleteAll();
        return "Data deleted successfully";
    }

    private void initiateAccount(UserRepresentation user, BootstrapDataModel bootstrapData) {
        log.info("Initiating account for user '{}'", user.getUsername());

        if (accountRepository.findByAccountId(user.getId()).isPresent()) {
            log.info("Account for user '{}' was already initialized", user.getUsername());
            return;
        }

        Account account = Account.builder().accountId(user.getId()).build();
        Leaderboard leaderboard = Leaderboard.builder()
                .account(account)
                .completionRate(bootstrapData.getLeaderboard().getCompletionRate())
                .currentStreak(bootstrapData.getLeaderboard().getCurrentStreak())
                .longestStreak(bootstrapData.getLeaderboard().getLongestStreak())
                .build();
        account.setLeaderboard(leaderboard);

        List<Biometrics> biometrics = new ArrayList<>();
        List<Mobility> mobilities = new ArrayList<>();

        bootstrapData.getMeasurements().forEach(measurement -> {
            LocalDate localDate = LocalDate.parse(measurement.getDate(), FORMATTER);
            LocalDateTime localDateTime = LocalDateTime.of(localDate, LocalTime.of(12, 0, 0));

            Biometrics biometric = Biometrics.builder()
                    .account(account)
                    .measuredOn(localDateTime)
                    .weight(measurement.getWeight())
                    .fat(measurement.getFat())
                    .visceralFat(measurement.getVisceralFat())
                    .build();
            biometrics.add(biometric);

            Mobility mobility = Mobility.builder()
                    .account(account)
                    .measuredOn(localDateTime)
                    .shoulder(measurement.getShoulder())
                    .back(measurement.getBack())
                    .hip(measurement.getHip())
                    .build();
            mobilities.add(mobility);
        });
        account.setBiometrics(biometrics);
        account.setMobilities(mobilities);

        accountRepository.save(account);
        log.info("Account initiated for user '{}'", user.getUsername());
    }
}