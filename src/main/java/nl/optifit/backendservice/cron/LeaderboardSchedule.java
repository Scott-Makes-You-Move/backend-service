package nl.optifit.backendservice.cron;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;
import java.time.LocalTime;

@ConfigurationProperties(prefix = "cron.leaderboard")
public record LeaderboardSchedule(
        LocalTime resetTime,
        DayOfWeek resetDayOfWeek,
        int resetWeek
) {}
