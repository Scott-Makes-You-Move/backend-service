package nl.optifit.backendservice.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.exception.*;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.BiometricsRepository;
import nl.optifit.backendservice.repository.ExerciseVideoRepository;
import nl.optifit.backendservice.repository.MobilityRepository;
import nl.optifit.backendservice.repository.SessionRepository;
import nl.optifit.backendservice.util.KeycloakService;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static nl.optifit.backendservice.model.ExerciseType.*;

@Hidden
@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/bootstrap")
@Tag(name = "Bootstrap", description = "Operations related to bootstrapping")
@RestController
public class BootstrapController {

    private final AccountRepository accountRepository;
    private final ExerciseVideoRepository exerciseVideoRepository;
    private final KeycloakService keycloakService;
    private final ObjectMapper objectMapper;

    public static final ClassPathResource BOOTSTRAP_DATA_RESOURCE = new ClassPathResource("bootstrap/bootstrap-data.json");
    private final SessionRepository sessionRepository;
    private final BiometricsRepository biometricsRepository;
    private final MobilityRepository mobilityRepository;

    /**
     * Creates accounts, leaderboard, biometrics, mobilities and sessions for users in bootstrap-data.json
     */
    @PostMapping
    public ResponseEntity<AccountsBootstrappedData> bootstrapData() throws IOException {
        File bootstrapDataFile = BOOTSTRAP_DATA_RESOURCE.getFile();
        AccountsBootstrappedData response = new AccountsBootstrappedData();

        try {
            List<BootstrapDataModel> bootstrapData = objectMapper.readValue(bootstrapDataFile, objectMapper.getTypeFactory().constructCollectionType(List.class, BootstrapDataModel.class));
            bootstrapData.forEach(data -> keycloakService.findUserByUsername(data.getUsername()).ifPresent(user -> {
                initiateAccount(user, data, response);
            }));
        } catch (Exception e) {
            log.error("Exception occurred during bootstrap", e);
            throw new BootstrapException("Something went wrong while bootstrapping", e);
        }

        int total = response.getAccountIds().size();
        response.setTotal(total);

        bootstrapExerciseVideos();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Deletes all accounts and their corresponding leaderboard, biometrics and mobilities.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteExistingData(@RequestParam String repository) {
        switch (repository) {
            case "videos" -> exerciseVideoRepository.deleteAll();
            case "sessions" -> sessionRepository.deleteAll();
            case "biometrics" -> biometricsRepository.deleteAll();
            case "mobility" -> mobilityRepository.deleteAll();
            case "all" -> accountRepository.deleteAll();
        }
        return ResponseEntity.noContent().build();
    }

    private void initiateAccount(UserRepresentation user, BootstrapDataModel bootstrapData, AccountsBootstrappedData response) {
        log.info("Initiating account for user '{}'", user.getUsername());

        if (accountRepository.findById(user.getId()).isPresent()) {
            log.info("Account for user '{}' was already initialized", user.getUsername());
            return;
        }

        Account account = Account.builder().id(user.getId()).build();
        Leaderboard leaderboard = Leaderboard.builder()
                .account(account)
                .lastUpdated(LocalDateTime.now())
                .completionRate(bootstrapData.getLeaderboard().getCompletionRate())
                .currentStreak(bootstrapData.getLeaderboard().getCurrentStreak())
                .longestStreak(bootstrapData.getLeaderboard().getLongestStreak())
                .build();
        account.setLeaderboard(leaderboard);

        List<Biometrics> biometrics = new ArrayList<>();
        List<Mobility> mobilities = new ArrayList<>();

        bootstrapData.getMeasurements().forEach(measurement -> {

            Biometrics biometric = Biometrics.builder()
                    .account(account)
                    .measuredOn(measurement.getMeasuredOn())
                    .weight(measurement.getWeight())
                    .fat(measurement.getFat())
                    .visceralFat(measurement.getVisceralFat())
                    .build();
            biometrics.add(biometric);

            Mobility mobility = Mobility.builder()
                    .account(account)
                    .measuredOn(measurement.getMeasuredOn())
                    .shoulder(measurement.getShoulder())
                    .back(measurement.getBack())
                    .hip(measurement.getHip())
                    .build();
            mobilities.add(mobility);
        });
        account.setBiometrics(biometrics);
        account.setMobilities(mobilities);

        List<Session> sessions = new ArrayList<>();
        bootstrapData.getSessions().forEach(session -> {
            Session completedSession = Session.builder()
                    .account(account)
                    .sessionStart(session.getSessionStart())
                    .sessionExecutionTime(session.getSessionExecutionTime())
                    .exerciseType(session.getExerciseType())
                    .sessionStatus(session.getSessionStatus())
                    .build();
            sessions.add(completedSession);
        });
        account.setSessions(sessions);

        Account savedAccount = accountRepository.save(account);
        response.getAccountIds().add(savedAccount.getId());
        log.info("Account initiated for user '{}'", user.getUsername());
    }

    private void bootstrapExerciseVideos() {
        log.info("Bootstrap session videos");
        ExerciseVideo hipLow = ExerciseVideo.builder().exerciseType(HIP).score(1).videoUrl("https://www.youtube.com/watch?v=glbLJEHfoa4").build();
        ExerciseVideo hipMedium = ExerciseVideo.builder().exerciseType(HIP).score(2).videoUrl("https://www.youtube.com/watch?v=ef0iIL20rMA").build();
        ExerciseVideo hipHigh = ExerciseVideo.builder().exerciseType(HIP).score(3).videoUrl("https://www.youtube.com/watch?v=fHLyMY828Jg").build();

        ExerciseVideo shoulderLow = ExerciseVideo.builder().exerciseType(SHOULDER).score(1).videoUrl("https://www.youtube.com/watch?v=glbLJEHfoa4").build();
        ExerciseVideo shoulderMedium = ExerciseVideo.builder().exerciseType(SHOULDER).score(2).videoUrl("https://www.youtube.com/watch?v=ef0iIL20rMA").build();
        ExerciseVideo shoulderHigh = ExerciseVideo.builder().exerciseType(SHOULDER).score(3).videoUrl("https://www.youtube.com/watch?v=fHLyMY828Jg").build();

        ExerciseVideo backLow = ExerciseVideo.builder().exerciseType(BACK).score(1).videoUrl("https://www.youtube.com/watch?v=buyuA7moFP4").build();
        ExerciseVideo backMedium = ExerciseVideo.builder().exerciseType(BACK).score(2).videoUrl("https://www.youtube.com/watch?v=zWH9v5Ge7Ao").build();
        ExerciseVideo backHigh = ExerciseVideo.builder().exerciseType(BACK).score(3).videoUrl("https://www.youtube.com/watch?v=fHLyMY828Jg").build();

        List<ExerciseVideo> exerciseVideos = List.of(hipLow, hipMedium, hipHigh, shoulderLow, shoulderMedium, shoulderHigh, backLow, backMedium, backHigh);
        exerciseVideoRepository.saveAll(exerciseVideos);
    }
}
