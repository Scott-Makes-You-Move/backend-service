package nl.optifit.backendservice.controller.admin;

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
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("@jwtConverter.currentUserHasRole('smym-admin')")
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
        if (repository.equals("videos")) {
            bootstrapExerciseVideos();
        } else {
            log.warn("Repository '{}' not recognized", repository);
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

        ExerciseVideo hipLow = ExerciseVideo.builder().exerciseType(HIP).score(1).videoUrl("https://youtu.be/-7mdU1-eEpk").build();
        ExerciseVideo hipMedium = ExerciseVideo.builder().exerciseType(HIP).score(2).videoUrl("https://youtu.be/uceJ4BlkYeM").build();
        ExerciseVideo hipHigh = ExerciseVideo.builder().exerciseType(HIP).score(3).videoUrl("https://youtu.be/KXfQJKzXYrM").build();

        ExerciseVideo shoulderLow = ExerciseVideo.builder().exerciseType(SHOULDER).score(1).videoUrl("https://youtu.be/b0kj4lzp1Kk").build();
        ExerciseVideo shoulderMedium = ExerciseVideo.builder().exerciseType(SHOULDER).score(2).videoUrl("https://youtu.be/s4eam7dodSQ").build();
        ExerciseVideo shoulderHigh = ExerciseVideo.builder().exerciseType(SHOULDER).score(3).videoUrl("https://youtu.be/QKEP71NglPI").build();

        ExerciseVideo backLow = ExerciseVideo.builder().exerciseType(BACK).score(1).videoUrl("https://youtu.be/jjBJeE_WN3Q").build();
        ExerciseVideo backMedium = ExerciseVideo.builder().exerciseType(BACK).score(2).videoUrl("https://youtu.be/xH7enFOXXE4").build();
        ExerciseVideo backHigh = ExerciseVideo.builder().exerciseType(BACK).score(3).videoUrl("https://youtu.be/Pu47xMcuwMo").build();

        List<ExerciseVideo> exerciseVideos = List.of(hipLow, hipMedium, hipHigh, shoulderLow, shoulderMedium, shoulderHigh, backLow, backMedium, backHigh);
        exerciseVideoRepository.saveAll(exerciseVideos);
    }
}
