package nl.optifit.backendservice.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.service.DriveService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Slf4j
@RequiredArgsConstructor
@Component
public class DriveCronScheduler {

    public static final String EUROPE_AMSTERDAM = "Europe/Amsterdam";

    private final DriveService driveService;

    @Scheduled(cron = "#{@cronProperties.drive.pull}", zone = EUROPE_AMSTERDAM)
    public void resetLeaderboard() throws GeneralSecurityException, IOException {
        driveService.getDriveFiles();
    }
}
