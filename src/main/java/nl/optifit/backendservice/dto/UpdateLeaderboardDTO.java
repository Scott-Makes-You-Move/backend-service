package nl.optifit.backendservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLeaderboardDTO {
    private Double completionRate;
    private Integer currentStreak;
    private Integer longestStreak;
}
