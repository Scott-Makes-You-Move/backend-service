package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.CreateLeaderboardDTO;
import nl.optifit.backendservice.dto.UpdateLeaderboardDTO;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Leaderboard;
import nl.optifit.backendservice.repository.AccountRepository;
import nl.optifit.backendservice.repository.LeaderboardRepository;
import nl.optifit.backendservice.util.KeycloakService;
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

    public List<Leaderboard> getLeaderboard(int page, int size, String direction, String sortBy) {
        log.debug("Retrieving leaderboard with page [{}], size [{}], direction [{}], sortBy [{}]", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return leaderboardRepository.findAll(pageable).getContent();
    }

    public Leaderboard createLeaderBoard(CreateLeaderboardDTO createLeaderboardDTO) {
        log.debug("Creating leaderboard for user [{}]", createLeaderboardDTO.getUsername());

        return keycloakService.findUserByUsername(createLeaderboardDTO.getUsername())
                .map(user -> {
                    Account foundOrCreatedAccount = accountRepository.findByAccountId(user.getId())
                            .orElseGet(() -> accountRepository.save(Account.builder().accountId(user.getId()).build()));

                    Leaderboard leaderboardToSave = Leaderboard.builder()
                            .account(foundOrCreatedAccount)
                            .completionRate(0.0)
                            .currentStreak(0)
                            .longestStreak(0)
                            .build();
                    return leaderboardRepository.save(leaderboardToSave);
                }).orElseThrow(() -> new RuntimeException("User doesn't exist in Keycloak"));
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
}
