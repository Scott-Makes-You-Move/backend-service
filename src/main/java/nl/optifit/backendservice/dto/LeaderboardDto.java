package nl.optifit.backendservice.dto;

import nl.optifit.backendservice.model.Leaderboard;

import java.util.Objects;

public record LeaderboardDto(
        String fullName,
        int score,
        double completionRate,
        int currentStreak,
        int longestStreak,
        boolean recentWinner) {

    public static LeaderboardDto fromLeaderboard(String fullName, Leaderboard leaderboard) {
        return new LeaderboardDto(
                fullName,
                Objects.requireNonNullElse(leaderboard.getScore(), 0),
                leaderboard.getCompletionRate(),
                leaderboard.getCurrentStreak(),
                leaderboard.getLongestStreak(),
                leaderboard.getRecentWinner()
        );
    }
}
