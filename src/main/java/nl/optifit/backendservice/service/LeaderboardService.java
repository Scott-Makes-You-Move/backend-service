package nl.optifit.backendservice.service;

import jakarta.ws.rs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.*;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.repository.*;
import nl.optifit.backendservice.util.KeycloakService;
import org.keycloak.admin.client.resource.UserResource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

import static nl.optifit.backendservice.model.SessionStatus.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeaderboardService {
    private final KeycloakService keycloakService;
    private final LeaderboardRepository leaderboardRepository;

    public PagedResponseDto<LeaderboardDto> getLeaderboard(int page, int size, String direction, String sortBy) {
        log.debug("Retrieving leaderboard with page '{}', size '{}', direction '{}', sortBy '{}'", page, size, direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));

        Page<LeaderboardDto> leaderboardDtoPage = leaderboardRepository.findAll(pageable).map(leaderboard -> {
            UserResource user = keycloakService.findUserById(leaderboard.getAccount().getId()).orElseThrow(() -> new RuntimeException("User not found"));
            return LeaderboardDto.fromLeaderboard(String.format("%s %s", user.toRepresentation().getFirstName(), user.toRepresentation().getLastName()), leaderboard);
        });

        return PagedResponseDto.fromPage(leaderboardDtoPage);
    }

    public void updateLeaderboard(Session latestSession) {
        Account account = latestSession.getAccount();
        log.debug("Updating leaderboard for account '{}'", account.getId());

        Leaderboard leaderboard = leaderboardRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new NotFoundException("No leaderboard found for account"));
        Leaderboard updatedLeaderboard = updateStreak(latestSession, leaderboard);

        leaderboardRepository.save(updatedLeaderboard);
    }

    public Leaderboard createLeaderboardForAccount(Account account) {
        return Leaderboard.builder()
                .account(account)
                .lastUpdated(LocalDateTime.now())
                .completionRate(0.0)
                .currentStreak(0)
                .longestStreak(0)
                .build();
    }

    private Leaderboard updateStreak(Session latestSession, Leaderboard leaderboard) {
        Account account = latestSession.getAccount();
        SessionStatus sessionStatus = latestSession.getSessionStatus();

        if (sessionStatus.equals(COMPLETED)) {
            leaderboard.setCurrentStreak(leaderboard.getCurrentStreak() + 1);
            leaderboard.setLongestStreak(Math.max(leaderboard.getCurrentStreak(), leaderboard.getLongestStreak()));
        }
        if (sessionStatus.equals(OVERDUE)) {
            leaderboard.setCurrentStreak(0);
        }
        leaderboard.setCompletionRate(calculateSessionCompletionRate(account));
        leaderboard.setLastUpdated(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        return leaderboard;
    }

    public double calculateSessionCompletionRate(Account account) {
        List<Session> sessionsForAccount = account.getSessions();

        long totalSessions = sessionsForAccount.size();
        long completedSessions = sessionsForAccount.stream()
                .map(Session::getSessionExecutionTime)
                .filter(Objects::nonNull)
                .count();

        return totalSessions > 0
                ? (double) completedSessions / totalSessions * 100
                : 0;
    }
}
