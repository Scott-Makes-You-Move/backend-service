package nl.optifit.backendservice.cron;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cron.leaderboard")
public class LeaderboardSchedule {
    private DayOfWeek resetDayOfWeek;
    private int resetWeek;
    private LocalTime resetTime;
}
