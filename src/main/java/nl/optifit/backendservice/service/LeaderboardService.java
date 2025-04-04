package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.LeaderboardViewDTO;
import nl.optifit.backendservice.dto.UpdateLeaderboardDTO;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Leaderboard;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.LeaderboardRepository;
import nl.optifit.backendservice.util.KeycloakService;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.*;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeaderboardService {
    private final LeaderboardRepository leaderboardRepository;
    private final AccountRepository accountRepository;
    private final KeycloakService keycloakService;

    public ResponseEntity<Page<LeaderboardViewDTO>> getLeaderboard(int page, int size, String direction, String sortBy) {
        log.debug("Retrieving leaderboard with page [{}], size [{}], direction [{}], sortBy [{}]", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));

        var leaderboardPage = leaderboardRepository.findAll(pageable).map(leaderboard -> {
            UserResource user = keycloakService.findUserById(leaderboard.getAccount().getId()).orElseThrow(() -> new RuntimeException("User not found"));
            return LeaderboardViewDTO.fromLeaderboard(String.format("%s %s", user.toRepresentation().getFirstName(), user.toRepresentation().getLastName()), leaderboard);
        });

        return  ResponseEntity.ok(leaderboardPage);
    }
}
