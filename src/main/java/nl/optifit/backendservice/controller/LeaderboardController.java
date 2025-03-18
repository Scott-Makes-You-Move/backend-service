package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.CreateLeaderboardDTO;
import nl.optifit.backendservice.dto.LeaderboardViewDTO;
import nl.optifit.backendservice.dto.UpdateLeaderboardDTO;
import nl.optifit.backendservice.model.Leaderboard;
import nl.optifit.backendservice.service.LeaderboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/leaderboard")
@RestController
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    @GetMapping
    public List<LeaderboardViewDTO> getLeaderboard(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(defaultValue = "DESC") String direction,
                                                   @RequestParam(defaultValue = "completionRate") String sortBy) {
        log.info("GET Leaderboard REST API called");
        return leaderboardService.getLeaderboard(page, size, direction, sortBy);
    }

    @PostMapping
    public Leaderboard createLeaderboard(@RequestBody CreateLeaderboardDTO createLeaderboardDTO) {
        log.info("POST Leaderboard REST API called");
        return leaderboardService.createLeaderBoard(createLeaderboardDTO);
    }

    @PutMapping("/{username}")
    public Leaderboard updateLeaderboard(@PathVariable String username,
                                         @RequestBody UpdateLeaderboardDTO updateLeaderboardDTO) {
        log.info("PUT Leaderboard REST API called");
        return leaderboardService.updateLeaderboard(username, updateLeaderboardDTO);
    }
}
