package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.LeaderboardViewDTO;
import nl.optifit.backendservice.dto.ResultListDataRepresentation;
import nl.optifit.backendservice.dto.UpdateLeaderboardDTO;
import nl.optifit.backendservice.model.Leaderboard;
import nl.optifit.backendservice.model.PagedResponse;
import nl.optifit.backendservice.service.LeaderboardService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
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
    public ResponseEntity<PagedResponse<LeaderboardViewDTO>> getLeaderboard(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size,
                                                                            @RequestParam(defaultValue = "desc") String direction,
                                                                            @RequestParam(defaultValue = "completionRate") String sortBy) {
        log.info("GET Leaderboard REST API called");
        Page<LeaderboardViewDTO> leaderboard = leaderboardService.getLeaderboard(page, size, direction, sortBy);
        return ResponseEntity.ok(new PagedResponse<>(leaderboard));
    }
}
