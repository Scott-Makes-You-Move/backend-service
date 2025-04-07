package nl.optifit.backendservice.dto;

import lombok.*;
import nl.optifit.backendservice.model.Leaderboard;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LeaderboardViewDto {
    private String fullName;
    private Double completionRate;
    private Integer currentStreak;
    private Integer longestStreak;

    public static LeaderboardViewDto fromLeaderboard(String fullName, Leaderboard leaderboard) {
        return LeaderboardViewDto.builder()
                .fullName(fullName)
                .completionRate(leaderboard.getCompletionRate())
                .currentStreak(leaderboard.getCurrentStreak())
                .longestStreak(leaderboard.getLongestStreak())
                .build();
    }
}
