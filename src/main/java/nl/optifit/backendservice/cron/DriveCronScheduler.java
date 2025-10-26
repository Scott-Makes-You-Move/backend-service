package nl.optifit.backendservice.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.service.FileService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DriveCronScheduler {

    private final FileService fileService;

    @Scheduled(cron = "${cron.drive.sync}", zone = "UTC")
    public void syncFiles() throws InterruptedException {
        fileService.syncFiles();
    }
}
