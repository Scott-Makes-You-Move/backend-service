package nl.optifit.backendservice.dto;

import nl.optifit.backendservice.model.Leaderboard;

public record LeaderboardDto(String fullName, double completionRate, int currentStreak, int longestStreak, boolean recentWinner) {

    public static LeaderboardDto fromLeaderboard(String fullName, Leaderboard leaderboard) {
        return new LeaderboardDto(fullName, leaderboard.getCompletionRate(), leaderboard.getCurrentStreak(), leaderboard.getLongestStreak(), leaderboard.getRecentWinner());
    }
}
