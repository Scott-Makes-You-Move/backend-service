package nl.optifit.backendservice.dto;

import nl.optifit.backendservice.model.Leaderboard;

public record LeaderboardDto(String fullName, Double completionRate, Integer currentStreak, Integer longestStreak) {

    public static LeaderboardDto fromLeaderboard(String fullName, Leaderboard leaderboard) {
        return new LeaderboardDto(fullName, leaderboard.getCompletionRate(), leaderboard.getCurrentStreak(), leaderboard.getLongestStreak());
    }
}
