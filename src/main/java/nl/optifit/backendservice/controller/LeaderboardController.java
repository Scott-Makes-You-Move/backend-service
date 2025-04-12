package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.LeaderboardViewDto;
import nl.optifit.backendservice.model.PagedResponse;
import nl.optifit.backendservice.service.LeaderboardService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/leaderboard")
@Tag(name = "Leaderboard", description = "Operations related to leaderboards")
@RestController
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<PagedResponse<LeaderboardViewDto>> getLeaderboard(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size,
                                                                            @RequestParam(defaultValue = "desc") String direction,
                                                                            @RequestParam(defaultValue = "completionRate") String sortBy) {
        log.info("GET Leaderboard REST API called");
        Page<LeaderboardViewDto> leaderboard = leaderboardService.getLeaderboard(page, size, direction, sortBy);
        return ResponseEntity.ok(new PagedResponse<>(leaderboard));
    }
}
