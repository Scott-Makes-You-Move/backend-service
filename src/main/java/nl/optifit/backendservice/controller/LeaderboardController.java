package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.LeaderboardDto;
import nl.optifit.backendservice.dto.PagedResponseDto;
import nl.optifit.backendservice.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/leaderboard")
@Tag(name = "Leaderboard", description = "Operations related to leaderboards")
@RestController
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<PagedResponseDto<LeaderboardDto>> getLeaderboard(@RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "10") int size,
                                                                           @RequestParam(defaultValue = "desc") String direction,
                                                                           @RequestParam(defaultValue = "completionRate") String sortBy) {
        log.info("GET Leaderboard REST API called");
        PagedResponseDto<LeaderboardDto> leaderboard = leaderboardService.getLeaderboard(page, size, direction, sortBy);
        return ResponseEntity.ok(leaderboard);
    }
}
