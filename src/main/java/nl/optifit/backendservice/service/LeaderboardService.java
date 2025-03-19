package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.CreateLeaderboardDTO;
import nl.optifit.backendservice.dto.LeaderboardViewDTO;
import nl.optifit.backendservice.dto.UpdateLeaderboardDTO;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Leaderboard;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.LeaderboardRepository;
import nl.optifit.backendservice.util.KeycloakService;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeaderboardService {
    private final LeaderboardRepository leaderboardRepository;
    private final AccountRepository accountRepository;
    private final KeycloakService keycloakService;

    public List<LeaderboardViewDTO> getLeaderboard(int page, int size, String direction, String sortBy) {
        log.debug("Retrieving leaderboard with page [{}], size [{}], direction [{}], sortBy [{}]", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        List<Leaderboard> leaderboardList = leaderboardRepository.findAll(pageable).getContent();
        return leaderboardList.stream().map(leaderboard -> {
            Optional<UserResource> optionalUser = keycloakService.findUserById(leaderboard.getAccount().getAccountId());
            UserRepresentation user = optionalUser.map(UserResource::toRepresentation).orElseThrow(() -> new RuntimeException("User not found"));
            return LeaderboardViewDTO.fromLeaderboard(String.format("%s %s", user.getFirstName(), user.getLastName()), leaderboard);
        }).toList();
    }

    public Leaderboard createLeaderBoard(CreateLeaderboardDTO createLeaderboardDTO) {
        log.debug("Creating leaderboard for user [{}]", createLeaderboardDTO.getAccountId());
        Account account = accountRepository.findByAccountId(createLeaderboardDTO.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        Leaderboard leaderboard = Leaderboard.builder()
                .account(account)
                .completionRate(0.0)
                .currentStreak(0)
                .longestStreak(0)
                .build();
        return leaderboardRepository.save(leaderboard);
    }

    public Leaderboard updateLeaderboard(String username, UpdateLeaderboardDTO updateLeaderboardDTO) {
        log.debug("Updating leaderboard for user [{}]", username);

        return keycloakService.findUserByUsername(username).map(user -> {
            Optional<Account> optionalAccount = accountRepository.findByAccountId(user.getId());

            return optionalAccount.map(foundAccount -> {
                Leaderboard foundLeaderboard = leaderboardRepository.findByAccount(foundAccount);

                Optional.ofNullable(updateLeaderboardDTO.getCompletionRate()).ifPresent(foundLeaderboard::setCompletionRate);
                Optional.ofNullable(updateLeaderboardDTO.getCurrentStreak()).ifPresent(foundLeaderboard::setCurrentStreak);
                Optional.ofNullable(updateLeaderboardDTO.getLongestStreak()).ifPresent(foundLeaderboard::setLongestStreak);

                return leaderboardRepository.save(foundLeaderboard);
            }).orElseGet(() -> {
                log.error("Could not update leaderboard for user [{}] - Account not found", username);
                return null;
            });
        }).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteLeaderboard(String accountId) {
        log.debug("Deleting leaderboard for user [{}]", accountId);
        leaderboardRepository.deleteByAccount_AccountId(accountId);
    }
}
