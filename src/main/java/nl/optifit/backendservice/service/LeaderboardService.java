package nl.optifit.backendservice.service;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.LeaderboardDto;
import nl.optifit.backendservice.dto.PagedResponseDto;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Leaderboard;
import nl.optifit.backendservice.model.Session;
import nl.optifit.backendservice.model.SessionStatus;
import nl.optifit.backendservice.repository.LeaderboardRepository;
import org.keycloak.admin.client.resource.UserResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static nl.optifit.backendservice.model.SessionStatus.COMPLETED;
import static nl.optifit.backendservice.model.SessionStatus.OVERDUE;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeaderboardService {
    private final KeycloakService keycloakService;
    private final LeaderboardRepository leaderboardRepository;

    public PagedResponseDto<LeaderboardDto> findAll(int page, int size, String direction, String sortBy) {
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
        leaderboard.setCompletionRate(calculateSessionCompletionRate(account, leaderboard));
        leaderboard.setLastUpdated(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        return leaderboard;
    }

    public double calculateSessionCompletionRate(Account account, Leaderboard leaderboard) {
        LocalDateTime resetAt = leaderboard.getResetAt();
        LocalDateTime relevantDateTime = resetAt == null ? leaderboard.getLastUpdated() : resetAt;
        ZonedDateTime relevantZonedDateTime = relevantDateTime.atZone(ZoneId.of("Europe/Amsterdam"));

        List<Session> relevantSessions = account.getSessions().stream()
                .filter(session -> session.getSessionStart().isAfter(relevantZonedDateTime))
                .toList();

        long totalSessions = relevantSessions.size();
        long completedSessions = relevantSessions.stream()
                .map(Session::getSessionExecutionTime)
                .filter(Objects::nonNull)
                .count();

        return totalSessions > 0
                ? (double) completedSessions / totalSessions * 100
                : 0;
    }

    public void resetLeaderboard() {
        log.debug("Resetting leaderboard");

        List<Leaderboard> leaderboards = leaderboardRepository.findAll();
        String recentWinner = findRecentWinner(leaderboards);

        leaderboards.forEach(leaderboard -> {

            if (leaderboard.getAccountId().equals(recentWinner)) {
                leaderboard.setRecentWinner(true);
            }

            leaderboard.setCurrentStreak(0);
            leaderboard.setLongestStreak(0);
            leaderboard.setCompletionRate(0.0);
            leaderboard.setLastUpdated(LocalDateTime.now());
            leaderboard.setResetAt(LocalDateTime.now());
        });

        leaderboardRepository.saveAll(leaderboards);
    }

    private static String findRecentWinner(List<Leaderboard> leaderboards) {
        List<Leaderboard> sorted = sortLeaderboards(leaderboards);

        Leaderboard top = sorted.get(0);
        List<Leaderboard> topCandidates = sorted.stream()
                .filter(l -> Objects.equals(l.getCompletionRate(), top.getCompletionRate())
                        && Objects.equals(l.getLongestStreak(), top.getLongestStreak())
                        && Objects.equals(l.getCurrentStreak(), top.getCurrentStreak()))
                .toList();

        Leaderboard winner = topCandidates.size() == 1
                ? topCandidates.get(0)
                : topCandidates.get(new Random().nextInt(topCandidates.size()));

        return winner.getAccountId();
    }

    private static List<Leaderboard> sortLeaderboards(List<Leaderboard> leaderboards) {
        if (leaderboards == null || leaderboards.isEmpty()) {
            throw new IllegalArgumentException("No leaderboards found");
        }

        Comparator<Leaderboard> comparator = Comparator
                .comparing(Leaderboard::getCompletionRate, Comparator.reverseOrder())
                .thenComparing(Leaderboard::getLongestStreak, Comparator.reverseOrder())
                .thenComparing(Leaderboard::getCurrentStreak, Comparator.reverseOrder());

        List<Leaderboard> sorted = new ArrayList<>(leaderboards);
        sorted.sort(comparator);
        return sorted;
    }
}
