package nl.optifit.backendservice.cron;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cron.session")
public class SessionSchedule {
    private TimeSlot morning;
    private TimeSlot lunch;
    private TimeSlot afternoon;

    public record TimeSlot(LocalTime create, LocalTime update) {
    }
}
