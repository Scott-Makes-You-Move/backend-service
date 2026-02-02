package nl.optifit.backendservice.controller.admin;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.ExerciseVideo;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.BiometricsService;
import nl.optifit.backendservice.service.ExerciseVideoService;
import nl.optifit.backendservice.service.FileService;
import nl.optifit.backendservice.service.LeaderboardService;
import nl.optifit.backendservice.service.MobilityService;
import nl.optifit.backendservice.service.SessionService;
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
@RequestMapping("/api/v1/management")
@Tag(name = "Management", description = "Operations related management")
@RestController
public class ManagementController {

    private final FileService fileService;
    private final SessionService sessionService;
    private final AccountService accountService;
    private final ExerciseVideoService exerciseVideoService;
    private final BiometricsService biometricsService;
    private final MobilityService mobilityService;
    private final LeaderboardService leaderboardService;

    /**
     * Run jobs
     */
    @PostMapping("/execute")
    public ResponseEntity<String> executeJob(@RequestParam("job") String job) {
        return switch (job) {
            case "syncFiles" -> fileService.syncFiles();
            case "removeStaleSessions" -> sessionService.removeStaleSessions();
            default -> throw new IllegalArgumentException("Job '%s' not recognized".formatted(job));
        };
    }

    /**
     * Bootstraps data
     */
    @PostMapping
    public ResponseEntity<Void> bootstrapData(@RequestParam String repository) {
        return switch (repository) {
            case "videos" -> bootstrapExerciseVideos();
            default -> throw new IllegalArgumentException("Repository '%s' not recognized".formatted(repository));
        };
    }

    /**
     * Delete repository data
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteData(@RequestParam String repository) {
        switch (repository) {
            case "accounts" -> accountService.deleteAll();
            case "videos" -> exerciseVideoService.deleteAll();
            case "sessions" -> sessionService.deleteAll();
            case "biometrics" -> biometricsService.deleteAll();
            case "mobility" -> mobilityService.deleteAll();
            case "leaderboard" -> leaderboardService.resetLeaderboard();
            default -> throw new IllegalArgumentException("Repository '%s' not recognized".formatted(repository));
        }
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Void> bootstrapExerciseVideos() {
        log.info("Bootstrapping exercise videos");

        var hipLow = ExerciseVideo.builder().exerciseType(HIP).score(1).videoUrl("https://youtu.be/-7mdU1-eEpk").build();
        var hipMedium = ExerciseVideo.builder().exerciseType(HIP).score(2).videoUrl("https://youtu.be/uceJ4BlkYeM").build();
        var hipHigh = ExerciseVideo.builder().exerciseType(HIP).score(3).videoUrl("https://youtu.be/KXfQJKzXYrM").build();

        var shoulderLow = ExerciseVideo.builder().exerciseType(SHOULDER).score(1).videoUrl("https://youtu.be/b0kj4lzp1Kk").build();
        var shoulderMedium = ExerciseVideo.builder().exerciseType(SHOULDER).score(2).videoUrl("https://youtu.be/s4eam7dodSQ").build();
        var shoulderHigh = ExerciseVideo.builder().exerciseType(SHOULDER).score(3).videoUrl("https://youtu.be/QKEP71NglPI").build();

        var backLow = ExerciseVideo.builder().exerciseType(BACK).score(1).videoUrl("https://youtu.be/jjBJeE_WN3Q").build();
        var backMedium = ExerciseVideo.builder().exerciseType(BACK).score(2).videoUrl("https://youtu.be/xH7enFOXXE4").build();
        var backHigh = ExerciseVideo.builder().exerciseType(BACK).score(3).videoUrl("https://youtu.be/Pu47xMcuwMo").build();

        List<ExerciseVideo> exerciseVideos = List.of(hipLow, hipMedium, hipHigh, shoulderLow, shoulderMedium, shoulderHigh, backLow, backMedium, backHigh);
        exerciseVideoService.saveAll(exerciseVideos);
        return ResponseEntity.ok().build();
    }
}
