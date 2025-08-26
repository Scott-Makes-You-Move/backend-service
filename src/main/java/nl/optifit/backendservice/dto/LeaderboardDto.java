package nl.optifit.backendservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nl.optifit.backendservice.model.Leaderboard;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LeaderboardDto {
    private String fullName;
    private Double completionRate;
    private Integer currentStreak;
    private Integer longestStreak;

    public static LeaderboardDto fromLeaderboard(String fullName, Leaderboard leaderboard) {
        return LeaderboardDto.builder()
                .fullName(fullName)
                .completionRate(leaderboard.getCompletionRate())
                .currentStreak(leaderboard.getCurrentStreak())
                .longestStreak(leaderboard.getLongestStreak())
                .build();
    }
}
