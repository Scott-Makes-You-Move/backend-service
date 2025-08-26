package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.ExerciseVideo;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.BiometricsRepository;
import nl.optifit.backendservice.repository.ExerciseVideoRepository;
import nl.optifit.backendservice.repository.MobilityRepository;
import nl.optifit.backendservice.repository.SessionRepository;
import nl.optifit.backendservice.service.LeaderboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static nl.optifit.backendservice.model.ExerciseType.BACK;
import static nl.optifit.backendservice.model.ExerciseType.HIP;
import static nl.optifit.backendservice.model.ExerciseType.SHOULDER;

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
    private final SessionRepository sessionRepository;
    private final BiometricsRepository biometricsRepository;
    private final MobilityRepository mobilityRepository;
    private final LeaderboardService leaderboardService;

    /**
     * Bootstraps data
     */
    @PostMapping
    public ResponseEntity<String> bootstrapData(@RequestParam("repository") String repository) {
        switch (repository) {
            case "videos" -> bootstrapExerciseVideos();
            default -> log.warn("Repository '{}' not recognized", repository);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("Bootstrap exercise videos successful");
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
            case "leaderboard" -> leaderboardService.resetLeaderboard();
            default -> accountRepository.deleteAll();
        }
        return ResponseEntity.noContent().build();
    }

    private void bootstrapExerciseVideos() {
        log.info("Bootstrapping exercise videos");
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
