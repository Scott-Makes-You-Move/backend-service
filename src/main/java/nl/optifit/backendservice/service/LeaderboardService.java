package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.LeaderboardViewDto;
import nl.optifit.backendservice.repository.LeaderboardRepository;
import nl.optifit.backendservice.util.KeycloakService;
import org.keycloak.admin.client.resource.UserResource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeaderboardService {
    private final LeaderboardRepository leaderboardRepository;
    private final KeycloakService keycloakService;

    public Page<LeaderboardViewDto> getLeaderboard(int page, int size, String direction, String sortBy) {
        log.debug("Retrieving leaderboard with page '{}', size '{}', direction '{}', sortBy '{}'", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));

        return leaderboardRepository.findAll(pageable).map(leaderboard -> {
            UserResource user = keycloakService.findUserById(leaderboard.getAccount().getId()).orElseThrow(() -> new RuntimeException("User not found"));
            return LeaderboardViewDto.fromLeaderboard(String.format("%s %s", user.toRepresentation().getFirstName(), user.toRepresentation().getLastName()), leaderboard);
        });
    }
}
