package nl.optifit.backendservice.cron;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

@ConfigurationProperties(prefix = "cron.sessions")
public record SessionSchedule(
        TimeSlot morning,
        TimeSlot lunch,
        TimeSlot afternoon
) {
    public record TimeSlot(LocalTime create, LocalTime update) {
    }
}
